package com.unibeta.cloudtest.constant;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.utils.XmlUtils;

public class CloudTestConstants {

	public static final String ASSERT_RESULT_SUFFIX = "Result";
	public static final String FILE_SEPARATOR = "/";
	public static final String ARRAY_TYPE_TAG = "-array";
	public static final String ARRAY_YYPE_SYMBOL = "[]";

	public static final String POSTFIX_TEST_CASE_XML = ".tc.xml";
	public static final String POSTFIX_UT_CASE_XML = ".ut.xml";

	public static Map<String, Class> PRIMITIVE_TYPE_MAP = buildPrimitiveMap();

	private static Map<String, Class> buildPrimitiveMap() {

		PRIMITIVE_TYPE_MAP = new HashMap<String, Class>();

		PRIMITIVE_TYPE_MAP.put(byte.class.getCanonicalName(), byte.class);
		PRIMITIVE_TYPE_MAP.put(char.class.getCanonicalName(), char.class);
		PRIMITIVE_TYPE_MAP.put(short.class.getCanonicalName(), short.class);
		PRIMITIVE_TYPE_MAP.put(int.class.getCanonicalName(), int.class);
		PRIMITIVE_TYPE_MAP.put(long.class.getCanonicalName(), long.class);

		PRIMITIVE_TYPE_MAP.put(float.class.getCanonicalName(), float.class);
		PRIMITIVE_TYPE_MAP.put(double.class.getCanonicalName(), double.class);

		PRIMITIVE_TYPE_MAP.put(boolean.class.getCanonicalName(), boolean.class);

		return PRIMITIVE_TYPE_MAP;
	}

	public static List<String> JAVA_NUMBERRIC_TYPE_LIST = new ArrayList<String>();
	public static List<String> JAVA_BASIC_TYPE_LIST = buildJavaBasicTypeList();

