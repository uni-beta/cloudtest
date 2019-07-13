package com.unibeta.cloudtest.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.RemoteTestServiceProxyPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.parallel.mapred.JobMapReducer;
import com.unibeta.cloudtest.parallel.mapred.Server;
import com.unibeta.cloudtest.parallel.mapred.Task;
import com.unibeta.cloudtest.parallel.thread.CloudTestLinkedServiceExecutor;
import com.unibeta.cloudtest.parallel.thread.CloudTestSingleServiceExecutor;
import com.unibeta.cloudtest.parallel.util.RemoteParallelUtil;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * The tool of parallel testing jobs.It is designed of MapReduce pattern, which
 * can enable multiple slave server for big data testing task management.
 * 
 * @author jordan.xue
 */
public class ParallelJob {

	static Logger log = Logger.getLogger(ParallelJob.class);

	/**
	 * Invoke parallel computing among all slave sites with indicate whether need
	 * rollback home path or not.
	 * 
	 * @param caseUris
	 * @param mail
	 * @param needRollbackHomePath
	 * @return
	 */
	public static CloudTestOutput run(String caseUris, String mail, boolean needRollbackHomePath) {

		return run(null, null, caseUris, mail, needRollbackHomePath);

	}

	/**
	 * Invoke parallel computing among all slave sites with indicate whether need
	 * rollback home path or not.
	 * 
	 * @param callType
	 *            : restful or webservice
	 * 
	 * @param servers
	 *            key = server name, value = remote address
	 * @param caseUris
	 * @param mail
	 * @param needRollbackHomePath
	 * @return
	 */
	public static CloudTestOutput run(String callType, Map<String, String> servers, String caseUris, String mail,
			boolean needRollbackHomePath) {

		CloudTestOutput output = new CloudTestOutput();

		if (null == caseUris) {

			output.setErrorMessage("CloudCaseInput is null ");
			output.setStatus(false);

			return output;
		}

		if (!CommonUtils.isNullOrEmpty(callType)
				&& (!CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE_RESTFUL.equalsIgnoreCase(callType)
						&& !CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE_WEBSERVICE.equalsIgnoreCase(callType))) {

			output.setErrorMessage("only 'restful' or 'webservice' rpc type can be supported. ");
			output.setStatus(false);

			return output;
		}

		Map<String, TestService> serverMap = null;
		Map<String, String> remoteServersHomePathMap = null;
		String errorMsg = null;

		try {
			RemoteTestServiceProxyPlugin remoteProxy = CloudTestPluginFactory.getRemoteTestServiceProxyPlugin();

			if (CommonUtils.isNullOrEmpty(callType) || CommonUtils.isNullOrEmpty(servers)) {
				serverMap = remoteProxy.create();
			} else {
				serverMap = remoteProxy.create(callType, servers);
			}

			remoteServersHomePathMap = retrieveRemoteServersHomePathMap(serverMap);

			errorMsg = RemoteParallelUtil.checkAndBackupDataToSlavesNode(serverMap, servers);

			// serverMap.put("localhost", new CloudTestService());

			Map<String, Task> taskMap = new LinkedHashMap<String, Task>();
			List<CloudCaseInput> inputs = CloudTestUtils.resolveCloudCaseInputByURIs(caseUris);

			for (CloudCaseInput in : inputs) {
				String caseFilePath = ConfigurationProxy.getCloudTestRootPath() + in.getFileName();
				taskMap.putAll(JobMapReducer.map(caseFilePath));
				
				if (taskMap.get(CloudTestUtils.getContextedURI(caseFilePath)) != null) {
					taskMap.get(CloudTestUtils.getContextedURI(caseFilePath)).setCaseId(in.getCaseId());
				}
			}

			/*
			 * String[] uris = caseUris.split(",");
			 * 
			 * for (String uri : uris) { String caseFilePath =
			 * ConfigurationProxy.getCloudTestRootPath() + uri;
			 * taskMap.putAll(JobMapReducer.map(caseFilePath)); }
			 */

			Map<String, Server> servers_ = RemoteParallelUtil.wrapToServer(serverMap);

			if (servers_.size() == 0) {
				output.setErrorMessage("No valid slave server was found.");
				output.setStatus(false);

				return output;
			}

			output = computing(taskMap, servers_, output);

			StringBuffer strb = RemoteParallelUtil.buildServerNamesString(serverMap);
			log.info(
					"Cloud Test Parallel Job done. Computed by " + strb + " duration is " + output.getRunTime() + "s.");
		} finally {
			recoverRemoteServersHomePath(serverMap, remoteServersHomePathMap, needRollbackHomePath);

			if (!CommonUtils.isNullOrEmpty(errorMsg)) {
				output.setErrorMessage(
						errorMsg + "\n" + (output.getErrorMessage() == null ? "" : output.getErrorMessage()));
			}

			if (serverMap != null && serverMap.size() > 0) {
				String froms = buildSlaveNodeNames(serverMap);
				sendReport(mail, output, froms);
			}

		}

		return output;

	}

