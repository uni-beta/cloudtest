package com.unibeta.cloudtest.parallel.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestInput.CloudTestParameter;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.PluginConfig;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.parallel.mapred.Server;
import com.unibeta.cloudtest.restful.http.HttpClientUtils;
import com.unibeta.cloudtest.tool.RemoteCasesManager;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.servlets.URLConfiguration;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Remote server node common util.
 * 
 * @author jordan.xue
 */
public class RemoteParallelUtil {

	private static final int REUPLOAD_MAX_TIMES = 10;
	private static final String DEFAULT_UPLOADING_BLOCK_MAX_SIZE = "1024";
	private static final int ONE_KB = 1024;

	static Logger log = Logger.getLogger(RemoteParallelUtil.class);

	public synchronized static String checkAndBackupDataToSlavesNode(Map<String, TestService> servers,Map<String, String> serversAddressMap) {

		StringBuffer sb = new StringBuffer();
		Map<String, TestService> validServers = new HashMap<String, TestService>();

		if (null == servers || servers.values() == null || servers.values().size() == 0) {
			return "no valid slave server is available.";
		}

		String root = ConfigurationProxy.getCloudTestRootPath();
		File zippedFile = null;
		try {
			zippedFile = CloudTestUtils.zipFiles(new File(root));
			Map<String, byte[]> fileMap = CloudTestUtils.readFilesToBytes(zippedFile.getPath());

			Set<String> set = servers.keySet();

			ThreadPoolExecutor executor = new ThreadPoolExecutor(set.size(), set.size(), 60, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>());

			for (String s : set) {

				TestService testService = servers.get(s);

				if (RemoteParallelUtil.getRemoteIsInParallelJobService(testService)) {
					String msg = "Slave node [" + s + "] will be ignored, caused by:"
							+ RemoteParallelUtil.getRemoteWarningMessage(testService);
					log.warn(msg);
					sb.append(msg);

					// servers.remove(s);
					continue;

				} else {
					// add lock for user
					RemoteParallelUtil.setRemoteParallelJobUser(testService, ConfigurationProxy.getOsUserName());
					String remoteHomePath = RemoteParallelUtil.setupCloudHomePath(testService, null);

					if (!CommonUtils.isNullOrEmpty(remoteHomePath)) {
						
						String address = null;
						if(null == serversAddressMap){
							address = getSlaverServerById(s).address;
						}else{
							address = serversAddressMap.get(s);
						}
						
						executor.execute(new CaseDataUploadingThread(testService, s,address ,fileMap));
						validServers.put(s, testService);

					} else {
						String msg = "Slave node [" + s + "]" + " CLOUDTEST_HOME path is invalid, it will be ignored.";
						log.warn(msg);
						sb.append("\n" + msg);

						// servers.remove(s);
						continue;
					}
				}
			}

			executor.shutdown();

			boolean done = false;
			while (!done) {
				done = executor.awaitTermination(2, TimeUnit.MILLISECONDS);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			sb.append("\n" + e.getMessage());
		} finally {
			servers.clear();
			servers.putAll(validServers);

			if (null != zippedFile) {
				CloudTestUtils.deleteFiles(zippedFile.getPath());
			}
		}

		return sb.toString();

	}

	private static String cleanAllData(TestService testService) {

		if (null == testService) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(RemoteCasesManager.class.getCanonicalName());
		input.setMethodName("cleanAll");

		CloudTestOutput output = testService.doTest(input);

		if (!output.getStatus()) {
			return output.getErrorMessage();
		}

		return null;

	}

	public static String getRemoteContextRealPath(TestService testService) {

		if (null == testService) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(URLConfiguration.class.getCanonicalName());
		input.setMethodName("getRealPath");

		CloudTestOutput output = testService.doTest(input);

		String h = (String) ObjectDigester.fromXML(output.getReturns());

		return CloudTestUtils.wrapFilePath(h);

	}

	public static String uploadToRemoteSlaveNode(TestService testService, String localFilePath, byte[] base64Code,
			String remoteFilePath, String serverName,String testServiceAddress) {

		if (null == testService) {
			testService = new CloudTestService();
		}

		// upload
		CloudTestInput input = null;
		List<CloudTestParameter> paras;
		CloudTestOutput output = null;

		String errorMsg = null;

		errorMsg = uploading(localFilePath, remoteFilePath, serverName,testServiceAddress);

		if (!CommonUtils.isNullOrEmpty(errorMsg)) {

			output = uploading(testService, base64Code, remoteFilePath, serverName,testServiceAddress);
			if (!output.getStatus()) {
				errorMsg = errorMsg + "\n" + output.getErrorMessage();
				log.error(errorMsg+ "\nBlocked-Distribution was also failed.");
				return errorMsg;
			}

		}

		// unzipFiles
		input = new CloudTestInput();
		input.setClassName(CloudTestUtils.class.getCanonicalName());
		input.setMethodName("unzipFiles");

		paras = new ArrayList<CloudTestInput.CloudTestParameter>();
		paras.add(buildParamater("file", ObjectDigester.toXML(new File(remoteFilePath)).replace("\\", "/")));
		input.setParameter(paras);

		output = testService.doTest(input);
		if (!output.getStatus() && !CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
			return output.getErrorMessage();
		}

		// deleteFiles
		input = new CloudTestInput();
		input.setClassName(CloudTestUtils.class.getCanonicalName());
		input.setMethodName("deleteFiles");

		paras = new ArrayList<CloudTestInput.CloudTestParameter>();
		paras.add(buildParamater("string", ObjectDigester.toXML(remoteFilePath)));
		input.setParameter(paras);

		output = testService.doTest(input);

		if (!output.getStatus()) {
			return output.getErrorMessage();
		} else {
			return null;
		}

	}

	private static String uploading(String localFilePath, String remoteFilePath, String serverName,String testServiceAddress) {

		String error = null;

		try {
			String serverAddress = testServiceAddress;
			if (CommonUtils.isNullOrEmpty(serverAddress)) {
				error = serverName + " server address can not be matched.";
			} else {

				URL url = new URL(serverAddress);
				String requestEndpoint = getUploadServiceEndpoint();
				if (CommonUtils.isNullOrEmpty(requestEndpoint)) {
					throw new Exception(
							"none 'cloudtest.parallel.remote.upload.service.endpoint' configuration is found for cases pushing.");
				}

				URL uploadUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), requestEndpoint);

				int status = HttpClientUtils.upload(uploadUrl.toString(), localFilePath, remoteFilePath);
				if (status != 1) {
					error = "Push cloudtest cases to " + serverName + " failed, cuased by status is :" + status;
				}
			}

		} catch (Exception e) {
			log.warn("Streamed-Distribution failed caused by " + e.getMessage()
					+ ", change to Blocked-Distribution model.", e);
			error = e.getMessage();
		}

