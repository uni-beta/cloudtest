package com.unibeta.cloudtest.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.Endpoint;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.config.CLOUDTEST_HOME$PathProvider;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.restful.LocalRESTfulTestController;
import com.unibeta.cloudtest.tool.AutomationCloudTestManager;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * CloudTestServlet. It will take below actions:<br>
 * 1. Initial local config path by plugin implementation defined in
 * PluginConfig.xml<br>
 * 2. Deploy Automatic Cloud Report service.<br>
 * 3. Publish the cloud test as web service to current server<br>
 * 4. Setup restfull post service<br>
 * <br>
 * servlet configuration example in web.xml:<br>
 * 
 * <pre>
 *  &lt;servlet&gt;
 *         &lt;servlet-name&gt;CloudTestServlet&lt;/servlet-name&gt;
 *         &lt;servlet-class&gt;com.unibeta.cloudtest.servlet.CloudTestServlet&lt;/servlet-class&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;CLOUDTEST_HOME$PathProvider&lt;/param-name&gt;
 *             &lt;param-value&gt;com.unibeta.cloudtest.config.impl.CLOUDTEST_HOME$PathProviderImpl&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;ROOT_FOLDER_NAME&lt;/param-name&gt;
 *             &lt;param-value&gt;cloudtest&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;WEBSERVICE_ENDPOINT_ADDRESS&lt;/param-name&gt;
 *             &lt;param-value&gt;/cloudtest&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;load-on-startup&gt;2&lt;/load-on-startup&gt;
 *     &lt;/servlet&gt;
 *     &lt;servlet-mapping&gt;
 *         &lt;servlet-name&gt;CloudTestServlet&lt;/servlet-name&gt;
 *         &lt;url-pattern&gt;/cloudtest/*&lt;/url-pattern&gt;
 *      &lt;/servlet-mapping&gt;
 *     &lt;servlet&gt;
 *         &lt;servlet-name&gt;vRules4jServlet&lt;/servlet-name&gt;
 *         &lt;servlet-class&gt;com.unibeta.vrules.servlets.VRules4jServlet&lt;/servlet-class&gt;
 *         &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *     &lt;/servlet&gt;
 * </pre>
 * 
 * @author jordan.xue
 */
public class CloudTestServlet extends HttpServlet {

	private static final String WEBSERVICE_ENDPOINT = "WEBSERVICE_ENDPOINT_ADDRESS";
	private static final String PARAMETER_CLOUDTEST_HOME = CLOUDTEST_HOME$PathProvider.class.getSimpleName();
	private static final String PARAMETER_CLOUDTEST_ROOT_FOLDER_NAME = "ROOT_FOLDER_NAME";

	private static final long serialVersionUID = 4105560253817494580L;
	private static Log log = LogFactory.getLog(CloudTestServlet.class);

	private static final String DEFAULT_UPLOADING_BLOCK_MAX_SIZE = "1024";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String operation = request.getHeader(CloudTestConstants.CLOUDTEST_OPERATION);

		if (operation != null && CloudTestConstants.CLOUDTEST_OPERATION_FILE_UPLOAD.equals(operation.toLowerCase())) {
			processFileUploadRequest(request, response);
		} else {
			processCloudTestRequest(request, response);
		}

	}

	private void processCloudTestRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