	private static String buildSlaveNodeNames(Map<String, TestService> serverMap) {

		Set<String> set = serverMap.keySet();
		StringBuffer froms = new StringBuffer("{");
		int i = 0;
		for (String s : set) {
			if (i == 0) {
				froms.append(s);
			} else {
				froms.append("," + s);
			}
			i++;
		}

		froms.append("}");
		return froms.toString();
	}

	/**
	 * Invoke parallel computing among all slave sites without rollback the
	 * cloudtest home path.
	 * 
	 * @param caseUris
	 *            using ',' to split uris eg: TestCase/mytest
	 * @param mail
	 *            address for testing report, eg: abc@abc.com
	 * @return cloud test result
	 */
	public static CloudTestOutput run(String caseUris, String mail) {

		return run(caseUris, mail, false);
	}

	private static void recoverRemoteServersHomePath(Map<String, TestService> serverMap,
			Map<String, String> remoteServersHomePathMap, boolean needRollbackHomePath) {

		if (remoteServersHomePathMap == null || serverMap == null) {
			return;
		}

		for (String name : serverMap.keySet()) {
			String remoteHomePath = remoteServersHomePathMap.get(name);

			TestService testService = serverMap.get(name);

			if (needRollbackHomePath) {
				RemoteParallelUtil.setupCloudHomePath(testService, remoteHomePath);
				log.info("reset cloudtest home path to '" + remoteHomePath + "' for server[name ='" + name + "']");
			}

			RemoteParallelUtil.setRemoteParallelJobUser(testService, null);
			log.info("released server[name = '" + name + "'] to free for parallel job service.");
		}

	}

	private static Map<String, String> retrieveRemoteServersHomePathMap(Map<String, TestService> serverMap) {

		Map<String, String> map = new HashMap<String, String>();

		for (String key : serverMap.keySet()) {
			try {
				TestService testService = serverMap.get(key);
				String remoteCloudTestHomePath = RemoteParallelUtil.getRemoteCloudHomePath(testService);

				map.put(key, remoteCloudTestHomePath);

			} catch (Exception e) {
				log.error("visit remote cloudtest home path failed.", e);
			}
		}

		return map;
	}

	private static CloudTestOutput computing(Map<String, Task> taskMap, Map<String, Server> servers,
			CloudTestOutput output) {

		long start = System.currentTimeMillis();
		long end = -1;
		List<CloudTestLinkedServiceExecutor> threadList = new ArrayList<CloudTestLinkedServiceExecutor>();
		try {

			log.info("Cloud Test Parallel Job startup...");
			while (!isAllTaskDone(taskMap.values())) {

				ThreadPoolExecutor executor = new ThreadPoolExecutor(servers.size() + 1, servers.size() + 1, 60,
						TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

				JobMapReducer.optimize(taskMap, servers);

				for (Server server : servers.values()) {

					if (!CloudTestConstants.CLOUDTEST_PARALLEL_DISTRIBUTION_MODE_PARALLEL
							.equalsIgnoreCase(PluginConfigProxy
									.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_DISTRIBUTION_MODE))) {
						CloudTestLinkedServiceExecutor command = new CloudTestLinkedServiceExecutor(server);

						executor.execute(command);
						threadList.add(command);

					} else {
						while (!server.getTaskQueue().isEmpty()) {
							Task task = server.getTaskQueue().poll();
							task.setOwner(server.getId());

							executor.execute(new CloudTestSingleServiceExecutor(server, task));
						}
					}

					server.setTasksBlockSize(0);
				}

				executor.shutdown();

				boolean done = false;
				while (!done) {

					if (JobMapReducer.optimize(servers)) {
						for (CloudTestLinkedServiceExecutor t : threadList) {
							t.shutdown();
						}
					}

					done = executor.awaitTermination(2, TimeUnit.MILLISECONDS);
				}

			}

			end = System.currentTimeMillis();

		} catch (Exception e) {
			String printExceptionStackTrace = CloudTestUtils.printExceptionStackTrace(e);
			log.error(printExceptionStackTrace);

			output.setErrorMessage(printExceptionStackTrace);
			output.setStatus(false);
		} finally {

			for (CloudTestLinkedServiceExecutor t : threadList) {
				t.shutdown();
			}

			if (end <= 0) {
				end = System.currentTimeMillis();
			}

			double performanceTimeCost = (end - start) / 1000.00;

			output = JobMapReducer.reduce(taskMap);
			output.setRunTime(performanceTimeCost);
			CloudTestUtils.processResultStatistics(output, true);

			if (CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
				output.setStatus(true);
			} else {
				output.setStatus(false);
			}

			output.setRunTime(performanceTimeCost);
		}
		return output;
	}