	private static List<String> buildJavaBasicTypeList() {

		JAVA_BASIC_TYPE_LIST = new ArrayList<String>();

		JAVA_NUMBERRIC_TYPE_LIST.add(int.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(long.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(byte.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(short.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(float.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(double.class.getName());

		JAVA_NUMBERRIC_TYPE_LIST.add(Long.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(Integer.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(Double.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(Float.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(Short.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(Byte.class.getName());
		JAVA_NUMBERRIC_TYPE_LIST.add(BigDecimal.class.getName());

		JAVA_BASIC_TYPE_LIST.add(char.class.getName());
		JAVA_BASIC_TYPE_LIST.add(boolean.class.getName());
		JAVA_BASIC_TYPE_LIST.add(Character.class.getName());
		JAVA_BASIC_TYPE_LIST.add(Boolean.class.getName());
		JAVA_BASIC_TYPE_LIST.add(String.class.getName());

		JAVA_BASIC_TYPE_LIST.add(StringBuffer.class.getName());
		JAVA_BASIC_TYPE_LIST.add(java.util.Date.class.getName());
		JAVA_BASIC_TYPE_LIST.add(java.sql.Date.class.getName());

		JAVA_BASIC_TYPE_LIST.addAll(JAVA_NUMBERRIC_TYPE_LIST);

		JAVA_BASIC_TYPE_LIST.addAll(getExternalJavaBasicTypeDefinitions());

		return JAVA_BASIC_TYPE_LIST;
	}

	private static List<? extends String> getExternalJavaBasicTypeDefinitions() {

		URL url = null;
		try {
			url = CloudTestConstants.class.getResource("/com/unibeta/cloudtest/constant/JavaBasicTypeList.xml");

			if (null != url) {
				if (url.getPath().indexOf("!") > 0) {

					String path = url.getPath();
					String xml = "";

					String zipFileName = path.substring(0, path.indexOf("!"));
					String targetFileName = path.substring(path.indexOf("!") + 2);

					try {
						xml = new String(CloudTestUtils.getZippedFileData(new File(extractFileName(zipFileName)),
								targetFileName));

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						return new ArrayList();
					}
					List list = (List) ObjectDigester.fromXML(xml);

					return list;
				} else {
					String path = url.getPath();

					String xml = XmlUtils.paserDocumentToString(XmlUtils.getDocumentByFileName(extractFileName(path)));

					List list = (List) ObjectDigester.fromXML(xml);

					return list;
				}

			} else {
				return new ArrayList();
			}
		} catch (Exception e) {
			return new ArrayList();
		}

	}

	private static String extractFileName(String path) {

		if (null == path) {
			return path;
		}

		if (path.startsWith("file")) {
			return path.substring(6);
		} else if (path.startsWith("jar")) {
			return path.substring(5);
		}
		// TODO Auto-generated method stub
		return path;
	}

	/**
	 * "cloudtest.AutomationTest.IntervalHours"
	 */
	public static final String CLOUDTEST_AUTOMATION_TEST_INTERVAL_HOURS = "cloudtest.AutomationTest.IntervalHours";
	/**
	 * "cloudtest.WebService.EndpointAddress"
	 */
	public static final String CLOUDTEST_WEB_SERVICE_ENDPOINT_ADDRESS = "cloudtest.WebService.EndpointAddress";
	/**
	 * "cloudtest.UserTransaction.JNDI"
	 */
	public static final String CLOUDTEST_USER_TRANSACTION_JNDI = "cloudtest.UserTransaction.JNDI";

	/**
	 * "cloudtest.LoadTest.MaxDetailedResponseAmount"
	 */
	public static final String CLOUDTEST_MAX_DETAILED_LOAD_TEST_RESPONSE_AMOUNT = "cloudtest.LoadTest.MaxDetailedResponseAmount";
	/**
	 * "cloudtest.MailService.DeployedServerName"
	 */
	public static final String CLOUDTEST_MAIL_ROBOT_SERVICE_DEPLOYED_SERVER_NAME = "cloudtest.MailService.DeployedServerName";
	/**
	 * "cloudtest.mail.StoreProtocal"
	 */
	public static final String CLOUDTEST_MAIL_STORE_PROTOCAL = "cloudtest.mail.StoreProtocal";
	/**
	 * "cloudtest.mail.Pop3Port"
	 */
	public static final String CLOUDTEST_MAIL_POP3_PORT = "cloudtest.mail.Pop3Port";
	/**
	 * "cloudtest.mail.Pop3Host"
	 */
	public static final String CLOUDTEST_MAIL_POP3_HOST = "cloudtest.mail.Pop3Host";
	/**
	 * "cloudtest.mail.UserPassword"
	 */
	public static final String CLOUDTEST_MAIL_USER_PASSWORD = "cloudtest.mail.UserPassword";
	/**
	 * "cloudtest.mail.Username"
	 */
	public static final String CLOUDTEST_MAIL_USERNAME = "cloudtest.mail.Username";
	/**
	 * "cloudtest.mail.Host"
	 */
	public static final String CLOUDTEST_MAIL_HOST = "cloudtest.mail.Host";
	/**
	 * "cloudtest.mail.UserAddress"
	 */
	public static final String CLOUDTEST_MAIL_USER_ADDRESS = "cloudtest.mail.UserAddress";
	/**
	 * "cloudtest.AutomationTest.SwitchFlag"
	 */
	public static final String CLOUDTEST_AUTOMATION_TEST_SWITCH_FLAG = "cloudtest.AutomationTest.SwitchFlag";
	/**
	 * "cloudtest.Interface.Impls.SearchingIndex"
	 */
	public static final String CLOUDTEST_INTERFACE_IMPLS_SEARCHING_INDEX = "cloudtest.Interface.Impls.SearchingIndex";
	/**
	 * "cloudtest.report.junit.xml.dir"
	 */
	public static final String CLOUDTEST_REPORT_JUNIT_XML_DIR = "cloudtest.report.junit.xml.dir";

	/**
	 * cloudtest.Report.Aggregator.Type
	 */
	public static final String CLOUDTEST_REPORT_AGGREGATOR_TYPE = "cloudtest.Report.Aggregator.Type";

	/**
	 * cloudtest.report.history.index.maximun
	 */
	public static final String CLOUDTEST_REPORT_HISTORY_INDEX_MAXIMUM = "cloudtest.report.history.index.maximum";

	/**
	 * cloudtest.report.hotspots.sampling_rate<br>
	 * a. 'cloudtest.report.hotspots.sampling_rate' for sampling rate in
	 * PluginConfig.xml, sampling amount is below: <br>
	 * (history.index.maximum * hotspots.sampling_rate)<br>
	 * b. hotspots means aggregating the most failed cases together with given
	 * sampling rate.
	 */
	public static final String CLOUDTEST_REPORT_HOTSPOTS_SAMPLING_RATE = "cloudtest.report.hotspots.sampling_rate";

	/**
	 * cloudtest.parallel.remote.net.timeout
	 */
	public static final String CLOUDTEST_PARALLEL_REMOTE_NET_TIMEOUT = "cloudtest.parallel.remote.net.timeout";
	/**
	 * cloudtest.parallel.remote.service.receive_timeout
	 */
	public static final String CLOUDTEST_PARALLEL_REMOTE_SERVICE_RECEIVE_TIMEOUT = "cloudtest.parallel.remote.service.receive_timeout";
	/**
	 * cloudtest.parallel.remote.service.connection_timeout
	 */
	public static final String CLOUDTEST_PARALLEL_REMOTE_SERVICE_CONNECTION_TIMEOUT = "cloudtest.parallel.remote.service.connection_timeout";
	/**
	 * cloudtest.parallel.distribution.mode
	 */
	public static final String CLOUDTEST_PARALLEL_DISTRIBUTION_MODE = "cloudtest.parallel.distribution.mode";
	/**
	 * parallel
	 */
	public static final String CLOUDTEST_PARALLEL_DISTRIBUTION_MODE_PARALLEL = "parallel";

	/**
	 * linked
	 */
	public static final String CLOUDTEST_PARALLEL_DISTRIBUTION_MODE_LINKED = "linked";

	public static final String FAILED_DEPENDENT_TESTCASE_DESC_PROFIX = "Dependent TestCase[id = \"";
	public static final String FAILED_DEPENDENT_TESTCASE_DESC_POSTFIX = "\"] executed failure.";

	/**
	 * cloudtest.assert.pre_compile.enable
	 */
	public static final String CLOUDTEST_ASSERT_PRE_COMPILE_ENABLE = "cloudtest.assert.pre_compile.enable";

	/**
	 * System default parameter of cache instance.<br>
	 * Name: $cache$<br>
	 * Value:com.unibeta.cloudtest.config.CacheManagerFactory.getInstance()
	 */
	public static final String CLOUDTEST_SYSTEM_CACHE = "$cache$";

	/**
	 * System default parameter of CloudObject instance.<br>
	 * Name: $CloudObject$<br>
	 * Value:com.unibeta.cloudtest.util.ObjectDigester.ObjectDigester()
	 */
	public static final String CLOUDTEST_SYSTEM_CLOUD_OBJECT = "$CloudObject$";

	/**
	 * System default parameter of RootPath instance.<br>
	 * Name: $RootPath$<br>
	 * Value:com.unibeta.cloudtest.config.ConfigurationProxy.getCloudTestRootPath()
	 */
	public static final String CLOUDTEST_SYSTEM_ROOT_PATH = "$RootPath$";

	/**
	 * System default parameter of Java2TestCases instance.<br>
	 * Name: $Java2TestCases$<br>
	 * Value:com.unibeta.cloudtest.tool.Java2TestCases.Java2TestCases()
	 */
	public static final String CLOUDTEST_SYSTEM_JAVA2_TEST_CASES = "$Java2TestCases$";

	/**
	 * System default parameter of BeanFactory instance.<br>
	 * Name: $BeanFactory$<br>
	 * Value:com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory.
	 * getSpringBeanFactoryPlugin()
	 */
	public static final String CLOUDTEST_SYSTEM_BEAN_FACTORY = "$BeanFactory$";

	/**
	 * System default parameter of BeanFactory instance.<br>
	 * Name: $beans$<br>
	 * Value:com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory.
	 * getSpringBeanFactoryPlugin()
	 */
	public static final String CLOUDTEST_SYSTEM_BEANS = "$beans$";
	/**
	 * System default parameter of PluginConfig instance.<br>
	 * Name: $PluginConfig$<br>
	 * Value:com.unibeta.cloudtest.config.plugin.PluginConfigProxy
	 */
	public static final String CLOUDTEST_SYSTEM_PLUGIN_CONFIG = "$PluginConfig$";

	/**
	 * jfreechart style setting options: 'LineChart' and 'LineChart3D', by default
	 * is 'LineChart3D'
	 */
	public static final String CLOUDTEST_REPORT_ENGINE_JFREECHART_CHART = "cloudtest.report.engine.jfreechart.chart";

	/**
	 * LineChart3D style of jfreechart
	 */
	public static final String CLOUDTEST_REPORT_ENGINE_JFREECHAR_CHART_LineChart3D = "LineChart3D";

	/**
	 * LineChart style of jfreechart
	 */
	public static final String CLOUDTEST_REPORT_ENGINE_JFREECHAR_CHART_LineChart = "LineChart";

	/**
	 * Trend report engine setting options: 'highcharts' and 'jfreechart', by
	 * default is 'highcharts'
	 */
	public static final String CLOUDTEST_REPORT_ENGINE = "cloudtest.report.engine";
	/**
	 * Trend report engine setting options of highcharts.
	 */
	public static final String CLOUDTEST_REPORT_ENGINE_HIGHCHARTS = "highcharts";

	/**
	 * Trend report engine setting options of jfreechart.
	 */
	public static final String CLOUDTEST_REPORT_ENGINE_JFREECHART = "jfreechart";

	/**
	 * cloud test script engine setting, options value are 'beanshell' and 'groovy'.
	 * by default, it is 'beanshell'.
	 */
	public static final String CLOUDTEST_SCRIPT_ENGINE = "cloudtest.script.engine";

	/**
	 * cloud test script engine setting, options of beanshell. beanshell
	 */
	public static final String CLOUDTEST_SCRIPT_ENGINE_BEANSHELL = "beanshell";
	/**
	 * cloud test script engine setting, options of groovy. groovy
	 */
	public static final String CLOUDTEST_SCRIPT_ENGINE_GROOVY = "groovy";

	/**
	 * cloud test parallel rpc call type. webservice or restful
	 */
	public static final String CLOUDTEST_PARALLEL_RPC_TYPE = "cloudtest.parallel.rpc.type";

	/**
	 * cloud test parallel rpc call type of restful.
	 */
	public static final String CLOUDTEST_PARALLEL_RPC_TYPE_RESTFUL = "restful";

	/**
	 * cloud test parallel rpc call type of webservice.
	 */
	public static final String CLOUDTEST_PARALLEL_RPC_TYPE_WEBSERVICE = "webservice";

	/**
	 * cloud test parallel map/reduce strategy of task block type. 'folder' or
	 * 'file'. by default is 'folder'.
	 */
	public static final String CLOUDTEST_PARALLEL_MAPRED_TASK_BLOCK_TYPE = "cloudtest.parallel.mapred.task.block.type";
	/**
	 * cloud test parallel map/reduce strategy of task block type. 'folder'
	 */
	public static final String CLOUDTEST_PARALLEL_MAPRED_TASK_BLOCK_TYPE_FOLDER = "folder";
	/**
	 * cloud test parallel map/reduce strategy of task block type. 'file'
	 */
	public static final String CLOUDTEST_PARALLEL_MAPRED_TASK_BLOCK_TYPE_FILE = "file";
	/**
	 * cloud test recorder, whether ignore duplicated test cases or not.<br>
	 * Option value:true or false, by default it is false.
	 */
	public static final String CLOUDTEST_RECORDER_CASE_IGNORE_DUPLICATED_ENABLE = "cloudtest.recorder.case.ignore_duplicated.enable";

	/**
	 * cloud test recorder, whether remove duplicated test cases or not.<br>
	 * Option value:true or false, by default it is false.
	 */
	public static final String CLOUDTEST_RECORDER_CASE_REMOVE_DUPLICATED_ENABLE = "cloudtest.recorder.case.remove_duplicated.enable";

	/**
	 * CDATA_START:"<![CDATA["
	 */
	public static final String CDATA_START = "<![CDATA[";

	/**
	 * CDATA_END:"]]>"
	 */
	public static final String CDATA_END = "]]>";

	/**
	 * slave server name separator.
	 */
	public static final String SLAVE_SERVER_NAME_SEPARATOR = ":";

	public static final String HTTP_HEADER_CONTENT_MD5 = "Content-MD5";
	public static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Size";

	public static final String CLOUDTEST_PARALLEL_REMOTE_UPLOAD_DATA_PACKAGE_MAX_SIZE = "cloudtest.parallel.remote.upload.data.package.max_size";
	public static final String CLOUDTEST_PARALLEL_REMOTE_UPLOAD_SERVICE_ENDPOINT = "cloudtest.parallel.remote.upload.service.endpoint";
	public static final String CLOUDTEST_PARALLEL_RESTFUL_CHECK_MD5_ENABLE = "cloudtest.parallel.restful.check_md5.enable";

	public static final String FILE_UPLOAD_TARGET_FILE_PATH = "targetFilePath";
	public static final String CLOUDTEST_OPERATION_FILE_UPLOAD = "file-upload";
	public static final String CLOUDTEST_OPERATION = "cloudtest-operation";
	public static final String CLOUDTEST_OPERATION_CLOUDTEST = "cloudtest";

	public static final String CLOUDTEST_SYSTEM_CACHE_TYPE_EHCACHE = "ehcache";
	public static final String CLOUDTEST_SYSTEM_CACHE_TYPE = "cloudtest.system.cache_type";

}
