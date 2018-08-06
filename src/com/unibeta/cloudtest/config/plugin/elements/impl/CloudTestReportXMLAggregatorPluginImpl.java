package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.assertion.AssertResult;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXML;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXML.TestCaseResults;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXMLAggregatorPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

public class CloudTestReportXMLAggregatorPluginImpl extends ReportGeneratorPluginImpl
		implements CloudTestReportXMLAggregatorPlugin {

	private Map<String, Integer> failedDependentCasesRankingMap = new HashMap<String, Integer>();

	public CloudTestReportXML aggregate(CloudTestOutput cloudTestOutput) throws Exception {

		if (null == cloudTestOutput) {
			return null;
		}

		CloudTestReportXML xml = convertToCloudTestXMLNode(cloudTestOutput);
		xml.testCaseResults = sortAndFilterTestCaseResults(cloudTestOutput);

		return xml;
	}

	class TestCaseResultsComparable implements Comparator<TestCaseResults> {

		public int compare(TestCaseResults o1, TestCaseResults o2) {

			double s1 = getFailureRanking(o1.testCaseResult);
			double s2 = getFailureRanking(o2.testCaseResult);

			int compareDouble = compareDouble(s1, s2);
			if (compareDouble == 0) {
				return o1.group.compareTo(o2.group);
			} else {
				return compareDouble;
			}
		}
	}

	class CloudTestReportXMLComparable implements Comparator<CloudTestReportXML> {

		public int compare(CloudTestReportXML o1, CloudTestReportXML o2) {

			double s1 = getFailureRanking(o1);
			double s2 = getFailureRanking(o2);

			return compareDouble(s1, s2);
		}

	}

	private int compareDouble(double s1, double s2) {

		if (s1 == s2) {
			return 0;
		} else {
			return s2 > s1 ? 1 : -1;
		}
	}

	private double getFailureRanking(List<CloudTestReportXML> o1) {

		if (o1 == null) {
			return 0;
		}

		double i = 0;
		for (CloudTestReportXML tr : o1) {

			i += getFailureRanking(tr);

		}

		return i;
	}

	private double getFailureRanking(CloudTestReportXML o1) {

		if (o1 == null) {
			return 0;
		}

		double i = 0;
		if (!o1.status) {
			i++;
		}

		Integer dependentFailedRanking = failedDependentCasesRankingMap.get(o1.caseId);
		if (dependentFailedRanking != null) {
			i += Math.pow(2, (dependentFailedRanking + 1));
		}

		return i;
	}

	private CloudTestReportXML convertToCloudTestXMLNode(CloudTestOutput cloudTestOutput) throws Exception {

		CloudTestReportXML xml = new CloudTestReportXML();

		xml.resultStatistics = cloudTestOutput.getResultStatistics();
		xml.caseId = cloudTestOutput.getCaseId();
		xml.casePath = cloudTestOutput.getCasePath();
		xml.timestamp = cloudTestOutput.getTimestamp();

		if (AGGREGATOR_TYPE_CASE_PATH.equalsIgnoreCase(
				PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_REPORT_AGGREGATOR_TYPE))) {

			if (!CommonUtils.isNullOrEmpty(cloudTestOutput.getCasePath())) {
				xml.group = cloudTestOutput.getCasePath().replace("/", "/").replace("\\", "/");
			}

			if (!CommonUtils.isNullOrEmpty(cloudTestOutput.getGroup())) {
				xml.group = cloudTestOutput.getGroup();
				String caseFileName = new File(
						ConfigurationProxy.getCloudTestRootPath() + "/" + cloudTestOutput.getCasePath()).getName();
				xml.caseId = caseFileName + "/" + xml.caseId;

			}

			String[] casesPath = xml.group.split(CloudTestConstants.SLAVE_SERVER_NAME_SEPARATOR);
			if (null != casesPath && casesPath.length >= 2) {
				xml.group = casesPath[casesPath.length - 1];
			}
		} else {
			xml.group = cloudTestOutput.getClassName();
		}

		xml.errorMessage = cloudTestOutput.getErrorMessage();
		xml.executionTime = cloudTestOutput.getRunTime();
		xml.failedAssertResults = convertToAssertResultString(cloudTestOutput.getFailedAssertResults());
		xml.returns = cloudTestOutput.getReturns();
		xml.status = cloudTestOutput.getStatus();

		String caseXML = convertToTestCaseString(cloudTestOutput.getTestCase());
		if (!CommonUtils.isNullOrEmpty(caseXML)) {
			xml.testCase = cloudTestOutput.getCasePath() + "\n\n" + caseXML;
		}

		return xml;
	}

	protected List<TestCaseResults> sortAndFilterTestCaseResults(CloudTestOutput testCaseResult) throws Exception {

		List<TestCaseResults> list = new ArrayList<CloudTestReportXML.TestCaseResults>();
		if (null == testCaseResult || testCaseResult.getTestCaseResults() == null) {
			return list;
		}

		Map<String, ArrayList<CloudTestReportXML>> map = new HashMap<String, ArrayList<CloudTestReportXML>>();

		for (CloudTestOutput o : testCaseResult.getTestCaseResults()) {

			CloudTestReportXML x = this.convertToCloudTestXMLNode(o);
			if (null == map.get(x.group)) {
				map.put(x.group, new ArrayList<CloudTestReportXML>());
			}

			map.get(x.group).add(x);

			if (!CommonUtils.isNullOrEmpty(testCaseResult.getErrorMessage())) {
				parseErrorMessage(testCaseResult.getErrorMessage());
			}
			parseErrorMessage(x.errorMessage);

		}

		for (String key : map.keySet()) {
			TestCaseResults caseResults = new TestCaseResults();

			caseResults.group = key;
			caseResults.testCaseResult = map.get(key);

			Collections.sort(caseResults.testCaseResult, new CloudTestReportXMLComparable());

			if (null != caseResults && caseResults.group != null) {
				list.add(caseResults);
			}
		}

		Collections.sort(list, new TestCaseResultsComparable());

		return list;
	}

	private void parseErrorMessage(String x) {

		if (x == null) {
			return;
		}

		String dependentCaseId = parseDependentCaseId(x);

		if (null != dependentCaseId) {
			Integer ranking = this.failedDependentCasesRankingMap.get(dependentCaseId);
			if (null == ranking) {
				this.failedDependentCasesRankingMap.put(dependentCaseId, 1);
			} else {
				this.failedDependentCasesRankingMap.put(dependentCaseId, ranking + 1);
			}
		}
	}

	private static String parseDependentCaseId(String errorMessage) {

		if (CommonUtils.isNullOrEmpty(errorMessage)) {
			return null;
		}

		String dependentProfix = CloudTestConstants.FAILED_DEPENDENT_TESTCASE_DESC_PROFIX;

		int start = errorMessage.indexOf(dependentProfix);
		int end = errorMessage.indexOf(CloudTestConstants.FAILED_DEPENDENT_TESTCASE_DESC_POSTFIX);

		if (0 <= start && start < end) {
			return errorMessage.substring(start + dependentProfix.length(), end).trim();
		} else {
			return null;
		}

	}

	private String convertToTestCaseString(CloudTestInput testCase) throws Exception {

		if (null == testCase) {
			return null;
		} else {
			return XmlUtils
					.formatXML(super.formatToTestCaseSchema(ObjectDigester.toXML(testCase)).replace("&quot;", "\""));
		}

	}

	private String convertToAssertResultString(List<AssertResult> failedAssertResults) {

		if (null == failedAssertResults) {
			return null;
		} else {
			StringBuffer sb = new StringBuffer();

			for (AssertResult ar : failedAssertResults) {
				sb.append(ar.getErrorMessage() + "\n");
			}
			return sb.toString().trim();
		}
	}

}