	private static void sendReport(String mail, CloudTestOutput output, String froms) {

		try {
			String subject = "Parallel " + "Cloud Test Report@" + new Date();

			String userName = System.getProperty("user.name");
			subject = subject + " From " + froms + " by " + userName;

			com.unibeta.cloudtest.tool.CloudTestReportor.sendReport(mail, subject, output, "parallel");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static boolean isAllTaskDone(Collection<Task> values) {

		for (Task t : values) {

			if (t.getStatus() == Task.STATUS_DONE || t.getStatus() == Task.STATUS_REJECTED) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Invoke parallel computing among all slave sites with specified cloudtest home
	 * path.
	 * 
	 * @param cloudtestHome
	 *            the home path of current target cloudtest located. com.unibeta.
	 *            cloudtest.config.ConfigurationProxy.setCLOUDTEST_HOME
	 *            (cloudtestHome).
	 * @param caseUri
	 *            eg: TestCase/mytest
	 * @param mail
	 *            address for testing report, eg: abc@abc.com
	 * @return cloud test result
	 */
	public static CloudTestOutput run(String cloudtestHome, String caseUri, String mail) {

		String homePath = ConfigurationProxy.getCLOUDTEST_HOME();

		ConfigurationProxy.setCLOUDTEST_HOME(cloudtestHome);
		CloudTestOutput output = run(caseUri, mail);
		ConfigurationProxy.setCLOUDTEST_HOME(homePath);

		return output;
	}

	/**
	 * Invoke parallel computing among all slave sites with specified cloudtest home
	 * path.
	 * 
	 * @param cloudtestHome
	 *            the home path of current target cloudtest located. com.unibeta.
	 *            cloudtest.config.ConfigurationProxy.setCLOUDTEST_HOME
	 *            (cloudtestHome).
	 * @param caseUri
	 *            eg: TestCase/mytest
	 * @param mail
	 *            address for testing report, eg: abc@abc.com
	 * @param needRollbackHomePath
	 *            whether need rollback home path.
	 * @return cloud test result
	 */
	public static CloudTestOutput run(String cloudtestHome, String caseUri, String mail, boolean needRollbackHomePath) {

		String homePath = ConfigurationProxy.getCLOUDTEST_HOME();

		ConfigurationProxy.setCLOUDTEST_HOME(cloudtestHome);
		CloudTestOutput output = run(caseUri, mail, needRollbackHomePath);
		ConfigurationProxy.setCLOUDTEST_HOME(homePath);

		return output;
	}

	/**
	 * Invoke parallel computing among all slave sites with specified cloudtest home
	 * path and remote servers' definition map
	 * 
	 * @param callType
	 *            : restful or webservice
	 * @param servers
	 *            key = server name, value = remote address
	 * @param cloudtestHome
	 *            the home path of current target cloudtest located. com.unibeta.
	 *            cloudtest.config.ConfigurationProxy.setCLOUDTEST_HOME
	 *            (cloudtestHome).
	 * @param caseUri
	 *            eg: TestCase/mytest
	 * @param mail
	 *            address for testing report, eg: abc@abc.com
	 * @param needRollbackHomePath
	 *            whether need rollback home path.
	 * @return cloud test result
	 */
	public static CloudTestOutput run(String callType, Map<String, String> servers, String cloudtestHome,
			String caseUri, String mail, boolean needRollbackHomePath) {

		String homePath = ConfigurationProxy.getCLOUDTEST_HOME();

		ConfigurationProxy.setCLOUDTEST_HOME(cloudtestHome);
		CloudTestOutput output = run(callType, servers, caseUri, mail, needRollbackHomePath);
		ConfigurationProxy.setCLOUDTEST_HOME(homePath);

		return output;
	}

}
