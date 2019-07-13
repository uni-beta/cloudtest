package com.unibeta.cloudtest;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import com.unibeta.cloudtest.CloudTestInput.CloudTestParameter;
import com.unibeta.cloudtest.assertion.AssertResult;
import com.unibeta.cloudtest.assertion.AssertService;
import com.unibeta.cloudtest.assertion.CloudTestAssert;
import com.unibeta.cloudtest.config.CacheManager;
import com.unibeta.cloudtest.config.CacheManagerFactory;
import com.unibeta.cloudtest.config.CloudTestCase;
import com.unibeta.cloudtest.config.CloudTestCase.Case;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.UserTransactionPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.parallel.thread.CloudTestSingleServiceExecutor;
import com.unibeta.cloudtest.parallel.util.LocalParallelJobUtil;
import com.unibeta.cloudtest.tool.XmlDataDigester;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.base.vRules4j;
import com.unibeta.vrules.base.vRules4j.Context;
import com.unibeta.vrules.base.vRules4j.Object.Rule.ErrorMessage;
import com.unibeta.vrules.tools.Java2vRules;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

import bsh.EvalError;

/**
 * <code>CloudTestService</code> is a core cloud testing service provider.
 * 
 * @author jordan.xue
 */
@WebService(name = TestService.WEB_SERVICE_NAME_CLOUD_TEST_SERVICE, targetNamespace = TestService.NAME_SPACE_CLOUDTEST_UNIBETA_COM)
public class CloudTestService implements TestService {

	private static final String MSG_PERFORMANCE_ASSERTION_FAILURE = "[PerformanceFailure]";
	private static final String[] ASSERTS = new String[] {}; // new
																// String[]{"org.springframework.util.Assert",/*"junit.framework.Assert",*/"org.junit.Assert"};
	private static final String REGEX_XML_EXPRESSION = "<\\D.*>.*</\\D.*>|<\\D.*/>";
	private static final String POSTFIX_ASSERT_XML = ".assert.xml";
	private static final String PARAMETER_TYPE_BY_CONTEXT = "2";
	private static final String PARAMETER_TYPE_BY_XML_FILE = "1";
	private static final String PARAMETER_TYPE_BY_VALUE = "0";

	private static Logger logger = Logger.getLogger(CloudTestService.class);

	private static Pattern xmlValuePattern = Pattern.compile(REGEX_XML_EXPRESSION, Pattern.DOTALL);

	// private XStream x = new XStream(new DomDriver());

	/**
	 * Test service by parameter<br>
	 * cloudTestServiceByParameter
	 * 
	 * @param input
	 *            - test input object, including class name, method name and
	 *            parameters.
	 * @return test result output
	 */
	@WebMethod(operationName = TestService.OPERATION_NAME_CLOUD_TEST_SERVICE_BY_PARAMETER)
	@WebResult(name = TestService.WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doTest(
			@WebParam(name = TestService.WEB_PARAM_TEST_CASE, mode = Mode.IN) CloudTestInput input) {

		CloudTestOutput checkParallelJobLockRisk = checkParallelJobLock();
		CloudTestOutput output = new CloudTestOutput();

		try {
			output = invoke(input);
		} catch (Exception e) {
			logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e));
			printExceptionStack(output, e);
		} finally {
			CacheManagerFactory.getInstance().clear();
		}

		output.setReturnValue(null);