		return error;

	}

	private static String getUploadServiceEndpoint() {
		String endpoint = null;
		try {
			endpoint = PluginConfigProxy
					.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_REMOTE_UPLOAD_SERVICE_ENDPOINT);
		} catch (Exception e) {
			return null;
		}
		return endpoint;
	}

	private static PluginConfig.SlaveServer getSlaverServerById(String serverName) {

		Map<String, PluginConfig.SlaveServer> servers = PluginConfigProxy.getSlaveServerMap();
		Set<String> keySet = servers.keySet();
		for (String key : keySet) {

			if (serverName.equals(key)) {
				return servers.get(key);
			}

		}

		return null;
	}

	@Deprecated
	private static CloudTestOutput uploading(TestService testService, byte[] base64Code, String filePath,
			String serverName,String testServiceAddress) {

		CloudTestInput input;
		List<CloudTestParameter> paras;
		String maxSize = getUploadingBlockMaxSize();

		int maxSizeKB = Integer.valueOf(maxSize);
		int bytesLength = ONE_KB * maxSizeKB;
		int blockSize = base64Code.length / bytesLength;
		CloudTestOutput output = new CloudTestOutput();

		// upload file blocks by defined max size.
		if (blockSize == 0) {// if less than defined max size
			input = new CloudTestInput();
			input.setClassName(RemoteCasesManager.class.getCanonicalName());
			input.setMethodName("upload");
			paras = new ArrayList<CloudTestInput.CloudTestParameter>();

			paras.add(buildParamater("byte[]", ObjectDigester.toXML(base64Code)));
			paras.add(buildParamater("string", ObjectDigester.toXML(filePath)));
			input.setParameter(paras);

			output = testService.doTest(input);

			int j = 0;
			while (!output.getStatus() && j < REUPLOAD_MAX_TIMES) {
				log.warn("last data pushing failed, caused by:\n" + output.getErrorMessage()
						+ "\nretry pushing data to remote slave node again...");
				output = testService.doTest(input);
				j++;
			}

		} else {// if large than max size, uploading by file block.
			double lastPrceoss = 0.0;
			for (int i = 0; i <= blockSize; i++) {
				input = new CloudTestInput();
				input.setClassName(RemoteCasesManager.class.getCanonicalName());
				input.setMethodName("upload");
				paras = new ArrayList<CloudTestInput.CloudTestParameter>();

				if (i < blockSize) {

					byte[] postBytes = new byte[bytesLength];
					System.arraycopy(base64Code, (i * bytesLength), postBytes, 0, bytesLength);

					paras.add(buildParamater("byte[]", ObjectDigester.toXML(postBytes)));
				} else {

					int lastBlock = base64Code.length - (i * bytesLength);
					byte[] postBytes = new byte[lastBlock];
					System.arraycopy(base64Code, (i * bytesLength), postBytes, 0, lastBlock);
					paras.add(buildParamater("byte[]", ObjectDigester.toXML(postBytes)));
				}

				paras.add(buildParamater("string", ObjectDigester.toXML(filePath)));
				input.setParameter(paras);
				output = testService.doTest(input);

				if (output.getStatus()) {
					double latestProcess = ((i + 1) / ((blockSize + 1) * 1.0)) * 100;

					if ((latestProcess - lastPrceoss) >= 10 || latestProcess > 95) {
						log.info("uploading to node[" + serverName + "]:" + latestProcess + "% success");
						lastPrceoss = latestProcess;
					}
				} else {
					log.info("uploading splited file block " + i + " failed");
					int j = 0;
					while (!output.getStatus() && j < REUPLOAD_MAX_TIMES) {
						log.warn("last data pushing failed, caused by:\n" + output.getErrorMessage()
								+ "\nretry pushing data to remote slave node again...");
						output = testService.doTest(input);
						j++;
					}

				}

			}
		}

		return output;

	}

	private static String getUploadingBlockMaxSize() {

		String maxSize = DEFAULT_UPLOADING_BLOCK_MAX_SIZE;
		String rpcType = null;
		try {
			maxSize = PluginConfigProxy
					.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_REMOTE_UPLOAD_DATA_PACKAGE_MAX_SIZE);
			rpcType = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE);
		} catch (Exception e) {
			log.warn("setting default max block size to " + maxSize + "KB for remoate cases' uploading");
			maxSize = DEFAULT_UPLOADING_BLOCK_MAX_SIZE; // by default is 2M
		}
		if (CommonUtils.isNullOrEmpty(maxSize)) {
			if ("restful".equalsIgnoreCase(rpcType)) {
				// log.warn("setting default max block size to " + maxSize
				// + "KB for remoate cases' uploading");
				maxSize = DEFAULT_UPLOADING_BLOCK_MAX_SIZE; // 1024k
			} else {
				maxSize = DEFAULT_UPLOADING_BLOCK_MAX_SIZE; // 1024k
			}
		}

		return maxSize;
	}

	public static CloudTestParameter buildParamater(String type, String value) {

		CloudTestParameter p = new CloudTestParameter();

		p.setDataType(type);
		p.setParameterType("0");

		if (value != null) {
			p.setValue(CloudTestConstants.CDATA_START + value + CloudTestConstants.CDATA_END);
		} else {
			p.setValue("null");
		}

		return p;
	}

	public static String getRemoteCloudHomePath(TestService testService) throws Exception {

		if (null == testService) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(ConfigurationProxy.class.getCanonicalName());
		input.setMethodName("getCLOUDTEST_HOME");

		String h = null;
		CloudTestOutput output = testService.doTest(input);

		if (output != null && output.getStatus()) {
			h = (String) ObjectDigester.fromXML(output.getReturns());
		} else {
			if (output == null) {
				throw new Exception(testService.getClass().getCanonicalName() + " getCLOUDTEST_HOME return null");
			} else {
				throw new Exception(testService.getClass().getCanonicalName() + " getCLOUDTEST_HOME meet error with "
						+ output.getErrorMessage());
			}
		}

		return CloudTestUtils.wrapFilePath(h);

		// if (!CommonUtils.isNullOrEmpty(h)) {
		// input.setMethodName("getCloudTestRootPath");
		// output = testService.doTest(input);
		//
		// return CloudTestUtils.wrapFilePath((String) ObjectDigester
		// .fromXML(output.getReturns()));
		// } else {
		// return null;
		// }

	}

	/**
	 * @param testService
	 *            remote test server
	 * @param remoteCloudTestHome
	 *            if null, will use "/cloud_tests/os_username" as cloudtest home
	 *            path by default.
	 * @return
	 */
	public static String setupCloudHomePath(TestService testService, String remoteCloudTestHome) {

		if (testService == null) {
			testService = new CloudTestService();
		}

		String serverContextRealPath = getRemoteContextRealPath(testService);
		File f = new File(serverContextRealPath);

		CloudTestInput input = new CloudTestInput();
		input.setClassName(ConfigurationProxy.class.getCanonicalName());
		input.setMethodName("setCLOUDTEST_HOME");

		List<CloudTestParameter> paras = new ArrayList<CloudTestInput.CloudTestParameter>();

		if (remoteCloudTestHome == null) {
			remoteCloudTestHome = "/cloud_tests/" + ConfigurationProxy.getOsUserName();
		}

		String wrapedHomePath = CloudTestUtils.wrapFilePath(f.getParent() + remoteCloudTestHome);
		paras.add(buildParamater("string", ObjectDigester.toXML(wrapedHomePath)));
		input.setParameter(paras);

		CloudTestOutput output = testService.doTest(input);
		input.setParameter(null);

		// output = refreshCloudTestContext(testService);

		return wrapedHomePath;
	}

	public static void setRemoteParallelJobUser(TestService testService, String parallelJobUser) {

		if (testService == null) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(LocalParallelJobUtil.class.getCanonicalName());

		List<CloudTestParameter> paras = new ArrayList<CloudTestInput.CloudTestParameter>();

		input.setMethodName("setCurrentUser");
		paras.add(buildParamater("string", ObjectDigester.toXML(parallelJobUser)));
		input.setParameter(paras);
		CloudTestOutput output = testService.doTest(input);

	}

	public static Boolean getRemoteIsInParallelJobService(TestService testService) {

		boolean result = false;

		if (testService == null) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(LocalParallelJobUtil.class.getCanonicalName());

		input.setMethodName("isInParallelJobService");
		CloudTestOutput output = testService.doTest(input);

		if (!CommonUtils.isNullOrEmpty(output.getReturns())) {
			Object obj = ObjectDigester.fromXML(output.getReturns());
			if (null != obj) {
				result = (Boolean) obj;
			}
		}

		return result;

	}

	public static String getRemoteWarningMessage(TestService testService) {

		if (testService == null) {
			testService = new CloudTestService();
		}

		CloudTestInput input = new CloudTestInput();
		input.setClassName(LocalParallelJobUtil.class.getCanonicalName());

		input.setMethodName("getWarningMessage");
		CloudTestOutput output = testService.doTest(input);

		if (!CommonUtils.isNullOrEmpty(output.getReturns())) {
			return (String) ObjectDigester.fromXML(output.getReturns());
		} else {
			return null;
		}

	}

	private static CloudTestOutput refreshCloudTestContext(TestService testService) {

		CloudTestInput input = new CloudTestInput();

		CloudTestOutput output;
		input.setClassName(PluginConfigProxy.class.getCanonicalName());
		input.setMethodName("refresh");
		output = testService.doTest(input);

		if (!CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
			input.setMethodName("initCloudTestPluginInstancesMap");
			output = testService.doTest(input);

			input.setMethodName("initParamValueMap");
			output = testService.doTest(input);
		}

		return output;
	}

	public static Map<String, Server> wrapToServer(Map<String, TestService> serverMap) {

		Map<String, Server> map = new HashMap<String, Server>();

		for (String s : serverMap.keySet()) {
			Server server = new Server();

			server.setId(s);
			server.setTestService(serverMap.get(s));

			map.put(s, server);
		}

		return map;
	}

	public static StringBuffer buildServerNamesString(Map<String, TestService> serverMap) {

		StringBuffer strb = new StringBuffer();
		for (String sid : serverMap.keySet()) {

			strb.append(sid + ",");
		}
		return strb;
	}

	static class CaseDataUploadingThread implements Runnable {

		TestService testService;
		String testServiceName;
		String testServiceAddress;
		Map<String, byte[]> fileMap;

		public CaseDataUploadingThread(TestService testService, String name,String address, Map<String, byte[]> fileMap) {

			this.testService = testService;
			this.testServiceName = name;
			this.testServiceAddress = address;
			this.fileMap = fileMap;
		}

		public void run() {

			try {
				this.uploadToSingleNode(this.testService, this.testServiceName,testServiceAddress, this.fileMap);
			} catch (Exception e) {
				log.error("Uploading test case data to " + this.testServiceName + " failure, caused by "
						+ e.getMessage());
			}
		}

		private String uploadToSingleNode(TestService testService, String s,String testServiceAddress, Map<String, byte[]> fileMap)
				throws Exception {

			String remoteHome = getRemoteCloudHomePath(testService);

			if (CommonUtils.isNullOrEmpty(remoteHome)) {
				String newPath = setupCloudHomePath(testService, null);

				if (!CommonUtils.isNullOrEmpty(newPath)) {
					remoteHome = newPath;
				}
			}

			if (!CommonUtils.isNullOrEmpty(remoteHome)) {

				Set<String> fileSet = fileMap.keySet();
				StringBuffer errs = new StringBuffer();

				String e = cleanAllData(testService);

				if (!CommonUtils.isNullOrEmpty(e)) {
					log.warn("Cleanup data from slave node [" + s + "] failure, caused by: " + e);
				}

				long start = System.currentTimeMillis();
				for (String fs : fileSet) {
					// String strCode = Base64.encode(fileMap.get(fs));

					String uri = fs.replace("\\", "/")
							.substring(ConfigurationProxy.getCLOUDTEST_HOME().replace("\\", "/").length());

					String caseFullPath = (remoteHome + "/" + uri).replace("\\", "/");
					String errorMsg = uploadToRemoteSlaveNode(testService, fs, fileMap.get(fs), caseFullPath, s,testServiceAddress);

					if (!CommonUtils.isNullOrEmpty(errorMsg)) {
						errs.append(errorMsg);
						break;
					}

					CloudTestUtils.deleteFiles(fs);
				}
				long end = System.currentTimeMillis();
				if (CommonUtils.isNullOrEmpty(errs)) {
					log.info(
							"Pushing data to remote slave node [" + s + "] done in " + ((end - start) / 1000.00) + "s");
				} else {
					log.error("Pushing data to slave node [" + s + "] failure, caused by \n" + errs);
				}

				refreshCloudTestContext(testService);

			}
			return remoteHome;
		}

	}
}