//		String operation = request.getHeader(CloudTestConstants.CLOUDTEST_OPERATION);
//		if (null == operation || !CloudTestConstants.CLOUDTEST_OPERATION_CLOUDTEST.equals(operation)) {
//			return;
//		}

		try {
			char[] content = new char[request.getContentLength()];
			request.getReader().read(content);

			if (!validate(request, response, new String(content).getBytes("UTF-8"))) {
				return;
			}

			log.debug(CloudTestServlet.class.getSimpleName() + " get request from " + request.getRemoteAddr() + "@"
					+ request.getRemoteUser());

			String reponse = LocalRESTfulTestController.invoke(new String(content));
			response.setContentType("text/xml;charset=UTF-8");

			response.getWriter().write(reponse);

		} catch (Exception e) {
			log.error(e.getMessage(), e);

			CloudTestOutput result = new CloudTestOutput();
			result.setStatus(false);
			result.setErrorMessage(CloudTestUtils.printExceptionStackTrace(e));
			try {
				response.getWriter().write(ObjectSerializer.marshalToXml(result));
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
				response.getWriter().write(CloudTestUtils.printExceptionStackTrace(e));
			}
		} finally {
			if (null != response.getWriter()) {
				response.getWriter().close();
			}

			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}
	}

	private boolean validate(HttpServletRequest request, HttpServletResponse response, byte[] content)
			throws IOException, Exception {

		CloudTestOutput result = new CloudTestOutput();
		result.setStatus(false);

		String length = request.getHeader(CloudTestConstants.HTTP_HEADER_CONTENT_LENGTH);
		String md5 = request.getHeader(CloudTestConstants.HTTP_HEADER_CONTENT_MD5);

		if (!CommonUtils.isNullOrEmpty(length) && !CommonUtils.isNullOrEmpty(md5)) {
			if (!Integer.valueOf(length).equals(content.length)) {
				String errorMsg = "cloudtest request length is invalid, expected is " + length + ", actual is "
						+ content.length;
				log.error(errorMsg);
				result.setErrorMessage(errorMsg);
				response.getWriter().write(ObjectSerializer.marshalToXml(result));
				return false;
			}

			String md5code = CloudTestUtils.getMD5code(content);
			if (!md5.equals(md5code)) {

				String errorMsg = ("cloudtest request MD5 code is invalid, expected is " + md5 + ", actual is "
						+ md5code);
				log.error(errorMsg);
				result.setErrorMessage(errorMsg);
				response.getWriter().write(ObjectSerializer.marshalToXml(result));
				return false;
			}
		}

		return true;
	}

	public void init(ServletConfig conf) throws ServletException {

		super.init(conf);
		try {
			String className = conf.getInitParameter(PARAMETER_CLOUDTEST_HOME);
			CLOUDTEST_HOME$PathProvider configPathServicePlugin = null;

			if (!CommonUtils.isNullOrEmpty(className)) {

				className = className.trim();
				configPathServicePlugin = (CLOUDTEST_HOME$PathProvider) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();

				ConfigurationProxy.setCLOUDTEST_HOME(configPathServicePlugin.getCLOUDTEST_HOME().trim());
			}

			String rootFolderName = conf.getInitParameter(PARAMETER_CLOUDTEST_ROOT_FOLDER_NAME);

			if (!CommonUtils.isNullOrEmpty(rootFolderName)) {
				ConfigurationProxy.setRootFolderName(rootFolderName.trim());
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		} finally {
			AutomationCloudTestManager.deploy();
			deployWebService(conf);
			log.info(this.getClass().getCanonicalName() + " startup done.");
			cleanupOldAssertBinaryClasses();
		}
	}

	private void cleanupOldAssertBinaryClasses() {
		
		String path = ConfigurationProxy.getCloudTestRootPath();
		List<String> list = CommonUtils.searchFilesUnder(path , "^..*.bin$", "", true);

		for (String f : list) {
			CloudTestUtils.deleteFiles(f);
			new File(f).delete();
		}
		
	}

	private void deployWebService(ServletConfig conf) {

		String address = null;

		String webserviceEndPoint = conf.getInitParameter(WEBSERVICE_ENDPOINT);
		if (!CommonUtils.isNullOrEmpty(webserviceEndPoint)) {
			address = webserviceEndPoint;
		}

		if (CommonUtils.isNullOrEmpty(address)) {
			try {
				address = CloudTestPluginFactory.getParamConfigServicePlugin().getWebServiceEndpointAddress();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.info("get 'WebServiceEndpointAddress' failed");
			}
		}

		if (CommonUtils.isNullOrEmpty(address) || "null".equalsIgnoreCase(address) || "none".equalsIgnoreCase(address)
				|| "no".equalsIgnoreCase(address)) {
			// do not publish web service
		} else {

			String addresPath = conf.getServletContext().getContextPath() + "/" + address;
			try {
				Endpoint.publish(address, new CloudTestService());
				log.info(CloudTestService.class.getSimpleName() + " was published as webservice success at "
						+ addresPath);
			} catch (Exception e) {
				try {
					Endpoint.publish(address, new CloudTestService());
				} catch (Exception e1) {
					log.warn(CloudTestService.class.getSimpleName() + " was publised failure to " + addresPath
							+ ", caused by " + e.getMessage());
				}
			}
		}

	}

	private void processFileUploadRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// 检测是不是存在上传文件
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		String operation = request.getHeader(CloudTestConstants.CLOUDTEST_OPERATION);

		if (isMultipart && operation != null && CloudTestConstants.CLOUDTEST_OPERATION_FILE_UPLOAD.equals(operation)) {

			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");

			DiskFileItemFactory factory = new DiskFileItemFactory();

			int maxsize = Integer.valueOf(getUploadingBlockMaxSize());
			// max memory size, default as 1024*1024 = 1M
			factory.setSizeThreshold(maxsize * 1024);

			File tempFolder = new File(ConfigurationProxy.getCLOUDTEST_HOME() + "/temp/");
			if (!tempFolder.exists()) {
				tempFolder.mkdirs();
			}

			// the temp dir is over Threshold size.
			factory.setRepository(tempFolder);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// max uploading file size for 1 file, default as 1G
			upload.setFileSizeMax(1024 * 1024 * 1024);

			// total max size for uploading request, default as 1G
			upload.setSizeMax(1024 * 1024 * 1024);
			upload.setHeaderEncoding("UTF-8");

			List<FileItem> items = null;

			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e) {
				e.printStackTrace();
			}

			if (items != null) {
				String targetFilePath = null;

				for (FileItem item : items) {
					// is not mutil-part type
					if (item.isFormField()) {
						// same as <input type="text" name="content">
						String name = item.getFieldName();
						String value = item.getString();

						if (CloudTestConstants.FILE_UPLOAD_TARGET_FILE_PATH.equals(name)) {
							CloudTestUtils.checkFile(value);

							targetFilePath = value;
							break;
						}

					}
				}

				if (targetFilePath == null) {
					throw new IOException("None targetFilePath is specified for file uploading.");
				}

				for (FileItem item : items) {
					if (!item.isFormField()) {
						try {
							File file = new File(targetFilePath);
							if (file.exists()) {
								file.delete();
							}

							file.createNewFile();

							item.write(file);
							break;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

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
}