		resolveParallelJobLockRisk(checkParallelJobLockRisk, output);
		return output;
	}

	/**
	 * Test service by <code>CloudTestCase</code>.
	 * 
	 * @param input
	 *            - test input object, including class name, method name and
	 *            parameters.
	 * @return test result output
	 */
	@WebMethod(exclude = true)
	public CloudTestOutput doTest(CloudTestCase input) {

		CloudTestOutput checkParallelJobLockRisk = checkParallelJobLock();

		CloudTestOutput output = new CloudTestOutput();
		List<CloudTestOutput> list = new ArrayList<CloudTestOutput>();

		List<Case> cases = input.testCase;
		long start = System.currentTimeMillis();

		for (Case c : cases) {

			CloudTestOutput doTestOutput = null;
			String caseId = c.id;
			String eachId = c.eachId;

			caseId = evaluateDataByCondition(caseId, eachId);

			try {
				doTestOutput = invoke(c);
			} catch (Exception e) {
				logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e));
				printExceptionStack(doTestOutput, e);
			}

			doTestOutput.setCaseId(caseId);
			doTestOutput.setReturnValue(null);

			list.add(doTestOutput);
		}

		long end = System.currentTimeMillis();

		CloudTestUtils.processResultStatistics(output, false);

		output.setRunTime((end - start) / 1000.00);
		output.setTestCaseResults(list);
		output.setStatus(true);

		CacheManagerFactory.getInstance().clear();

		resolveParallelJobLockRisk(checkParallelJobLockRisk, output);
		return output;
	}

	private String evaluateDataByCondition(String caseId, String eachId) {

		if (!CommonUtils.isNullOrEmpty(eachId)) {
			try {
				Object eachIdVaule = ObjectDigester.fromJava(eachId);

				if (!CommonUtils.isNullOrEmpty(eachIdVaule)) {
					caseId = caseId + eachIdVaule.toString();
				}
			} catch (Exception e1) {
				logger.warn("evaluate '" + eachId + "' failed caused by: " + e1.getMessage());
			}
		}
		return caseId;
	}

	private void executeDependsCases(Case c) throws Exception {

		if (!CommonUtils.isNullOrEmpty(c.depends)) {
			String[] ids = c.depends.split(",");
			for (int i = 0; i < ids.length; i++) {
				Object object = getDependCaseFromNameSpaceLibs(c.nsLibs, ids[i]);

				if (!CommonUtils.isNullOrEmpty(ids[i]) && null != object) {

					Case testCase = (Case) object;
					if (!testCase.id.equals(c.id)) {

						// below code would lead java heap space OOM issue. remove it on 2018/06/22
						// if (!testCase.nsLibs.contains(c.nsLibs)) {
						// testCase.nsLibs = c.nsLibs + "," + testCase.nsLibs;
						// }

						CloudTestOutput doTestOutput = null;
						Boolean ignore = isIgnoreCase(testCase.ignore);
						boolean exeDepends = false;

						if (null == ignore) {
							this.executeDependsCases(testCase);
							exeDepends = true;

							ignore = isIgnoreCase(testCase.ignore);
						}

						if ("true".equalsIgnoreCase(testCase.ignore.trim()) || (!Boolean.TRUE.equals(ignore))) {

							if (!exeDepends) {
								this.executeDependsCases(testCase);
							}
							doTestOutput = invoke(testCase);

						} else {
							continue;
						}

						if (null != doTestOutput) {
							String assertErr = getFailedAssertMsg(doTestOutput.getFailedAssertResults());
							if (null != doTestOutput && !doTestOutput.getStatus()
									&& !assertErr.contains(MSG_PERFORMANCE_ASSERTION_FAILURE)) {

								throw new Exception(CloudTestConstants.FAILED_DEPENDENT_TESTCASE_DESC_PROFIX
										+ testCase.id + CloudTestConstants.FAILED_DEPENDENT_TESTCASE_DESC_POSTFIX
										+ "\nCaused by "
										+ (doTestOutput.getErrorMessage() == null ? ""
												: doTestOutput.getErrorMessage() + ";")
										+ (assertErr.length() > 0 ? ("assertion failure:" + assertErr) : ""));
							}
						}

					}
				} else {
					logger.warn("Dependent TestCase[" + ids[i] + "] was not found, will be ignored.");
				}
			}
		}
	}

	private String getFailedAssertMsg(List<AssertResult> failedAssertResults) {

		if (null == failedAssertResults || failedAssertResults.size() == 0) {
			return "";
		}

		StringBuffer sb = new StringBuffer();

		for (AssertResult ar : failedAssertResults) {
			sb.append(ar.getErrorMessage() + ";");
		}

		return sb.toString();
	}

	private CloudTestOutput invoke(Case c) throws Exception {

		logger.debug("cloudtest is executing case[id = '" + c.id + "']");

		CloudTestInput test = ConfigurationProxy.converCaseToCloudTestInput(c);
		CloudTestOutput doTestOutput;

		String group = this.evaluateDataByCondition("", c.group);
		if (CommonUtils.isNullOrEmpty(group)) {
			group = c.group;
		}

		doTestOutput = invoke(test);

		if ("true".equalsIgnoreCase(c.returnFlag) && !CommonUtils.isNullOrEmpty(c.returnTo)
				&& doTestOutput.getStatus()) {

			CacheManagerFactory.getInstance().put(CacheManagerFactory.getInstance().CACHE_TYPE_RUNTIME_DATA, c.returnTo,
					doTestOutput.getReturnValue());
		}

		doTestOutput.setGroup(group);
		return doTestOutput;
	}

	/**
	 * Do loading test. Starts 'concurrentNumber' thread in 'inFixedSeconds'. It is
	 * thread-safety for multiple concurrence in short time.
	 * 
	 * @param caseFilePath
	 *            - test case file path, can be the directory or file.
	 * @param concurrentNumber
	 *            - concurrent thread number
	 * @param concurrentSeconds
	 *            - the give fixed time (second)
	 * @return - <code>CloudTestOutput</code>
	 */
	@WebMethod(exclude = true)
	public CloudTestOutput doLoadTest(String caseFilePath, Long concurrentNumber, Long concurrentSeconds) {

		CloudTestOutput output = invokeLoadTest(caseFilePath, null, concurrentNumber, concurrentSeconds,
				concurrentNumber);
		return output;
	}

	/**
	 * Do loading test by given <code>CloudLoadTestInput</code>.
	 * cloudLoadTestServiceByCase.
	 * 
	 * @param loadTestInput
	 *            contains fileName caseId concurrentNumber and concurrentSeconds.
	 * @return <code>CloudTestOutput</code>
	 */
	@WebMethod(operationName = TestService.OPERATION_NAME_CLOUD_LOAD_TEST_SERVICE_BY_CASE)
	@WebResult(name = TestService.WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doLoadTest(
			@WebParam(name = TestService.WEB_PARAM_CLOUD_LOAD_INPUT, mode = Mode.IN) CloudLoadInput loadTestInput) {

		CloudTestOutput output;

		if (null == loadTestInput) {

			CloudTestOutput output1 = new CloudTestOutput();
			output1.setStatus(false);
			output1.setErrorMessage("The cloud load test input is null.");

			output = output1;
		}

		CloudTestOutput checkParallelJobLockRisk = checkParallelJobToken(loadTestInput);
		output = invokeLoadTest(loadTestInput.getFileName(), loadTestInput.getCaseId(),
				loadTestInput.getConcurrentNumber(), loadTestInput.getConcurrentSeconds(),
				loadTestInput.getMaxThreadPoolSize());

		resolveParallelJobLockRisk(checkParallelJobLockRisk, output);
		return output;
	}

	private CloudTestOutput checkParallelJobLock() {

		if (LocalParallelJobUtil.isInParallelJobService()) {
			CloudTestOutput output = new CloudTestOutput();

			output.setErrorMessage(LocalParallelJobUtil.getWarningMessage());

			return output;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unused")
	private CloudTestOutput invokeLoadTest(String caseFilePath, String[] caseId, Long concurrentNumber,
			Long concurrentSeconds) {

		CloudTestOutput output = new CloudTestOutput();

		double waitTime = 0;

		if (null == concurrentNumber) {
			concurrentNumber = 0L;
		}

		if (null != concurrentSeconds && concurrentSeconds > 0) {
			waitTime = concurrentSeconds / (concurrentNumber * 1.0);
		}

		List<CloudTestSingleServiceExecutor> serviceList = new ArrayList<CloudTestSingleServiceExecutor>();
		List<Thread> threadList = new ArrayList<Thread>();

		long start = System.currentTimeMillis();
		long end = -1;
		ThreadGroup group = new ThreadGroup("CloudTestService-" + System.currentTimeMillis());

		try {

			for (int i = 0; i < concurrentNumber; i++) {

				CloudTestSingleServiceExecutor cloudTestServiceThread = new CloudTestSingleServiceExecutor(this,
						caseFilePath, caseId);
				serviceList.add(cloudTestServiceThread);

				Thread thr = new Thread(group, cloudTestServiceThread);
				threadList.add(thr);
			}

			int i = 0;
			start = System.currentTimeMillis();
			for (Thread t : threadList) {
				t.start();
				i++;

				if (i < threadList.size()) {
					Thread.sleep((long) (waitTime * 1000));
				}
			}

			// Waiting for all thread ending, let them join here
			while (group.activeCount() > 0) {
				Thread.sleep((1 * 10));
			}
			end = System.currentTimeMillis();

			for (CloudTestSingleServiceExecutor t : serviceList) {

				if (null != t.getCloudTestOutput()) {
					if (output.getTestCaseResults() == null) {
						output.setTestCaseResults(t.getCloudTestOutput().getTestCaseResults());
					} else {
						output.getTestCaseResults().addAll(t.getCloudTestOutput().getTestCaseResults());
					}

					if (!CommonUtils.isNullOrEmpty(t.getCloudTestOutput().getErrorMessage())) {

						StringBuffer errorMsg = new StringBuffer();
						errorMsg.append(t.getCloudTestOutput().getErrorMessage());

						if (!CommonUtils.isNullOrEmpty(output.getErrorMessage())
								&& output.getErrorMessage().indexOf(errorMsg.toString()) < 0) {
							errorMsg.append("\nFound Error:" + output.getErrorMessage());
						}

						output.setErrorMessage(errorMsg.toString());
					}
				}
			}

			group.destroy();

			if (CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
				output.setStatus(true);
			} else {
				output.setStatus(false);
			}

		} catch (Exception e) {
			String printExceptionStackTrace = CloudTestUtils.printExceptionStackTrace(e);
			logger.error(printExceptionStackTrace);

			output.setErrorMessage(printExceptionStackTrace);
			output.setStatus(false);
		} finally {

			if (end <= 0) {
				end = System.currentTimeMillis();
			}

			output.setRunTime((end - start) / 1000.00);
			CloudTestUtils.processResultStatistics(output, false);
		}

		return output;
	}

	private CloudTestOutput invokeLoadTest(String caseFilePath, String[] caseId, Long concurrentNumber,
			Long concurrentSeconds, Long maxThreadPoolSize) {

		CloudTestOutput checkParallelJobLockRisk = checkParallelJobLock();
		CloudTestOutput output = new CloudTestOutput();

		if (null == concurrentNumber || concurrentNumber < 0) {
			concurrentNumber = 0L;
		}

		if (null == maxThreadPoolSize || maxThreadPoolSize <= 0) {
			if (concurrentNumber > 0) {
				maxThreadPoolSize = concurrentNumber;
			} else {
				maxThreadPoolSize = 1L;
			}
		}

		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxThreadPoolSize.intValue(), maxThreadPoolSize.intValue(),
				60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		double waitTime = 0;

		if (null != concurrentSeconds && concurrentSeconds > 0) {
			waitTime = concurrentSeconds / (concurrentNumber * 1.0);
		}

		List<CloudTestSingleServiceExecutor> serviceList = new ArrayList<CloudTestSingleServiceExecutor>();

		long start = System.currentTimeMillis();
		long end = -1;
		int maxDetailedLoadTestResponseAmount = 100000;

		try {
			maxDetailedLoadTestResponseAmount = CloudTestPluginFactory.getParamConfigServicePlugin()
					.getMaxDetailedLoadTestResponseAmount();

			start = System.currentTimeMillis();

			for (int i = 0; i < concurrentNumber; i++) {

				CloudTestSingleServiceExecutor cloudTestServiceThread = new CloudTestSingleServiceExecutor(this,
						caseFilePath, caseId);
				if (concurrentNumber <= maxDetailedLoadTestResponseAmount) {
					serviceList.add(cloudTestServiceThread);
				}

				executor.execute(cloudTestServiceThread);

				if (i < concurrentNumber - 1) {
					Thread.sleep((long) (waitTime * 1000));
				}
			}

			executor.shutdown();
			// Waiting for all thread ending, let them join here
			boolean done = false;
			while (!done) {
				done = executor.awaitTermination(10, TimeUnit.MILLISECONDS);
			}
			end = System.currentTimeMillis();

			for (CloudTestSingleServiceExecutor t : serviceList) {

				if (null != t.getCloudTestOutput()) {
					if (output.getTestCaseResults() == null) {
						output.setTestCaseResults(t.getCloudTestOutput().getTestCaseResults());
					} else if (null != t.getCloudTestOutput().getTestCaseResults()) {
						output.getTestCaseResults().addAll(t.getCloudTestOutput().getTestCaseResults());
					}

					if (!CommonUtils.isNullOrEmpty(t.getCloudTestOutput().getErrorMessage())) {

						StringBuffer errorMsg = new StringBuffer();
						errorMsg.append(t.getCloudTestOutput().getErrorMessage());

						if (!CommonUtils.isNullOrEmpty(output.getErrorMessage())
								&& output.getErrorMessage().indexOf(errorMsg.toString()) < 0) {
							errorMsg.append("\nFound Error:" + output.getErrorMessage());
						}

						output.setErrorMessage(errorMsg.toString());
					}
				}
			}

			if (CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
				output.setStatus(true);
			} else {
				output.setStatus(false);
			}

		} catch (Exception e) {
			String printExceptionStackTrace = CloudTestUtils.printExceptionStackTrace(e);
			logger.error(printExceptionStackTrace);

			output.setErrorMessage(printExceptionStackTrace);
			output.setStatus(false);
		} finally {

			if (end <= 0) {
				end = System.currentTimeMillis();
			}

			output.setRunTime((end - start) / 1000.00);

			if (concurrentNumber >= maxDetailedLoadTestResponseAmount) {
				output.setReturns("The concurrentNumber is larger than " + maxDetailedLoadTestResponseAmount
						+ ", detailed TestCaseOutputs information was ignored for load test service.");
			}

			CloudTestUtils.processResultStatistics(output, false);
			CacheManagerFactory.getInstance().clear();
		}

		resolveParallelJobLockRisk(checkParallelJobLockRisk, output);
		return output;
	}

	private void printExceptionStack(CloudTestOutput output, Exception e) {

		output.setStatus(false);

		StringBuilder sBuilder = new StringBuilder();

		if (!this.isEmpty(output.getErrorMessage())) {
			sBuilder.append(output.getErrorMessage() + "\n");
		}

		sBuilder.append(CloudTestUtils.printExceptionStackTrace(e).trim());

		// if (null != output.getErrorMsg()
		// && output.getErrorMsg().trim().length() > 0) {
		// sBuilder.append("\n... much more \n" + output.getErrorMsg());
		// }

		output.setErrorMessage(sBuilder.toString());
	}

	/**
	 * Test service by TestCase. If caseId is null, it will execute all cased under
	 * specified case file. <br>
	 * cloudTestServiceByTestCase
	 * 
	 * @param input
	 *            - input cae object, including case file full path and caseId.
	 * @return test result output
	 */
	@WebMethod(operationName = TestService.OPERATION_NAME_CLOUD_TEST_SERVICE_BY_TEST_CASE)
	@WebResult(name = TestService.WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doTest(
			@WebParam(name = TestService.WEB_PARAM_CLOUD_CASE_INPUT, mode = Mode.IN) CloudCaseInput input) {

		CloudTestOutput checkParallelJobLockRisk;
		CloudTestOutput output = new CloudTestOutput();

		if (null == input || input.getFileName() == null) {

			output.setErrorMessage("CloudCaseInput or case fileName is null ");
			output.setStatus(false);

			return output;
		} else {
			checkParallelJobLockRisk = checkParallelJobToken(input);

		}

		/*String[] fileNames = input.getFileName().split(",");
		String[] caseIds = input.getCaseId();*/
		
		List<CloudCaseInput> inputs = CloudTestUtils.resolveCloudCaseInputByURIs(input.getFileName());
		
		List<String> filePathList = new ArrayList<String>();
		List<CloudTestOutput> outputList = new ArrayList<CloudTestOutput>();
		Boolean isDirectory = false;
		Map<String,String[]> caseIdMap = new HashMap<String,String[]>();
		
		for (CloudCaseInput caseInput : inputs) {

			String fileName = caseInput.getFileName();
			caseIdMap.put(CloudTestUtils.getContextedURI(fileName), caseInput.getCaseId());
			
			if (CommonUtils.isNullOrEmpty(fileName )) {
				continue;
			} else {
				fileName = fileName.trim();
			}

			String caseFilePath = ConfigurationProxy.getCloudTestRootPath() + fileName;
			File file = new File(caseFilePath);

			if (!file.exists()) {
				output.setErrorMessage("File Path is null or file does not exist,invalid file name is " + caseFilePath);
				output.setStatus(false);
			}

			if (file.isDirectory()) {
				isDirectory = true;
				filePathList.addAll(CloudTestUtils.getAllFilePathListInFolder(caseFilePath, true));
			} else {
				filePathList.add(caseFilePath);
			}
		}

		if (filePathList.size() == 0) {
			output.setErrorMessage((output.getErrorMessage() == null ? "" : output.getErrorMessage() + ",")
					+ "No runnable TestCase was found from case URIs:" + input.getFileName());
			output.setStatus(false);

			return output;
		}

		Collections.sort(filePathList, new CaseContextPathComparator());
		long start = System.currentTimeMillis();

		try {
			loadCasesDependsToContext(filePathList, null);
			loadCasesTasksToContext(filePathList, input.getCaseId());

			startPreCompileThread(filePathList);

			for (int i = 0; i < filePathList.size(); i++) {

				String filePath = filePathList.get(i);
				String casePath = filePath.substring(ConfigurationProxy.getCloudTestRootPath().length());
				CloudTestCase cloudTestCase = null;

				try {
					String[] caseIds = caseIdMap.get(CloudTestUtils.getContextedURI(filePath));
					
					if (isDirectory) {
						caseIds = null;
					}else {
						caseIds = input.getCaseId();
					}

					// get input list by fileName and caseIds
					cloudTestCase = ConfigurationProxy.loadCloudTestCase(filePath, caseIds);
					if (null == cloudTestCase) {
						continue;
					}

					Boolean ignore = isIgnoreCase(cloudTestCase.ignore);
					boolean exeDepends = false;

					if (null == ignore) {
						executeRootDependsCases(cloudTestCase);
						exeDepends = true;

						ignore = isIgnoreCase(cloudTestCase.ignore);
					}

					if (!Boolean.TRUE.equals(ignore)) {

						if (!exeDepends) {
							executeRootDependsCases(cloudTestCase);
						}

						List<Case> cases = cloudTestCase.testCase;
						String assertFileName = checkAssertFile(filePath, cloudTestCase.assertRuleFile);

						String group = this.evaluateDataByCondition("", cloudTestCase.group);
						if (CommonUtils.isNullOrEmpty(group)) {
							group = cloudTestCase.group;
						}

						for (Case c : cases) {

							if (c != null) {
								executeTestCase(group, c, casePath, assertFileName, outputList);
							}

							CacheManagerFactory.getInstance().remove(
									CacheManagerFactory.getInstance().CACHE_TYPE_TASKS_QUEUE,
									this.buildDependsNsURI(casePath, c.id));
						}
					} else {
						clearAllCaseTasksFromContext(casePath, cloudTestCase);
					}

				} catch (Exception e) {

					if (null != cloudTestCase) {
						clearAllCaseTasksFromContext(casePath, cloudTestCase);
					}
					logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e));

					if (!this.isEmpty(output.getErrorMessage())) {
						output.setErrorMessage(output.getErrorMessage() + "\n......\n"
								+ "Found error in Test Case file, the path is: " + filePath);
					} else {
						output.setErrorMessage("Found error in Test Case file, the path is: " + filePath);
					}

					printExceptionStack(output, e);
				}
			}

		} finally {

			Collections.sort(outputList, new CloudTestUtils.TestCaseOutputComparator());
			output.setTestCaseResults(outputList);

			if (CommonUtils.isNullOrEmpty(output.getErrorMessage())
					&& (null == output.getTestCaseResults() || output.getTestCaseResults().size() == 0)) {
				output.setErrorMessage(CLOUDTEST_ERROR_MESSAGE_NO_TEST_CASE_WAS_FOUND);
				output.setStatus(false);
			}

			CacheManagerFactory.getInstance().clear();

			long end = System.currentTimeMillis();
			output.setRunTime((end - start) / 1000.00);
		}

		CloudTestUtils.processResultStatistics(output, isDirectory);

		resolveParallelJobLockRisk(checkParallelJobLockRisk, output);
		return output;
	}

	private void resolveParallelJobLockRisk(CloudTestOutput checkParallelJobLockRisk, CloudTestOutput output) {

		if (checkParallelJobLockRisk != null) {
			output.setErrorMessage(checkParallelJobLockRisk.getErrorMessage()
					+ (output.getErrorMessage() != null ? output.getErrorMessage() : ""));
		}
	}

	private CloudTestOutput checkParallelJobToken(CloudCaseInput input) {

		CloudTestOutput checkParallelJobLockRisk = this.checkParallelJobLock();
		String[] tokens = LocalParallelJobUtil.parseTokenAndValue(input.getFileName());

		if (tokens != null && tokens.length == 2) {
			input.setFileName(tokens[1]);

			if (tokens[0] != null && tokens[0].equals(LocalParallelJobUtil.getCurrentUser())) {
				checkParallelJobLockRisk = null;
			}
		}

		return checkParallelJobLockRisk;
	}

	private void startPreCompileThread(List<String> filePathList) {

		try {
			String enable = PluginConfigProxy
					.getParamValueByName(CloudTestConstants.CLOUDTEST_ASSERT_PRE_COMPILE_ENABLE);
			if ("true".equalsIgnoreCase(enable)) {
				new Thread(new CaseAssertPreCompileThread(filePathList)).start();
			}
		} catch (Exception e1) {
			logger.info(
					"assertion pre-compile thread was disabled, which can be enabled by 'cloudtest.assert.pre_compile.enable' parameter in plugin config. ");
		}

	}

	private void executeRootDependsCases(CloudTestCase cloudTestCase) throws Exception {

		if (!CommonUtils.isNullOrEmpty(cloudTestCase.depends)) {
			String[] depends = cloudTestCase.depends.split(",");
			String[] imports = cloudTestCase.imports.split(",");

			// for (String impt : imports) {
			// CloudTestCase dependsCloudTestCase = ConfigurationProxy
			// .loadCloudTestCase(CloudTestUtils
			// .resolveTestCaseImportsPath(cloudTestCase.ns,
			// impt)[0], null);
			// this.executeRootDependsCases(dependsCloudTestCase);
			// }

			for (String s : depends) {
				if (isDependsEntireCaseFile(s, imports)) {
					CloudTestCase dependsCloudTestCase = ConfigurationProxy
							.loadCloudTestCase(CloudTestUtils.resolveTestCaseImportsPath(cloudTestCase.ns, s)[0], null);

					if (!dependsCloudTestCase.ns.equals(cloudTestCase.ns)) {
						this.executeRootDependsCases(dependsCloudTestCase);
					}

					for (Case c : dependsCloudTestCase.testCase) {
						if (!isIgnoreCase(c.ignore)) {
							this.executeDependsCases(c);
							invoke(c);
						}
					}
				} else {
					Object object = getDependCaseFromNameSpaceLibs(cloudTestCase.nsLibs, s);
					if (null != object && object instanceof Case) {
						Case c = (Case) object;

						CloudTestCase dependsCloudTestCase = ConfigurationProxy.loadCloudTestCase(c.ns, null);

						if (!dependsCloudTestCase.ns.equals(cloudTestCase.ns)) {
							this.executeRootDependsCases(dependsCloudTestCase);
						}

						this.executeDependsCases(c);
						invoke(c);
					} else {
						logger.warn("Dependent TestCase[" + s + "] was not found, will be ignored.");
					}
				}
			}
		}
	}

	private boolean isDependsEntireCaseFile(String s, String[] imports) {

		if (CommonUtils.isNullOrEmpty(s)) {
			return false;
		}

		for (String impt : imports) {
			if (s.equals(impt)) {
				return true;
			}
		}
		return false;
	}

	private void clearAllCaseTasksFromContext(String casePath, CloudTestCase cloudTestCase) {

		for (Case c : cloudTestCase.testCase) {
			CacheManagerFactory.getInstance().remove(CacheManagerFactory.getInstance().CACHE_TYPE_TASKS_QUEUE,
					this.buildDependsNsURI(casePath, c.id));
		}
	}

	private void loadCasesTasksToContext(List<String> filePathList, String[] caseIds) {

		for (int i = 0; i < filePathList.size(); i++) {
			String filePath = filePathList.get(i);

			try {
				// get input list by fileName and caseIds
				CloudTestCase cloudTestCase = ConfigurationProxy.loadCloudTestCase(filePath, caseIds);

				for (Case c : cloudTestCase.testCase) {
					if (!"true".equalsIgnoreCase(c.ignore)) {
						String uri = this.buildDependsNsURI(
								filePath.substring(ConfigurationProxy.getCloudTestRootPath().length()), c.id);
						CacheManagerFactory.getInstance().put(CacheManagerFactory.getInstance().CACHE_TYPE_TASKS_QUEUE,
								uri, uri);

					}
				}
			} catch (Exception e) {
				logger.warn("Cases loading in " + filePath + " failed, caused by " + e.getMessage() + ".");
			}
		}
	}

	private Object getDependCaseFromNameSpaceLibs(String nsLibs, String depend) {

		String[] libs = nsLibs.split(",");
		Object object = null;

		for (String ns : libs) {
			String buildDependsNsURI = buildDependsNsURI(ns, depend);
			object = CacheManagerFactory.getInstance().get(CacheManagerFactory.getInstance().CACHE_TYPE_TESTCASE,
					buildDependsNsURI);
			if (null != object) {
				return object;
			}
		}

		return object;
	}

	private String buildDependsNsURI(String ns, String depend) {

		return (ns + ":" + depend).replace("\\", "/");
	}

	private Boolean isIgnoreCase(String ignoreExpress) {

		Boolean ignore = false;
		Object ignoreObj = null;

		if (CommonUtils.isNullOrEmpty(ignoreExpress)) {
			return false;
		}

		try {
			ignoreObj = ObjectDigester.fromJava(ignoreExpress);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.warn("eval ingore expression '" + ignoreExpress + "'.caused by:" + e.getMessage());
			return null;
		}

		if (null != ignoreObj && ignoreObj.getClass().isAssignableFrom(Boolean.class)) {
			ignore = (Boolean) ignoreObj;
		}

		return ignore;
	}

	private String checkAssertFile(String filePath, String assertRuleFile) {

		String assertFileName = null;
		String[] assertFileNames = CommonUtils.fetchIncludesFileNames(assertRuleFile, filePath);

		if (assertFileNames != null && assertFileNames.length > 0) {
			assertFileName = assertFileNames[0];
		}

		if (!CommonUtils.isNullOrEmpty(assertFileName)) {

			synchronized (assertFileName) {
				if (!new File(assertFileName).exists()) {
					try {
						buildAssertRuleFile(ConfigurationProxy.loadCloudTestCase(filePath, null), assertFileName);
					} catch (Exception e) {
						assertFileName = null;
						logger.warn("checking assert rule file failure, the assert will be ignored. caused by "
								+ e.getMessage(), e);
					}
				}
			}
		}
		return assertFileName;
	}

	private void loadCasesDependsToContext(List<String> filePathList, List<String> importsLogList) {

		if (null == importsLogList) {
			importsLogList = Collections.synchronizedList(new ArrayList<String>());
		}

		for (int i = 0; i < filePathList.size(); i++) {
			String filePath = filePathList.get(i);

			try {
				// get input list by fileName and caseIds
				CloudTestCase cloudTestCase = ConfigurationProxy.loadCloudTestCase(filePath, null);

				String imports = cloudTestCase.imports;
				if (!CommonUtils.isNullOrEmpty(imports)) {
					File currentFile = new File(filePath);
					List<String> importsList = new ArrayList<String>();
					String[] strs = null;

					String[] splits = imports.split(",");

					for (String impt : splits) {
						strs = CloudTestUtils.resolveTestCaseImportsPath(filePath, impt);

						for (String p : strs) {
							File f = new File(p);

							if (!f.getAbsolutePath().equals(currentFile.getAbsolutePath()) && f.exists()
									&& !importsLogList.contains(f.getPath())) {
								importsList.add(f.getPath());
							}
						}
						importsLogList.addAll(importsList);
					}

					this.loadCasesDependsToContext(importsList, importsLogList);
				}

				List<Case> cases = cloudTestCase.testCase;

				for (Case c : cases) {
					String buildDependsNsURI = buildDependsNsURI(cloudTestCase.ns, c.id);

					c.ns = cloudTestCase.ns;
					CacheManagerFactory.getInstance().put(CacheManagerFactory.getInstance().CACHE_TYPE_TESTCASE,
							buildDependsNsURI, c);
				}

			} catch (Exception e) {
				logger.warn("Cases loading in " + filePath + " failed, caused by " + e.getMessage()
						+ ". depends cases will not be executed.");
			}
		}
	}

	private boolean isEmpty(String errorMsg) {

		return (null == errorMsg) || errorMsg.trim().length() == 0;
	}

	private Object executeTestCase(String group, Case c, String casePath, String assertFileName,
			List<CloudTestOutput> outputList) {

		CloudTestOutput testCaseOutput = new CloudTestOutput();

		Object returnObj = null;
		CloudTestInput input = null;

		Boolean ignore = isIgnoreCase(c.ignore);
		try {
			input = ConfigurationProxy.converCaseToCloudTestInput(c);

			if (!Boolean.TRUE.equals(ignore)) {

				if (!CommonUtils.isNullOrEmpty(c.foreach)) {
					executeForeachCases(casePath, c, group, assertFileName, input, outputList);
					testCaseOutput = null;
				} else {

					boolean exeDepends = false;

					if (null == ignore) {
						this.executeDependsCases(c);
						exeDepends = true;

						ignore = isIgnoreCase(c.ignore);
					}

					if (!Boolean.TRUE.equals(ignore)) {
						if (!exeDepends) {
							this.executeDependsCases(c);
						}

						testCaseOutput = invoke(c);
						exeAndAssertPostsResult(c, testCaseOutput);

						if (testCaseOutput != null) {

							returnObj = testCaseOutput.getReturnValue();
							assertExecutionResult(c, assertFileName, input, testCaseOutput);
						}
					} else {
						testCaseOutput = null;
					}
				}

			} else {
				testCaseOutput = null;
			}

		} catch (Exception e) {
			logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e), e);

			if (ignore != null && testCaseOutput != null) {
				testCaseOutput.setStatus(false);
				testCaseOutput.setTestCase(input);
				printExceptionStack(testCaseOutput, e);
			} else {
				testCaseOutput = null;
			}
		} finally {
			if (null != testCaseOutput) {
				if (CommonUtils.isNullOrEmpty(testCaseOutput.getGroup())) {
					testCaseOutput.setGroup(group);
				}
				testCaseOutput.setCasePath(casePath);
				testCaseOutput.setCaseId(this.evaluateDataByCondition(c.id, c.eachId));
				testCaseOutput.setReturnValue(null);
				outputList.add(testCaseOutput);

				logger.debug(c.id + "@" + casePath + " was done in " + testCaseOutput.getRunTime() + "s");
			}

		}

		return returnObj;
	}

	private void exeAndAssertPostsResult(Case c, CloudTestOutput testCaseOutput) throws Exception {

		List<CloudTestOutput> postsResult = executePostsCases(c);

		if (postsResult != null) {
			for (CloudTestOutput output : postsResult) {
				if (!CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
					testCaseOutput.setErrorMessage(
							(testCaseOutput.getErrorMessage() == null ? "" : testCaseOutput.getErrorMessage())
									+ "post case[id='" + output.getCaseId() + "'] was executed failed, caused by:\n"
									+ output.getErrorMessage());
				}
			}
		}
	}

	private void assertExecutionResult(Case c, String assertFileName, CloudTestInput input,
			CloudTestOutput testCaseOutput) throws Exception {

		Object assertObject = testCaseOutput.getReturnValue();
		if (needAssert(c, assertFileName, testCaseOutput)) {
			// if (assertObject != null) {

			List<AssertResult> list = new AssertService().doAssert(assertFileName, c.assertId, assertObject);

			if (list != null) {
				testCaseOutput.setFailedAssertResults(list);
				testCaseOutput.setStatus(false);
			} else {
				testCaseOutput.setStatus(true);
			}
			// } else {
			// assertNotNull(c, testCaseOutput);
			// }
		}
		if ("false".equalsIgnoreCase(c.returnFlag)) {
			testCaseOutput.setReturns(null);
		}

		if ((null != testCaseOutput.getErrorMessage() && testCaseOutput.getErrorMessage().length() > 0)
				|| (null != testCaseOutput.getFailedAssertResults()
						&& testCaseOutput.getFailedAssertResults().size() > 0)) {
			testCaseOutput.setStatus(false);
			testCaseOutput.setTestCase(input);
		} else {
			testCaseOutput.setStatus(true);
		}

		assertPerformanceTime(c, input, testCaseOutput);
	}

	private void executeForeachCases(String casePath, Case c, String group, String assertFileName, CloudTestInput input,
			List<CloudTestOutput> oututputList) throws Exception {

		Object foreach = ObjectDigester.fromJava(c.foreach);
		if (foreach != null && foreach instanceof Iterable) {

			Iterable interable = (Iterable) foreach;
			int i = 0;

			for (Object eachvar : interable) {
				CacheManagerFactory.getInstance().put(CacheManager.CACHE_TYPE_RUNTIME_DATA, c.eachvar, eachvar);

				CloudTestOutput testCaseOutput = new CloudTestOutput();
				try {

					this.executeDependsCases(c);

					Boolean ignoreCase = this.isIgnoreCase(c.ignore);
					if (null == ignoreCase) {
						logger.warn("eval ingore expression '" + c.ignore + "' failed for below 'eachvar' element:\n"
								+ ObjectDigester.toXML(eachvar));
					} else if (ignoreCase) {
						continue;
					}

					testCaseOutput = invoke(c);

					if (testCaseOutput.getStatus()) {
						exeAndAssertPostsResult(c, testCaseOutput);
					}

					assertExecutionResult(c, assertFileName, input, testCaseOutput);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);

					testCaseOutput.setStatus(false);
					testCaseOutput.setErrorMessage(CloudTestUtils.printExceptionStackTrace(e));
					testCaseOutput.setTestCase(ConfigurationProxy.converCaseToCloudTestInput(c));
				}

				String caseId = c.id;
				if (CommonUtils.isNullOrEmpty(c.eachId)) {
					caseId = c.id + "@" + (++i);
				}

				String eachId = c.eachId;

				caseId = evaluateDataByCondition(caseId, eachId);

				if (CommonUtils.isNullOrEmpty(testCaseOutput.getGroup())) {
					testCaseOutput.setGroup(group);
				}
				testCaseOutput.setCasePath(casePath);
				testCaseOutput.setCaseId(caseId);
				testCaseOutput.setReturnValue(null);

				oututputList.add(testCaseOutput);
			}
		} else {
			String msg = "Case[id= '" + c.id + "'] "
					+ "foreach command will be ignored due to evaluated result is invalid. foreach element is below:\n"
					+ ObjectDigester.toXML(foreach);
			logger.warn(msg);

			CloudTestOutput testCaseOutput = new CloudTestOutput();

			testCaseOutput.setGroup(group);
			testCaseOutput.setCasePath(casePath);
			testCaseOutput.setCaseId(c.id);

			testCaseOutput.setErrorMessage(msg);
			testCaseOutput.setStatus(false);
			testCaseOutput.setTestCase(ConfigurationProxy.converCaseToCloudTestInput(c));

			oututputList.add(testCaseOutput);
		}
	}

	private List<CloudTestOutput> executePostsCases(Case c) throws Exception {

		if (CommonUtils.isNullOrEmpty(c.posts)) {
			return null;
		}
		List<CloudTestOutput> list = new ArrayList<CloudTestOutput>();

		String[] posts = c.posts.split(",");
		for (String post : posts) {
			Object object = getDependCaseFromNameSpaceLibs(c.nsLibs, post);

			if (!CommonUtils.isNullOrEmpty(post) && null != object) {
				Case testCase = (Case) object;

				this.executeDependsCases(testCase);
				CloudTestOutput invokeResult = invoke(testCase);

				invokeResult.setCaseId(c.id);
				list.add(invokeResult);
			} else {
				logger.warn("post case[id = '" + post + "'] is not found, will be ignored.");
			}
		}

		return list;
	}

	private void assertPerformanceTime(Case c, CloudTestInput input, CloudTestOutput testCaseOutput) {

		Double expectedTime = c.assertion.timeout;

		Double runTime = testCaseOutput.getRunTime();
		if (runTime == null) {
			runTime = 0D;
		}

		if (expectedTime != null && expectedTime >= 0 && runTime > expectedTime) {

			AssertResult ar = new AssertResult();
			ar.setErrorMessage(MSG_PERFORMANCE_ASSERTION_FAILURE + "Expected time is " + expectedTime
					+ "s, actual execution time is " + runTime + "s");

			List<AssertResult> assertList = testCaseOutput.getFailedAssertResults();
			if (null == assertList) {
				assertList = new ArrayList<AssertResult>();
			}

			assertList.add(ar);

			testCaseOutput.setFailedAssertResults(assertList);
			testCaseOutput.setTestCase(input);
			testCaseOutput.setStatus(false);
		}
	}

	@SuppressWarnings("unused")
	private void assertNotNull(Case c, CloudTestOutput testCaseOutput) throws Exception {

		Boolean needNull = false;
		Method m = getMethodByCase(c);

		// if none method found, return back
		if (null == m) {
			return;
		}

		String name = CloudTestUtils.formatSimpleName(m.getReturnType()) + CloudTestConstants.ASSERT_RESULT_SUFFIX;

		Pattern p = Pattern.compile(name + ".*==.*null", Pattern.DOTALL);

		if (CommonUtils.isNullOrEmpty(c.assertion.assert_) || "true".equals(c.assertion.assert_.trim())
				|| p.matcher(c.assertion.assert_).find()) {
			needNull = true;
		}

		if (!needNull) {
			testCaseOutput.setStatus(false);

			List<AssertResult> al = testCaseOutput.getFailedAssertResults();
			if (al == null) {
				al = new ArrayList<AssertResult>();
			}

			AssertResult ar = new AssertResult();
			ar.setErrorMessage(c.id + " asserted failure, returned result is null.");

			al.add(ar);
			testCaseOutput.setFailedAssertResults(al);
		}

		// try {
		// Class.forName("bsh.Interpreter");
		//
		// Interpreter bsh = new Interpreter();
		// bsh.set(name, null);
		//
		// Object o = bsh.eval(c.assertion.assert_);
		// if (o != null
		// && (Boolean.class.isAssignableFrom(o.getClass()) || boolean.class
		// .isAssignableFrom(o.getClass()))) {
		// needNull = (Boolean) o;
		// }
		//
		// } catch (Exception e) {
		// Pattern p = Pattern.compile(name + ".*==.*null",
		// Pattern.DOTALL);
		// if (p.matcher(c.assertion.assert_).find()) {
		// needNull = true;
		// }
		// }

	}

	private boolean needAssert(Case c, String assertFileName, CloudTestOutput testCaseOutput) throws Exception {

		boolean isMethodReturnValue = true;

		/*
		 * Method m = getMethodByCase(c); if (null != m && null != m.getReturnType() &&
		 * !void.class.equals(m.getReturnType())) { isMethodReturnValue = true; }
		 */

		return isMethodReturnValue && !CommonUtils.isNullOrEmpty(assertFileName)
				&& assertFileName.toLowerCase().endsWith(POSTFIX_ASSERT_XML) && !CommonUtils.isNullOrEmpty(c.assertId)
				&& testCaseOutput.getStatus() == true && CommonUtils.isNullOrEmpty(testCaseOutput.getErrorMessage());
	}

	private void buildAssertRuleFile(CloudTestCase cloudTestCase, String assertFileName) throws Exception {

		vRules4j rules4j = new vRules4j();
		String[] asserts = ASSERTS;

		List<Context> contextList = new ArrayList<vRules4j.Context>();
		List<com.unibeta.vrules.base.vRules4j.Object> objList = new ArrayList<vRules4j.Object>();
		rules4j.imports = rules4j.imports + "static com.unibeta.cloudtest.util.ObjectDigester.*;";

		for (String ast : asserts) {
			boolean has = true;
			try {
				Class.forName(ast);
			} catch (ClassNotFoundException e) {
				has = false;
			}

			if (has) {
				rules4j.imports = rules4j.imports + "static " + ast + ".*;";
			}

		}
		rules4j.java = "\n" + "com.unibeta.cloudtest.config.CacheManager $cache$ = "
				+ "com.unibeta.cloudtest.config.CacheManagerFactory.getInstance();" + "\n";

		for (Case c : cloudTestCase.testCase) {
			if (CommonUtils.isNullOrEmpty(c.assertId)) {
				continue;
			}

			Method m = this.getMethodByCase(c);

			if (null != m && m.getReturnType() != null && !void.class.equals(m.getReturnType())) {
				try {
					Context ctx = new Context();
					String canonicalName = m.getReturnType().getCanonicalName();

					if (m.getReturnType().isMemberClass()) {
						canonicalName = CloudTestUtils.formatMemberClassCanonicalName(m.getReturnType());
					}

					ctx.className = CloudTestUtils.evalDataType(canonicalName).getCanonicalName();
					ctx.name = CloudTestUtils.formatSimpleName(m.getReturnType())
							+ CloudTestConstants.ASSERT_RESULT_SUFFIX;

					if (!contextList.contains(ctx)) {
						contextList.add(ctx);
					}
				} catch (Exception e) {
					logger.warn("Assert rule evals failure, which should be due to invalid return type of "
							+ m.getReturnType().getCanonicalName() + ". caused by " + e.getMessage(), e);
				}

			}

			com.unibeta.vrules.base.vRules4j.Object obj = new com.unibeta.vrules.base.vRules4j.Object();
			obj.id = c.assertId;
			obj.className = CloudTestAssert.class.getName();
			obj.name = c.id;
			obj.nillable = "false";

			com.unibeta.vrules.base.vRules4j.Object.Rule r = obj.rules[0];
			r.id = c.id + "Rule";
			r.name = r.id;
			r.assert_ = c.assertion.assert_;

			if (CommonUtils.isNullOrEmpty(r.assert_)) {
				r.assert_ = "true";
			}

			ErrorMessage em = new ErrorMessage();
			if (!CommonUtils.isNullOrEmpty(c.assertion.message)) {
				em.message = c.assertion.message;
			} else {
				em.message = c.id + " returned value is invalid.";
			}
			em.id = c.id;

			r.errorMessage = em;
			r.isComplexType = "false";
			r.isMapOrList = "false";

			obj.rules[0] = r;
			objList.add(obj);

		}

		rules4j.contexts = contextList.toArray(new Context[] {});
		rules4j.objects = objList.toArray(new com.unibeta.vrules.base.vRules4j.Object[] {});

		Java2vRules.toXml(rules4j, assertFileName);

	}

	private Class[] buildMethodParamClassArray(Case c) throws Exception {

		CloudTestInput in = ConfigurationProxy.converCaseToCloudTestInput(c);

		Class[] methodParamClassArray = null;

		if (in.getParameter() != null && in.getParameter().size() > 0) {
			methodParamClassArray = new Class[in.getParameter().size()];
			List<Class> cl = new ArrayList<Class>();

			for (CloudTestParameter p : in.getParameter()) {
				cl.add(getDataType(p.getDataType().trim()));
			}

			methodParamClassArray = cl.toArray(methodParamClassArray);
		} else {
			methodParamClassArray = new Class[] {};
		}
		return methodParamClassArray;
	}

	private CloudTestOutput invoke(CloudTestInput input) throws Exception {

		CloudTestOutput output = new CloudTestOutput();
		output.setTimestamp(new Date());
		output.setStatus(true);

		if (!validateInput(input, output)) {
			return output;
		}

		Class c = null;
		// get className and methodName
		String className = input.getClassName().trim();
		String methodName = input.getMethodName().trim();
		// get method input parameters list
		List cloudTestParameterList = input.getParameter();

		int paramLength = 0;
		if (cloudTestParameterList != null) {
			paramLength = cloudTestParameterList.size();
		}

		// parameters type array
		Class[] methodParamClassArray = new Class[paramLength];
		// parameters value array
		Object[] methodParamValueArray = new Object[paramLength];

		CloudTestParameter cloudTestParameter;
		UserTransaction trans = null;

		try {

			for (int i = 0; i < paramLength; i++) {
				cloudTestParameter = (CloudTestParameter) cloudTestParameterList.get(i);

				if (null == cloudTestParameter.getDataType() || cloudTestParameter.getDataType().length() == 0) {
					output.setStatus(false);
					output.setErrorMessage("parameter data type is null!");
					return output;
				}

				methodParamClassArray[i] = getDataType(cloudTestParameter.getDataType().trim());

				// 1:the field value is file path in the CloudTestParameter
				if (PARAMETER_TYPE_BY_XML_FILE.equals(cloudTestParameter.getParameterType())) {
					String xmlDataFile = ConfigurationProxy.getCloudTestRootPath() + cloudTestParameter.getValue();
					File caseDataFile = new File(xmlDataFile);

					if (caseDataFile.exists()) {

						methodParamValueArray[i] = ObjectDigester.fromXML(
								XmlUtils.paserDocumentToString(XmlUtils.getDocumentByFileName(caseDataFile.getPath())));
					} else {
						String xmlData = new XmlDataDigester().toXml(cloudTestParameter.getDataType().trim(),
								caseDataFile.getPath());

						output.setStatus(false);
						output.setErrorMessage(
								"\n test data file was not found, XmlDataDigester created xml file successfully. xml data file is located in "
										+ caseDataFile.getPath());

						methodParamValueArray[i] = ObjectDigester.fromXML(xmlData);
					}
				} // 2:test case is context case
				else if (PARAMETER_TYPE_BY_CONTEXT.equals(cloudTestParameter.getParameterType())) {

					methodParamValueArray[i] = evalueDataFromContext(cloudTestParameter);

				} else if (PARAMETER_TYPE_BY_VALUE.equals(cloudTestParameter.getParameterType())) {
					// the value of the field value is parameter value

					if (null == cloudTestParameter.getValue()) {
						cloudTestParameter.setValue("");
					}

					boolean isXmlData = xmlValuePattern.matcher(cloudTestParameter.getValue()).find();
					if (isXmlData) {
						methodParamValueArray[i] = ObjectDigester.fromXML(cloudTestParameter.getValue());
					} else {
						methodParamValueArray[i] = evalueDataFromContext(cloudTestParameter);
					}
				} else {
					output.setStatus(false);
					output.setErrorMessage("parameter type is invalid, only 0,1 are acceptable in current version. "
							+ "\n  0 stands for the java plain value(e.g \"string value...\", 12,12.5D ) or xml formed data(e.g <string>string value</string>, <int>12</int>,<double>12.5</double>), also java code such as 'new String(\"string value\")'; "
							+ "\n  1 stands for loading data from xml data file, such as 'd:\\data.xml'.");
					return output;
				}
			}

			Method m;
			Object returnValue = null;

			long start = System.currentTimeMillis();
			long end = -1;
			try {

				Object beanObject = null;

				try {
					// get object by bean factory
					Class.forName("org.springframework.beans.factory.BeanFactory");

					BeanFactory beanFactory = CloudTestPluginFactory.getSpringBeanFactoryPlugin();

					beanObject = beanFactory.getBean(className);
					c = beanObject.getClass();
				} catch (Exception e) {
					// get class by className
					try {
						c = Class.forName(className);
					} catch (ClassNotFoundException e1) {
						try {
							boolean isXmlData = xmlValuePattern.matcher(className).find();

							if (isXmlData) {
								beanObject = ObjectDigester.fromXML(className);
							} else {
								beanObject = ObjectDigester.fromJava(className);
							}

							if (null != beanObject) {
								c = beanObject.getClass();
							} else {
								String message = "class '" + className + "' evaluated result is null.\n";
								logger.error(message);
								throw new ClassNotFoundException(message, e1);
							}
						} catch (Exception e2) {

							String message = "'" + className
									+ "' class was not found, evaluated also failure.\nCaused by " + e2.getMessage();
							e2.printStackTrace();
							logger.error(message, e2);
							throw new ClassNotFoundException(message, e1);
						}
					}
				}

				m = getMethod(c, methodName, methodParamClassArray);
				m.setAccessible(true);

				UserTransactionPlugin userTransactionPlugin = CloudTestPluginFactory.getUserTransactionPlugin();
				try {
					if (null != userTransactionPlugin) {
						trans = userTransactionPlugin.getUserTransaction();
					}
				} catch (Exception e) {
					logger.debug(
							"Gets UserTransaction plugin instance failed, UserTransaction control will be disabled.");
				}

				if (null != userTransactionPlugin) {
					userTransactionPlugin.before();
				}

				if (null != trans) {
					trans.begin();
				}

				start = System.currentTimeMillis();

				if (Modifier.isStatic(m.getModifiers())) {
					returnValue = m.invoke(c, methodParamValueArray);
				} else {
					if (null == beanObject) {
						Class implClass = resolveInterfaceImpl(output, c);
						beanObject = implClass.newInstance();
					}
					output = CloudTestPluginFactory.getCaseRunnerPlugin().run(beanObject, m, methodParamValueArray);
					returnValue = output.getReturnValue();
				}

				if (null != trans) {
					trans.commit();
				}

				if (null != userTransactionPlugin) {
					userTransactionPlugin.after();
				}
			} catch (Exception e) {
				printExceptionStack(output, e);
				e.printStackTrace();
				logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e));
				try {
					if (null != trans) {
						trans.rollback();
					}
				} catch (Exception e1) {
					// let it be, no need handle it
				}

			} finally {

				if (end <= 0) {
					end = System.currentTimeMillis();
				}

				if (output.getReturnValue() == null) {
					output.setReturnValue(returnValue);
				}
				if (output.getReturns() == null) {
					try {
						output.setReturns(ObjectDigester.toXML(returnValue));
					} catch (Exception e) {

						logger.warn("Convert result to xml failure caused by " + e.getMessage(), e);
						if (null != returnValue) {
							output.setReturns(returnValue.toString());
						}
					}
				}
				if (output.getRunTime() == null || output.getRunTime() == 0.0) {
					output.setRunTime((end - start) / 1000.00);
				}
			}

		} catch (Exception e) {
			printExceptionStack(output, e);
			logger.error(e.getMessage() + CloudTestUtils.printExceptionStackTrace(e));
		} finally {
			// current is empty
		}

		if (output.getRunTime() == null) {
			output.setRunTime(0D);
		}

		Class targetClass = CloudTestUtils.getProxiedTargetClass(className);

		if (targetClass == null) {
			if (c != null) {
				output.setClassName(c.getCanonicalName());
			} else {
				output.setClassName(className);
			}
		} else {
			output.setClassName(targetClass.getCanonicalName());
		}

		return output;
	}

	private Class resolveInterfaceImpl(CloudTestOutput output, Class c) {

		Class implClass = c;

		if (c.isInterface()) {
			List<Class> l = null;

			try {
				l = CloudTestUtils.findImplementations(c,
						new String[] { CloudTestUtils.getIndexedSearchPackageName(c) }, false);
			} catch (Exception e) {
				// empty
			}

			if (null != l && l.size() > 0) {
				implClass = l.get(0);
				String instantiationWarn = "[InstantiationWarn]Interface '" + c.getCanonicalName()
						+ "' can't be instantiated, cloud test engine find an implementation '"
						+ implClass.getCanonicalName()
						+ "' for this test case execution. For any concern, please check test case definition xml file.";

				logger.warn(instantiationWarn);
				output.setErrorMessage(instantiationWarn);
			}
		}

		return implClass;
	}

	private Method getMethod(Class c, String methodName, Class[] methodParamClassArray) throws NoSuchMethodException {

		Method m = null;
		try {
			m = c.getDeclaredMethod(methodName, methodParamClassArray);
		} catch (Exception e1) {
			m = c.getMethod(methodName, methodParamClassArray);
		}
		return m;
	}

	private Method getMethodByCase(Case cs) {

		Class clazz = null;
		Object beanObject = null;

		Class[] methodParamClassArray;
		String methodName;
		Method method = null;

		try {
			methodParamClassArray = buildMethodParamClassArray(cs);

			String className = cs.className.trim();
			methodName = cs.methodName.trim();

			try {
				// get object by bean factory
				Class springClass = Class.forName("org.springframework.beans.factory.BeanFactory");

				BeanFactory beanFactory = CloudTestPluginFactory.getSpringBeanFactoryPlugin();

				beanObject = beanFactory.getBean(className);
				clazz = beanObject.getClass();
			} catch (Exception e) {
				// get class by className
				try {
					clazz = Class.forName(className);
				} catch (Exception e1) {
					Object fromJava = ObjectDigester.fromJava(className);
					if (null != fromJava) {
						clazz = fromJava.getClass();
					} else {
						throw e1;
					}
				}
			}

			method = this.getMethod(clazz, methodName, methodParamClassArray);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}

		return method;
	}

	private Class getDataType(String name) throws Exception {

		Class clazz = null;

		if (CloudTestConstants.PRIMITIVE_TYPE_MAP.keySet().contains(name)) {
			clazz = CloudTestConstants.PRIMITIVE_TYPE_MAP.get(name);
		} else {
			Object o = null;
			try {
				clazz = Class.forName(name);
			} catch (Exception e) {

				clazz = CloudTestUtils.evalDataType(name);
			}
		}

		return clazz;
	}

	private Object evalueDataFromContext(CloudTestParameter cloudTestParameter) throws Exception {

		Object methodParamValue = null;

		try {
			methodParamValue = ObjectDigester.fromJava(cloudTestParameter.getValue());
		} catch (EvalError e) {

			String errMsg = "Parameter value evaluated error: " + e.getMessage();
			e.printStackTrace();
			logger.error(errMsg, e);

			throw new Exception(errMsg, e);

		}

		return methodParamValue;
	}

	private boolean validateInput(CloudTestInput input, CloudTestOutput output) {

		if (input == null) {
			output.setStatus(false);
			output.setErrorMessage("input object is null");
			return false;
		}
		if (input.getClassName() == null) {
			output.setStatus(false);
			output.setErrorMessage("className is null");
			return false;
		}
		if (input.getMethodName() == null) {
			output.setStatus(false);
			output.setErrorMessage("MethodName is null");
			return false;
		}

		return true;
	}

	static class CaseContextPathComparator implements Comparator<String> {

		public int compare(String o1, String o2) {

			if (CommonUtils.isNullOrEmpty(o1) || CommonUtils.isNullOrEmpty(o2)) {
				return 0;
			}

			int i1 = o1.replace("\\", "/").split("/").length;
			int i2 = o2.replace("\\", "/").split("/").length;

			return i1 - i2;
		}

	}

	class CaseAssertPreCompileThread implements Runnable {

		private CaseAssertPreCompileThread() {

		}

		List<String> filePathList = null;

		public CaseAssertPreCompileThread(List<String> filePathList) {

			this.filePathList = filePathList;
		}

		public void run() {

			if (null == filePathList) {
				return;
			}

			for (String filePath : this.filePathList) {
				CloudTestCase cloudTestCase = null;
				try {
					cloudTestCase = ConfigurationProxy.loadCloudTestCase(filePath, null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("CaseAssertPreCompileThread encounter error for " + filePath + ", caused by ", e);
				}

				if (null == cloudTestCase) {
					continue;
				}

				long start = System.currentTimeMillis();

				synchronized (cloudTestCase) {
					String assertFileName = checkAssertFile(filePath, cloudTestCase.assertRuleFile);
					if (!CommonUtils.isNullOrEmpty(assertFileName)) {
						new AssertService().doAssert(assertFileName, "CaseAssertPreCompileThread", new Object());
					}
				}

				long end = System.currentTimeMillis();
				logger.debug(filePath + " assertion pre-compile was done in " + (end - start) / 1000.00 + "s");

			}
		}

	}
}
