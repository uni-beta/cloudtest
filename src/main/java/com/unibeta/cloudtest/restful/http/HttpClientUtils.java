package com.unibeta.cloudtest.restful.http;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.CloudTestInput.CloudTestParameter;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.parallel.util.RemoteParallelUtil;
import com.unibeta.cloudtest.restful.RemoteRESTfulTestServiceProxy;
import com.unibeta.cloudtest.tool.RemoteCasesManager;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * HttpClientUtils for post execution.
 * 
 * @author jordan.xue
 */
public class HttpClientUtils {

	/**
	 * post request execution
	 * 
	 * @param url
	 * @param params
	 * @param bytes
	 * @return byte[]
	 * @throws Exception
	 */
	public static byte[] post(String url, Map<String, String> params, byte[] bytes) throws Exception {

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
		HttpPost httpPost = new HttpPost(url);
		try {
			if (null != params && params.size() > 0) {
				for (Map.Entry<String, String> entry : params.entrySet()) {
					httpPost.setHeader(entry.getKey(), entry.getValue());
				}
			}

			if (!params.containsKey("Content-Type")) {
				// set default Content-type=text/xml; charset=UTF-8
				ContentType contentType = ContentType.DEFAULT_BINARY;
				contentType.withCharset("UTF-8");
				httpPost.setHeader("Content-Type", contentType.toString());
			}

			if (bytes != null) {
				HttpEntity requestEntity = new ByteArrayEntity(bytes);
				httpPost.setEntity(requestEntity);
			}

			HttpResponse httpResponse = closeableHttpClient.execute(httpPost);
			if (params != null)
				params.clear();

			// if (httpResponse.getAllHeaders() != null) {
			// for (Header header : httpResponse.getAllHeaders()) {
			// params.put(header.getName(), header.getValue());
			// }
			// }

			HttpEntity responseEntity = httpResponse.getEntity();
			if (responseEntity != null) {
				return EntityUtils.toByteArray(responseEntity);
			}
			return null;
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				closeableHttpClient.close();
			} catch (Exception e) {
				throw e;
			}
		}
	}

	/**
	 * Uploads the local file to remote server via give url's restful api.
	 * 
	 * @param url
	 *            remote server's url
	 * @param localFile,
	 *            local file path
	 * @param remoteFile,
	 *            remote file path
	 * @return 1 if success; otherwise failed.
	 */

	public static int upload(String url, String localFile, String remoteFile) {

		File local = new File(localFile);

		if (local.isFile()) {
			return upload0(url, localFile, remoteFile);
		} else {
			List<String> fileList = CloudTestUtils.getAllFilePathListInFolder(localFile, false);
			for (String file : fileList) {
				String path = new File(file).getPath();
				String name = path.substring(local.getPath().length() + 1, path.length()).replace("\\", "/");

				int s = upload(url, file, remoteFile + "/" + name);
				if (s != 1) {
					return s;
				}
			}
		}

		return 1;
	}

	private static int upload0(String url, String localFile, String remoteFile) {

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		int status = 0;

		try {
			httpClient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost(url);

			httpPost.setHeader(CloudTestConstants.CLOUDTEST_OPERATION,
					CloudTestConstants.CLOUDTEST_OPERATION_FILE_UPLOAD);
			
			String authToken = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_AUTH_TOKEN);
			if(!CommonUtils.isNullOrEmpty(authToken)) {
				httpPost.setHeader(CloudTestConstants.AUTH_AUTHORIZATION, CloudTestConstants.AUTH_BEARER + authToken);
			}
			
			FileBody file = new FileBody(new File(localFile));
			StringBody targetFilePath = new StringBody(remoteFile, ContentType.create("text/plain", Consts.UTF_8));

			HttpEntity reqEntity = MultipartEntityBuilder.create()
					// same as <input type="file" name="file"/>
					.addPart("file", file)
					// same as <input type="text" name="targetFilePath" value=targetFilePath>
					.addPart(CloudTestConstants.FILE_UPLOAD_TARGET_FILE_PATH, targetFilePath).build();

			httpPost.setEntity(reqEntity);

			response = httpClient.execute(httpPost);

			HttpEntity resEntity = response.getEntity();
			EntityUtils.consume(resEntity);

			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				status = response.getStatusLine().getStatusCode();
			} else {
				status = 1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return status;
	}

	/**
	 * Download remote file to local give folder via url's restful api.
	 * 
	 * @param url
	 *            remote server's url
	 * @param localFile,
	 *            local root path
	 * @param remoteFile,
	 *            remote file path
	 * @return error message if failed, otherwise return null.
	 */
	public static String download(String url, String localFile, String remoteFile) {
		TestService service = new RemoteRESTfulTestServiceProxy(url);

		StringBuffer errors = new StringBuffer();

		CloudTestInput input = null;
		List<CloudTestParameter> paras;
		CloudTestOutput output = null;

		input = new CloudTestInput();
		input.setClassName(CloudTestUtils.class.getCanonicalName());
		input.setMethodName("getAllFilePathListInFolder");

		paras = new ArrayList<CloudTestInput.CloudTestParameter>();
		paras.add(RemoteParallelUtil.buildParamater("string", ObjectDigester.toXML(remoteFile)));
		paras.add(RemoteParallelUtil.buildParamater("boolean", ObjectDigester.toXML(false)));
		input.setParameter(paras);

		output = service.doTest(input);
		if (!output.getStatus() && !CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
			return output.getErrorMessage();
		}

		List<String> fileList = (List<String>) ObjectDigester.fromXML(output.getReturns());

		for (String filePath : fileList) {
			input = new CloudTestInput();
			input.setClassName(CloudTestUtils.class.getCanonicalName());
			input.setMethodName("readFilesToBytes");

			paras = new ArrayList<CloudTestInput.CloudTestParameter>();
			paras.add(RemoteParallelUtil.buildParamater("string", ObjectDigester.toXML(filePath)));
			input.setParameter(paras);

			output = service.doTest(input);
			if (!output.getStatus() && !CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
				return output.getErrorMessage();
			}

			Map<String, byte[]> byteMap = (Map<String, byte[]>) ObjectDigester.fromXML(output.getReturns());

			for (String file : byteMap.keySet()) {
				try {
					String targetFilePathName = localFile + "/" + file.replace(":", "_DISK");
					File file2 = new File(targetFilePathName);
					
					if(file2.exists()) {
						file2.delete();
					}
					
					RemoteCasesManager.upload(byteMap.get(file), targetFilePathName);
				} catch (Exception e) {
					errors.append(e.getMessage() + ",");
				}
			}
		}

		if (CommonUtils.isNullOrEmpty(errors.toString())) {
			return null;
		} else {
			return errors.toString();
		}
	}

}
