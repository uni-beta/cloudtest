package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.activation.FileDataSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestInput.CloudTestParameter;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestOutput.ResultStatistics;
import com.unibeta.cloudtest.assertion.AssertResult;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.TestSuite;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXML;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXML.TestCaseResults;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXMLAggregatorPlugin;
import com.unibeta.cloudtest.config.plugin.elements.ReportGeneratorPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.tool.CloudTestReportor;
import com.unibeta.cloudtest.tool.XmlDataDigester;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * Default report in HTML and chart in png format.
 * 
 * @author jordan.xue
 */
public class ReportGeneratorPluginImpl implements ReportGeneratorPlugin {

	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH.mm.ss";
	private static final String REPORT_HOTSPOTS = "hotspots";
	private static final String HISTORY_FOLDER_NAME = "history";
	private static final String INDEX_TABLE_NAME_RESULT_STATISTICS_ENDING = "</table>";
	private static final String HEADLINE_CLOUD_TEST_REPORT_ON = "Cloud Test Report on ";
	private static final String HEADLINE_DOCTYPE_HTML_PUBLIC_W3C_DTD_XHTML_1_0_TRANSITIONAL_EN_HTTP_WWW_W3_ORG_TR_XHTML1_DTD_XHTML1_TRANSITIONAL_DTD = "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>\n";
	private static final String HEADLINE_POWERED_BY_CLOUD_AUTOMATION_TEST_MANAGER_REPORTOR_REG_ENGINE = "----powered by Cloud Automation Test Manager&Reportor&reg; engine</font> </span>\n<br><br><br>";
	private static final String HEADLINE_CLOUD_TEST_REPORT_HISTORY_INDEX_ON = "Cloud Test Report History Index on ";
	private static final String CLOUDTEST_REPORT_HISTORY_INDEX_MAXIMUM_DEFAULT = "1000";
	private static final String INDEX_TABLE_NAME_RESULT_STATISTICS = "<table name ='RESULT_STATISTICS' width='100%' border='1' cellpadding='0' cellspacing='0'>";
	private static final String HEADLINE_RESULT_STATISTICS = "Result Statistics:";
	private static final String HEADLINE_LATEST_CLOUD_TEST_RESULT_STATISTICS = "Latest CloudTest Result Statistics:";

	Date currentDate = null;

	private static final String SPECIAL_SYMBOL_$_REPLACEMENT = "_-";
	private static final String SPECIAL_SYMBOL_$ = "$";

	private static final String TEST_CASE_OUTPUT = "testCaseOutput";
	private static final String TEST_CASE_OUTPUT_CLASS_NAME = CloudTestOutput.class.getName().replace(SPECIAL_SYMBOL_$,
			SPECIAL_SYMBOL_$_REPLACEMENT);

	private static final String PARAMETER = "parameter";
	private static final String PARAMETER_CLASS_NAME = CloudTestParameter.class.getName().replace(SPECIAL_SYMBOL_$,
			SPECIAL_SYMBOL_$_REPLACEMENT);
	private static final String ASSERT_RESULT = "AssertResult";
	private static final String ASSERT_RESULT_CLASS_NAME = AssertResult.class.getName().replace(SPECIAL_SYMBOL_$,
			SPECIAL_SYMBOL_$_REPLACEMENT);
	private static final String TEST_CASE = "testCase";
	private static final String TEST_CASE_CLASS_NAME = CloudTestInput.class.getName().replace(SPECIAL_SYMBOL_$,
			SPECIAL_SYMBOL_$_REPLACEMENT);
	// private static final int MAX_ERROR_MSG_LENGTH = 60;
	protected static String CURRENT_REPORT_PATH_NAME = REPORT_PATH_NAME + File.separator + "default";

	private static Logger log = LoggerFactory.getLogger(CloudTestReportor.class);

	// protected XStream xstream = new XStream(new DomDriver());

	public ReportResult generateReport(CloudTestOutput cloudTestOutput) throws Exception {

		this.currentDate = null;

		try {
			this.generateJunitXML(cloudTestOutput);
		} catch (Exception e) {
			log.warn("generate junit xml failure, " + e.getMessage(), e);
		}

		ReportResult reportResult = new ReportResult();

		reportResult.setContent(this.generateHtmlText(cloudTestOutput));
		String fileName = saveCurrentReportFile(reportResult.getContent());

		addToIndexMap(fileName, cloudTestOutput.getResultStatistics());

		List<DataSource> dataResources = new ArrayList<DataSource>();
		FileDataSource dataSource = new FileDataSource(fileName);

		dataResources.add(dataSource);
		reportResult.setDataResources(dataResources);
		reportResult.setContent(getHistoryIndexHtmlForMailContent(reportResult.getContent()));

		runHistoryIndexThread(cloudTestOutput);
		// runHotspotsReportThread(cloudTestOutput);
		generateHotspotsReport(cloudTestOutput);
		

		this.currentDate = null;
		return reportResult;
	}

	private ReportResult generateHotspotsReport(CloudTestOutput cloudTestOutput) {

		ReportResult result = new ReportResult();
		long start = System.currentTimeMillis();

		try {
			if (!isHotspots()) {
				String hotspotsReportRoot = REPORT_PATH_NAME + File.separator + REPORT_HOTSPOTS + File.separator;

				String hotspotsXmlFolder = hotspotsReportRoot + "xml" + File.separator + "history" + File.separator;
				String historyXmlDataFile = hotspotsXmlFolder + this.getFormattedCurrentDate() + ".xml";

				CloudTestUtils.checkFile(historyXmlDataFile);
				XmlUtils.prettyPrint(ObjectDigester.toXML(cloudTestOutput), historyXmlDataFile);

				CloudTestOutput output = new CloudTestOutput();
				List<CloudTestOutput> testCaseResults = new ArrayList<CloudTestOutput>();
				List<CloudTestOutput> allResults = new ArrayList<CloudTestOutput>();
				Map<String, Object> everFailedMap = new HashMap<String, Object>();

				output.setTestCaseResults(testCaseResults);

				File xmlFolder = new File(hotspotsXmlFolder);
				File[] files = xmlFolder.listFiles();
				List<File> fileList = new ArrayList<File>();

				for (File f : files) {
					fileList.add(f);
				}

				Collections.sort(fileList, new TestReportFileComparable());
				double rate = 1.0;

				try {
					String maxStr = PluginConfigProxy
							.getParamValueByName(CloudTestConstants.CLOUDTEST_REPORT_HOTSPOTS_SAMPLING_RATE);

					if (!CommonUtils.isNullOrEmpty(maxStr)) {
						rate = Double.valueOf(maxStr);
					}

				} catch (Exception e) {
					// empty
				}

				cleanHistoryIndexData(fileList, rate);

				for (File f : fileList) {
					CloudTestOutput o = null;

					try {
						o = (CloudTestOutput) ObjectDigester.fromXMLFile(f.getPath());
					} catch (Exception e) {
						log.error(e.getMessage(),e);
					}

					if (null == o || null == o.getTestCaseResults() || null == o.getResultStatistics()) {
						continue;
					} else {

						for (CloudTestOutput out : o.getTestCaseResults()) {
							if (!out.getStatus()) {
								everFailedMap.put(buildHotspotsKey(out), out.getCasePath());

							}
						}

						allResults.add(o);
					}
				}

				for (CloudTestOutput o : allResults) {

					for (CloudTestOutput ot : o.getTestCaseResults()) {
						String key = buildHotspotsKey(ot);

						if (everFailedMap.get(key) != null) {
							ot.setGroup(ot.getCaseId().replace(":", "."));

							SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);

							if (ot.getTimestamp() == null) {
								ot.setTimestamp(o.getResultStatistics().getTimestamp());
							}
							String time = dateFormat.format(ot.getTimestamp());
							ot.setCaseId(time);

							testCaseResults.add(ot);

						}
					}
				}

				CloudTestUtils.processResultStatistics(output, true);

				long end = System.currentTimeMillis();

				output.getResultStatistics().setDurationTime((end - start) / 1000.0);

				this.setReportFolderName(REPORT_HOTSPOTS);
				result = this.generateReport(output);
			}
		} catch (Exception e) {
			log.warn("generate hotspots report failure, " + e.getMessage());
			e.printStackTrace();

			return result;
		}

		log.info("[cloudtest]Hotspots report is generated in " + (System.currentTimeMillis() - start) / 1000.0 + "s");

		return result;
	}

	private String buildHotspotsKey(CloudTestOutput out) {
		
		if(out.getCasePath() == null) {
			out.setCasePath("null");
		}

		String[] keys = out.getCasePath().split(":");
		String key = out.getCasePath();

		if (keys != null && keys.length >= 1) {
			key = keys[keys.length - 1];
		}

		return key + "@" + out.getCaseId();
	}

	private void addToIndexMap(String fileName, CloudTestOutput.ResultStatistics sesultStatistics) throws Exception {

		String name = new File(fileName).getName();
		Map<String, ResultStatistics> indexMap = new ResultIndexManager().getIndex();
		indexMap.put(name, sesultStatistics);
	}

	private String getHistoryIndexHtmlForMailContent(String content) {

		String emailIndexHtml = content;
		try {
			String lastIndexHtml = fetchHTMLContent("index.html");

			String newHead = getSpecifiedHtmlContent(content,
					HEADLINE_DOCTYPE_HTML_PUBLIC_W3C_DTD_XHTML_1_0_TRANSITIONAL_EN_HTTP_WWW_W3_ORG_TR_XHTML1_DTD_XHTML1_TRANSITIONAL_DTD,
					HEADLINE_POWERED_BY_CLOUD_AUTOMATION_TEST_MANAGER_REPORTOR_REG_ENGINE);
			String lastHead = getSpecifiedHtmlContent(lastIndexHtml,
					HEADLINE_DOCTYPE_HTML_PUBLIC_W3C_DTD_XHTML_1_0_TRANSITIONAL_EN_HTTP_WWW_W3_ORG_TR_XHTML1_DTD_XHTML1_TRANSITIONAL_DTD,
					HEADLINE_POWERED_BY_CLOUD_AUTOMATION_TEST_MANAGER_REPORTOR_REG_ENGINE);

			lastIndexHtml = lastIndexHtml.replace(lastHead, newHead).replace(HEADLINE_CLOUD_TEST_REPORT_ON,
					HEADLINE_CLOUD_TEST_REPORT_HISTORY_INDEX_ON);

			String newTableData = getSpecifiedHtmlContent(content, INDEX_TABLE_NAME_RESULT_STATISTICS,
					INDEX_TABLE_NAME_RESULT_STATISTICS_ENDING);
			String lastTableData = getSpecifiedHtmlContent(lastIndexHtml, INDEX_TABLE_NAME_RESULT_STATISTICS,
					INDEX_TABLE_NAME_RESULT_STATISTICS_ENDING);

			emailIndexHtml = lastIndexHtml.replace(lastTableData, newTableData);

		} catch (Exception e) {
			emailIndexHtml = getSimpleHtmlContent(content);
		}

		return emailIndexHtml;
	}

	protected void runHistoryIndexThread(CloudTestOutput cloudTestOutput) throws InterruptedException {

		Thread indexThread = new Thread(new HistoryIndexWriterThread(cloudTestOutput));
		indexThread.setPriority(Thread.MAX_PRIORITY);

		indexThread.start();
		indexThread.join();
	}

	protected void runHotspotsReportThread(CloudTestOutput cloudTestOutput) {

		Thread indexThread = new Thread(new HotspotsReportThread(cloudTestOutput));
		indexThread.setPriority(Thread.MAX_PRIORITY);

		indexThread.start();
	}

	protected void generateReportHistoryIndex(CloudTestOutput cloudTestOutput) {

		if (null == cloudTestOutput) {
			return;
		}

		StringBuffer historyIndex = new StringBuffer();

		String title = HEADLINE_CLOUD_TEST_REPORT_HISTORY_INDEX_ON + getCurrentDate();
		historyIndex.append(buildReportHeader(title));
		historyIndex.append(buildReportResultStatistics(cloudTestOutput, HEADLINE_LATEST_CLOUD_TEST_RESULT_STATISTICS));

		try {
			historyIndex.append(buildDetailedHistoryIndexResult());

			String inexFilePath = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + "index" + ".html";
			saveFile(historyIndex.toString(), inexFilePath);
		} catch (Exception e) {
			log.error("CloudTest report history index generated failed.\n" + e.getMessage(), e);
			e.printStackTrace();
		}
	}

	private String generateHtmlText(CloudTestOutput cloudTestOutput) throws Exception {

		if (null == cloudTestOutput) {
			return "";
		}

		StringBuffer s = new StringBuffer();
		CloudTestReportXMLAggregatorPlugin aggregatorPlugin = CloudTestPluginFactory
				.getCloudTestReportXMLAggregatorPlugin();

		String title = HEADLINE_CLOUD_TEST_REPORT_ON + getCurrentDate();

		s.append(buildReportHeader(title));
		s.append(buildReportResultStatistics(cloudTestOutput, HEADLINE_RESULT_STATISTICS));

		removeTestReturns(cloudTestOutput);

		String xmlFileName = this.getReportDir(REPORT_FOLDER_NAME_XML) + File.separator + "report.xml";
		String htmlFileName = this.getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + "report.html";
		String xslFileName = ConfigurationProxy.getCloudTestRootPath() + File.separator + "Config" + File.separator
				+ "default-report.xsl";

		CloudTestReportXML reportXMLObj = aggregatorPlugin.aggregate(cloudTestOutput);

		if (isHotspots()) {
			for (TestCaseResults x : reportXMLObj.testCaseResults) {
				Collections.sort(x.testCaseResult, new HotspotsResultComparable());
			}
		}

		if (new File(xmlFileName).exists()) {
			try {
				CloudTestReportXML oldReportXMLObj = (CloudTestReportXML) ObjectSerializer
						.unmarshalToObject(CloudTestUtils.readFileContent(xmlFileName), CloudTestReportXML.class);
				analyzeChangeType(reportXMLObj, oldReportXMLObj);
			} catch (Exception e) {
				log.warn("load history report xml file [" + xmlFileName + "} failed.", e);
			}
		}

		String reportXML = ObjectSerializer.marshalToXml(reportXMLObj);

		XmlUtils.prettyPrint(reportXML, xmlFileName);
		String reportHtml = CloudTestUtils.transformToHtml(xmlFileName, xslFileName, htmlFileName);

		// System.out.println("reportHtml is " +reportHtml);
		// s.append(fetchHTMLContent("report.html"));
		s.append(reportHtml);

		// s.append(buildDetailedResult(cloudTestOutput));
		// s.append(buildReportInXML(cloudTestOutput));

		return s.toString();
	}

	private boolean isHotspots() {
		return CURRENT_REPORT_PATH_NAME.endsWith(REPORT_HOTSPOTS);
	}

	private void analyzeChangeType(CloudTestReportXML reportXMLObj, CloudTestReportXML oldReportXMLObj) {

		if (null == oldReportXMLObj || null == oldReportXMLObj.testCaseResults) {
			return;
		}

		Map<String, CloudTestReportXML> oldReportXmlMap = new HashMap<String, CloudTestReportXML>();
		Map<String, CloudTestReportXML> oldReportXMLLatestMap = new HashMap<String, CloudTestReportXML>();

		for (TestCaseResults xml : oldReportXMLObj.testCaseResults) {

			for (CloudTestReportXML x : xml.testCaseResult) {
				oldReportXmlMap.put(x.casePath + "$" + x.group + "@" + x.caseId, x);

				if (oldReportXMLLatestMap.get(x.casePath + "$" + x.group) == null) {
					oldReportXMLLatestMap.put(x.casePath + "$" + x.group, x);
				}
			}
		}

		for (TestCaseResults xml : reportXMLObj.testCaseResults) {

			for (CloudTestReportXML x : xml.testCaseResult) {
				CloudTestReportXML oldResultX = oldReportXmlMap.get(x.casePath + "$" + x.group + "@" + x.caseId);

				if (oldResultX == null) {

					CloudTestReportXML latestX = oldReportXMLLatestMap.get(x.casePath + "$" + x.group);
					if (this.isHotspots() && latestX != null) {
						if (latestX.status != x.status) {
							x.changeType = x.status ? CloudTestReportXML.CHANGE_TYPE_NEWSUCCESS
									: CloudTestReportXML.CHANGE_TYPE_NEWFAILED;
						}
					} else {
						x.changeType = CloudTestReportXML.CHANGE_TYPE_NEWADDED;
					}
				} else if (oldResultX.status != x.status) {
					x.changeType = x.status ? CloudTestReportXML.CHANGE_TYPE_NEWSUCCESS
							: CloudTestReportXML.CHANGE_TYPE_NEWFAILED;
				}

				if (CloudTestReportXML.CHANGE_TYPE_MAP.get(x.changeType) > CloudTestReportXML.CHANGE_TYPE_MAP
						.get(xml.changeType)) {
					xml.changeType = x.changeType;
				}
			}
		}

	}

	private void removeTestReturns(CloudTestOutput cloudTestOutput) {

		if (null == cloudTestOutput.getTestCaseResults()) {
			return;
		}

		for (CloudTestOutput r : cloudTestOutput.getTestCaseResults()) {
			r.setReturns(null);
		}

	}

	private String saveCurrentReportFile(String string) throws Exception {

		String time = getFormattedCurrentDate();

		String path = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + HISTORY_FOLDER_NAME + File.separator
				+ "Cloud Test Report@" + time + ".html";
		saveFile(string, path);

		String latestCloudTestReportPath = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + "latest" + ".html";
		saveFile(string, latestCloudTestReportPath);

		return path;

	}

	private String getFormattedCurrentDate() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
		String time = dateFormat.format(getCurrentDate());
		return time;
	}

	protected void saveFile(String string, String path) throws IOException, Exception {

		File file = new File(path);
		CloudTestUtils.checkFile(path);

		FileWriter writer = new FileWriter(file);
		try {
			writer.write(string);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (null != writer) {
				writer.close();
			}
		}
	}

	protected Date getCurrentDate() {

		if (null == currentDate) {
			currentDate = new Date();
		}

		return currentDate;
	}

	protected static String getReportDir(String name) {

		String reportPath = CURRENT_REPORT_PATH_NAME;

		String xmlPath = reportPath + File.separator + name + File.separator;

		File path = new File(xmlPath);
		if (!path.exists()) {
			path.mkdirs();
		}

		return xmlPath;
	}

	protected String fetchHTMLContent(String fileName) throws Exception {

		String path = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + fileName;

		return CloudTestUtils.readFileContent(path);
	}

	protected String buildReportInXML(CloudTestOutput cloudTestOutput) throws Exception {

		StringBuffer s = new StringBuffer();

		s.append("<br>" + "<font size=1 face=Arial><span lang=EN-US style='font-size:10pt;font-weight:bold'>"
				+ "Test Result In XML:" + "</span></font>" + "<br>");

		String replaceToSimpleXML = null;

		try {
			replaceToSimpleXML = new XmlDataDigester()
					.format(this.formatToTestCaseSchema(ObjectDigester.toXML((cloudTestOutput))));
		} catch (Exception e) {
			replaceToSimpleXML = this.formatToTestCaseSchema(ObjectDigester.toXML((cloudTestOutput)));
			log.error(e.getMessage(), e);
		}

		s.append("<pre>" + StringEscapeUtils.escapeXml(replaceToSimpleXML) + "</pre>");
		s.append("</body>\n" + "</html>");

		return s.toString();
	}

	protected String buildDetailedHistoryIndexResult() throws Exception {

		StringBuffer s = new StringBuffer();

		s.append(generalHistoryIndexTableHtml(new ResultIndexManager().getIndex()));

		return s.toString();
	}

	private String generalHistoryIndexTableHtml(Map<String, ResultStatistics> indexMap) throws Exception {

		StringBuffer s = new StringBuffer();

		s.append("<font size=1 face=Arial><span lang=EN-US style='font-size:10pt;font-weight:bold'>"
				+ "History Report List:\n" + "</span></font>"
				+ "<table width='100%' border='1' cellpadding='0' cellspacing='0'>\n" + "<tr>\n"
				+ "<th  align='center' width='7%' nowrap>No</th>\n"
				+ "<th  align='center' width='30%' nowrap>CloudTest Report</th>\n"
				+ "<th  align='center' width='9%' nowrap>Size</th>\n" + "<th  align='center' width='9%'>Total</th>\n"
				+ "<th  align='center' width='9%'>Pass</th>\n" + "<th  align='center' width='9%'>Failure</th>\n"
				+ "<th  align='center' width='9%'>Pass Rate</th>\n" + "<th  align='center' width='9%'>Run Time</th>\n"
				+ "<th  align='center' width='9%'>Duration</th>\n" + "</tr>\n");

		File[] files = new File(getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator).listFiles();

		List<File> fileList = new ArrayList<File>();
		// CommonUtils.copyArrayToList(files, fileList);

		for (File f : files) {
			if (f.getName().contains("@")) {
				String historyFile = f.getParentFile().getAbsolutePath() + File.separator + HISTORY_FOLDER_NAME
						+ File.separator + f.getName();
				CommonUtils.copyFile(f.getAbsolutePath(), historyFile);

				fileList.add(new File(historyFile));
				f.delete();
			}
		}

		CommonUtils.copyArrayToList(
				new File(getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + HISTORY_FOLDER_NAME + File.separator)
						.listFiles(),
				fileList);

		Collections.sort(fileList, new TestReportFileComparable());

		List<File> deleteList = cleanHistoryIndexData(fileList, 1.0);

		for (File f : deleteList) {
			indexMap.remove(f.getName());
		}

		if (null != fileList) {
			int i = fileList.size();
			for (File f : fileList) {

				if (f.getName().contains("@")) {

					ResultStatistics rs = null;
					if (null != indexMap.get(f.getName())) {
						rs = indexMap.get(f.getName());

					} else {
						rs = parseResultStatisticsFromHtml(f);
						indexMap.put(f.getName(), rs);
					}

					s.append("<tr>\n" + "<td  align='center' nowrap bgcolor = '#CCCCCC'>" + i-- + "</td>\n"
							+ "<td  align='center' nowrap><a href= './" + HISTORY_FOLDER_NAME + "/" + f.getName() + "'>"
							+ f.getName() + "</a></td>\n" + "</td>\n" + "<td  align='center' nowrap>"
							+ BigDecimal.valueOf((f.length() / 1024.00)).setScale(2, BigDecimal.ROUND_HALF_UP) + "k"
							+ "</td>\n" + "<td  align='center' >" + rs.getTotalAmount() + "</td>\n"
							+ "<td  align='center' >" + rs.getSuccessfulAmount() + "</td>\n" + "<td  align='center'>"
							+ rs.getFailedAmount() + "</td>\n" + "<td  align='center'>"
							+ formatDouble(rs.getPassRate() * 100, 2) + "%</td>\n" + "<td  align='center'>"
							+ formatPerformanceTimeCost(rs.getTotalRunTime()) + "</td>\n" + "<td  align='center'>"
							+ formatPerformanceTimeCost(rs.getDurationTime()) + "</td>\n" + "</tr>\n");
				}
			}
		}

		s.append("</table>\n");
		s.append(getCopyRightBottom());
		return s.toString();
	}

	protected String getCopyRightBottom() {

		return "<div style =\"font:10px arial,sans-serif;text-align:right;width: 100%;height:60px;line-height:60px;\">Copyright &copy;2012-"
				+ Calendar.getInstance().get(Calendar.YEAR)
				+ " CloudTest, Licensed under the Apache License, Version 2.0</div>";
	}

	private List<File> cleanHistoryIndexData(List<File> fileList, double rate) {

		int maximum = Integer.valueOf(CLOUDTEST_REPORT_HISTORY_INDEX_MAXIMUM_DEFAULT);
		int currentSize = fileList.size();

		List<File> deleteList = new ArrayList<File>();
		String maxStr = null;

		try {
			maxStr = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_REPORT_HISTORY_INDEX_MAXIMUM);

			if (!CommonUtils.isNullOrEmpty(maxStr)) {
				maximum = Integer.valueOf(maxStr);
			}

		} catch (Exception e) {
			// empty
		}

		maximum = (int) (maximum * rate);

		if (currentSize < maximum) {
			return deleteList;
		}

		for (int i = (maximum + 0); i < fileList.size(); i++) {
			deleteList.add(fileList.get(i));
		}

		for (File f : deleteList) {
			f.delete();
		}

		fileList.removeAll(deleteList);
		return deleteList;
	}

	private ResultStatistics parseResultStatisticsFromHtml(File f) throws Exception {

		ResultStatistics resultStatistics = new ResultStatistics();
		resultStatistics.setTimestamp(new Date(f.lastModified()));

		String tableXML = getSpecifiedHtmlContent(fetchHTMLContent(HISTORY_FOLDER_NAME + File.separator + f.getName()),
				INDEX_TABLE_NAME_RESULT_STATISTICS, INDEX_TABLE_NAME_RESULT_STATISTICS_ENDING);

		if (null == tableXML) {
			return resultStatistics;
		}

		tableXML = tableXML.replace("'", "\"");

		Document doc = XmlUtils.paserStringToDocument(tableXML);
		NodeList nodeList = doc.getElementsByTagName("td");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			String value = node.getFirstChild().getNodeValue();

			switch (i) {
			case 0: {
				resultStatistics.setTotalAmount(Integer.valueOf(value));
				break;
			}
			case 1: {
				resultStatistics.setSuccessfulAmount(Integer.valueOf(value));
				break;
			}
			case 2: {
				resultStatistics.setFailedAmount(Integer.valueOf(value));
				break;
			}
			case 3: {
				resultStatistics.setPassRate(Double.valueOf(parseContent2DataValue(value)));
				break;
			}
			case 4: {
				resultStatistics.setTotalRunTime(Double.valueOf(parseContent2DataValue(value)));
				break;
			}
			case 5: {
				resultStatistics.setDurationTime(Double.valueOf(parseContent2DataValue(value)));
				break;
			}
			}
		}
		return resultStatistics;

	}

	private String getSpecifiedHtmlContent(String htmlString, String start, String end) throws Exception {

		if (CommonUtils.isNullOrEmpty(htmlString)) {
			return null;
		}

		int startIndex = htmlString.indexOf(start);

		if (startIndex < 0) {
			return null;
		}

		String tableXML = htmlString.substring(startIndex);
		int endIndex = tableXML.indexOf(end);
		tableXML = tableXML.substring(0, endIndex + end.length());

		return tableXML;
	}

	private String parseContent2DataValue(String value) {

		if (value != null && value.length() > 0) {

			String number = value.substring(0, value.length() - 1);

			if (value.toLowerCase().endsWith("%")) {
				return Double.valueOf(number) / 100.00 + "";
			} else if (value.toLowerCase().endsWith("mins")) {
				number = value.substring(0, value.length() - 4);
				return Double.valueOf(number) * 60.00 + "";
			} else if (value.toLowerCase().endsWith("s")) {
				return number;
			}

			return number;
		}

		return "0";
	}

	protected String formatToTestCaseSchema(String s) {

		if (null == s) {
			return "";
		} else {
			return s.replace("<parameter>", "").replace("</parameter>", "").replace(PARAMETER_CLASS_NAME, PARAMETER)
					.replace(TEST_CASE_OUTPUT_CLASS_NAME, TEST_CASE_OUTPUT)
					.replace(ASSERT_RESULT_CLASS_NAME, ASSERT_RESULT).replace(TEST_CASE_CLASS_NAME, TEST_CASE);
		}
	}

	protected String buildReportResultStatistics(CloudTestOutput cloudTestOutput, String headTitle) {

		StringBuffer s = new StringBuffer();

		ResultStatistics resultStatistics = cloudTestOutput.getResultStatistics();

		if (!CommonUtils.isNullOrEmpty(cloudTestOutput.getErrorMessage())
				&& !HEADLINE_LATEST_CLOUD_TEST_RESULT_STATISTICS.equals(headTitle)) {
			s.append("<font size=1 face=Arial><span lang=EN-US style='font-size:10pt;font-weight:bold'>"
					+ "Messages:\n" + "\n" + "</span></font>"
					+ "<table width='100%' border='1' cellpadding='0' cellspacing='0'>\n" + "<tr>\n"
					+ "<td  align='left' > <pre>" + cloudTestOutput.getErrorMessage() + "</pre></td>\n" + "</tr>\n"
					+ "</table>\n" + "<br>");
		}

		if (null != resultStatistics) {

			if (CommonUtils.isNullOrEmpty(headTitle)) {
				headTitle = HEADLINE_RESULT_STATISTICS;
			}
			s.append("<font size=1 face=Arial><span lang=EN-US style='font-size:10pt;font-weight:bold'>" + headTitle
					+ "\n\n" + "</span></font>"
					+ "<table name ='RESULT_STATISTICS' width='100%' border='1' cellpadding='0' cellspacing='0'>\n"
					+ "<tr>\n" + "<th  align='center' width='15%'>Total</th>\n"
					+ "<th  align='center' width='15%'>Pass</th>\n" + "<th  align='center' width='15%'>Failure</th>\n"
					+ "<th  align='center' width='19%'>Pass Rate</th>\n"
					+ "<th  align='center' width='18%'>Run Time</th>\n"
					+ "<th  align='center' width='18%'>Duration</th>\n" + "\n" + "</tr>\n" + "<tr>\n"
					+ "<td  align='center' >" + resultStatistics.getTotalAmount() + "</td>\n" + "<td  align='center' >"
					+ resultStatistics.getSuccessfulAmount() + "</td>\n" + "<td  align='center'>"
					+ resultStatistics.getFailedAmount() + "</td>\n" + "<td  align='center'>"
					+ formatDouble(resultStatistics.getPassRate() * 100, 2) + "%</td>\n" + "<td  align='center'>"
					+ formatPerformanceTimeCost(resultStatistics.getTotalRunTime()) + "</td>\n" + "<td  align='center'>"
					+ formatPerformanceTimeCost(resultStatistics.getDurationTime()) + "</td>\n" + "</tr>\n"
					+ "</table>\n" + "<br>");
		}

		return s.toString();
	}

	private String formatPerformanceTimeCost(Double cost) {

		String value = cost + "s";

		if (cost < 600) {
			value = formatDouble(cost, 3) + "s";
		} else {
			value = formatDouble(cost / 60.00, 3) + "mins";
		}

		return value;
	}

	private Double formatDouble(Double value, int scale) {

		if (null == value) {
			return 0.0;
		} else {
			return BigDecimal.valueOf((value)).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();

		}
	}

	protected String buildReportHeader(String headTitle) {

		StringBuffer s = new StringBuffer();

		if (CommonUtils.isNullOrEmpty(headTitle)) {
			headTitle = HEADLINE_CLOUD_TEST_REPORT_ON + getCurrentDate();
		}

		s.append(
				HEADLINE_DOCTYPE_HTML_PUBLIC_W3C_DTD_XHTML_1_0_TRANSITIONAL_EN_HTTP_WWW_W3_ORG_TR_XHTML1_DTD_XHTML1_TRANSITIONAL_DTD
						+ "<html xmlns='http://www.w3.org/1999/xhtml'>\n" + "<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n"
						+ "<style type= 'text/css'>\n"
						+ " table,th,td {     border: 1px solid #dddddd;  border-collapse: collapse;  padding: 2px; }  th {   background-color: #888888;  font: bold 12px arial, sans-serif;  color: #ffffff; }  table,td {   font: 12px arial, sans-serif; }"
						+ "</style>\n" + "\n" + "<title>Cloud Test Report</title>" + "</head>\n" + "<body>\n"
						+ "<span align='center' style='font-size:14pt;font-weight:bold;font-family:Tahoma;'>" + "<br/>"
						+ headTitle + ""
						+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
						+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
						+ "<font size='1'>" + HEADLINE_POWERED_BY_CLOUD_AUTOMATION_TEST_MANAGER_REPORTOR_REG_ENGINE);

		return s.toString();
	}

	protected void generateJunitXML(CloudTestOutput cloudTestOutput) throws Exception {

		String junitXmlDir = PluginConfigProxy.getParamValueByName(CLOUDTEST_REPORT_JUNIT_XML_DIR);

		if (null == cloudTestOutput || CommonUtils.isNullOrEmpty(junitXmlDir)) {
			return;
		}

		File f = new File(junitXmlDir);
		if (f == null || !f.exists()) {
			return;
		}

		// CloudTestUtils.deleteFiles(getReportDir(REPORT_FOLDER_NAME_XML));

		for (CloudTestOutput r : cloudTestOutput.getTestCaseResults()) {

			transformCloudCaseToJunitXml(r);
		}

	}

	private void transformCloudCaseToJunitXml(CloudTestOutput r) throws Exception {

		TestSuite ts = new TestSuite();
		if (!r.getStatus()) {
			ts.error.errorMessage = "TestCase Description:\n " + ObjectDigester.toXML(r.getTestCase()) + "\n";

			if (r.getFailedAssertResults() != null && r.getFailedAssertResults().size() > 0) {
				ts.failures = 1;
				ts.errors = 0;
				ts.error.errorMessage = ts.error.errorMessage + "\nFailure Assert Result:\n"
						+ getAssertMessages(r.getFailedAssertResults()) + "\n";
				ts.error.type = "AssertFailed";
			} else {
				ts.failures = 0;
				ts.errors = 1;
				ts.error.type = Exception.class.getName();
			}

			ts.error.message = r.getCaseId();
			if (!CommonUtils.isNullOrEmpty(r.getErrorMessage())) {
				ts.error.errorMessage = ts.error.errorMessage + r.getErrorMessage();
			}
		} else {
			ts.errors = 0;
			ts.failures = 0;
		}

		ts.name = r.getCasePath().replace("/", ".").replace("\\", ".") + "." + r.getCaseId();
		ts.tests = 1;
		ts.time = r.getRunTime();
		// ts.timestamp = Calendar.getInstance().getTime().toLocaleString();
		// ts.properties = getProperties();

		String xml = ObjectSerializer.marshalToXml(ts);

		String xmlPath = getReportDir(REPORT_FOLDER_NAME_XML);

		// String s = new String(xml.getBytes(), Charset.defaultCharset());
		// System.out.println(s);

		if (xmlPath.contains("junit")) {
			String fileName = xmlPath + File.separator + "TEST-" + r.getCaseId() + ".xml";
			XmlUtils.saveToXml(XmlUtils.paserStringToDocument(xml), fileName);
		}

		String junitXmlDir = PluginConfigProxy.getParamValueByName(CLOUDTEST_REPORT_JUNIT_XML_DIR);

		if (!CommonUtils.isNullOrEmpty(junitXmlDir)) {
			if (new File(junitXmlDir).exists()) {
				String fileName1 = junitXmlDir + File.separator + "TEST-" + r.getCaseId() + ".xml";
				XmlUtils.saveToXml(XmlUtils.paserStringToDocument(xml), fileName1);
			}
		}
	}

	private String getAssertMessages(List<AssertResult> failedAssertResults) {

		StringBuffer s = new StringBuffer();

		for (AssertResult a : failedAssertResults) {
			s.append(a.getErrorMessage() + "\n");
		}
		return s.toString();
	}

	private String getSimpleHtmlContent(String htmlContent) {

		int begin = htmlContent.lastIndexOf("<div");

		while (begin >= 0) {
			String deletedDiv = htmlContent.substring(begin);
			int end = deletedDiv.indexOf("</div>");

			if (end >= 0) {
				deletedDiv = deletedDiv.substring(0, end + 6);
				htmlContent = htmlContent.replace(deletedDiv, "");
			}

			begin = htmlContent.lastIndexOf("<div");
		}

		return htmlContent;
	}

	class TestReportFileComparable implements Comparator<File> {

		public int compare(File f1, File f2) {

			// return (int) (f2.lastModified() - f1.lastModified());
			return f2.getName().compareTo(f1.getName());
		}
	}

	class HotspotsResultComparable implements Comparator<CloudTestReportXML> {

		public int compare(CloudTestReportXML f1, CloudTestReportXML f2) {

			if (f2.timestamp == null || f1.timestamp == null) {
				return f2.caseId.compareTo(f1.caseId);
			} else {
				return f2.timestamp.compareTo(f1.timestamp);
			}
		}
	}

	static class ResultIndexManager {

		String indexFileName = getReportDir(REPORT_FOLDER_NAME_XML) + "/index.xml";
		static Map<String, Map<String, ResultStatistics>> indexMap = Collections
				.synchronizedMap(new HashMap<String, Map<String, ResultStatistics>>());

		Map<String, ResultStatistics> getIndex() throws Exception {

			if (null == indexMap.get(indexFileName)) {
				indexMap.put(indexFileName, loadIndex());
			}

			return indexMap.get(indexFileName);
		}

		Map<String, ResultStatistics> loadIndex() {

			Map<String, ResultStatistics> indexMap = null;

			File indexFile = new File(indexFileName);
			if (indexFile.exists()) {
				try {
					indexMap = (Map<String, ResultStatistics>) (ObjectDigester
							.fromXMLFile(indexFile.getAbsolutePath()));
				} catch (Exception e) {
					// if failed, ignore the broken index file
					indexMap = Collections.synchronizedMap(new HashMap<String, ResultStatistics>());
				}
			} else {
				indexMap = Collections.synchronizedMap(new HashMap<String, ResultStatistics>());
			}

			return indexMap;
		}

		synchronized void saveIndexToDisk() throws Exception {

			File indexFile = new File(indexFileName);

			XmlUtils.prettyPrint(ObjectDigester.toXML(getIndex()), indexFile.getAbsolutePath());
		}

	}

	class HistoryIndexWriterThread implements Runnable {

		CloudTestOutput cloudTestOutput = null;

		HistoryIndexWriterThread(CloudTestOutput cloudTestOutput) {

			this.cloudTestOutput = cloudTestOutput;
		}

		public void run() {

			try {
				synchronized (cloudTestOutput) {
					generateReportHistoryIndex(cloudTestOutput);
					generateResultTrendCharts();
					new ResultIndexManager().saveIndexToDisk();
				}
			} catch (Exception e) {
				log.error("History index writing failure, " + e.getMessage(), e);
			}
		}

	}

	class HotspotsReportThread implements Runnable {

		CloudTestOutput cloudTestOutput = null;

		HotspotsReportThread(CloudTestOutput cloudTestOutput) {

			this.cloudTestOutput = cloudTestOutput;
		}

		public void run() {

			try {
				synchronized (cloudTestOutput) {
					generateHotspotsReport(cloudTestOutput);
				}
			} catch (Exception e) {
				log.error("HotspotsIndexWriterThread index writing failure, " + e.getMessage(), e);
			}
		}

	}

	protected void generateResultTrendCharts() {

		String trendIndex = null;
		Exception ex = null;

		String chartEngine = CloudTestConstants.CLOUDTEST_REPORT_ENGINE_HIGHCHARTS;
		try {
			String engine = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_REPORT_ENGINE);

			if (!CommonUtils.isNullOrEmpty(engine)
					&& CloudTestConstants.CLOUDTEST_REPORT_ENGINE_JFREECHART.equalsIgnoreCase(engine)) {
				try {
					trendIndex = generateJFreeChart().toString();
					chartEngine = CloudTestConstants.CLOUDTEST_REPORT_ENGINE_JFREECHART;
				} catch (Exception e) {
					e.printStackTrace();
					ex = e;

					log.warn("ResultTrendJFreeChartGenerator executed failure, caused by " + e.getMessage()
							+ ".\nTrying to use highcharts to generate the report.", e);

					String[] htmls = new ResultTrendHighChartsGenerator().generateCharts();
					trendIndex = htmls[0];

					chartEngine = CloudTestConstants.CLOUDTEST_REPORT_ENGINE_HIGHCHARTS;
				}
			} else {
				try {
					String[] htmls = new ResultTrendHighChartsGenerator().generateCharts();
					trendIndex = htmls[0];
					chartEngine = CloudTestConstants.CLOUDTEST_REPORT_ENGINE_HIGHCHARTS;
				} catch (Exception e) {
					e.printStackTrace();
					ex = e;
					log.warn("ResultTrendHighChartsGenerator executed failure, caused by " + e.getMessage()
							+ ".\nTrying to use jfreechart to generate the report.", e);

					trendIndex = generateJFreeChart().toString();
					chartEngine = CloudTestConstants.CLOUDTEST_REPORT_ENGINE_JFREECHART;
				}
			}
			log.info("CloudTest chart report was generated sucessfully by " + chartEngine);

		} catch (Exception e1) {
			log.error("CloudTest chart report was generated failed, caused by "
					+ (ex != null ? "1. " + ex.getMessage() + "\n2.  " : "") + e1.getMessage() + "\n", e1);
		}

		try {
			String trendReportHtml = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + "trend" + ".html";
			saveFile(trendIndex.toString(), trendReportHtml);
			log.info("CloudTest chart report was achieved to '" + trendReportHtml + "' sucessfully.");
		} catch (Exception e) {
			log.error("CloudTest Result Trend Report generated failed.\n" + e.getMessage(), e);
		}
	}

	private StringBuffer generateJFreeChart() throws Exception {

		String[] charts = new ResultTrendJFreeChartGenerator().generateCharts();

		StringBuffer trendIndex = new StringBuffer();

		String title = "Cloud Test Result Trend Report on " + getCurrentDate();
		trendIndex.append(buildReportHeader(title));

		if (charts != null && charts.length > 0) {
			for (String chart : charts) {
				trendIndex.append("<img src=\"./" + chart + ".png\" width = \"100%\" />");
			}
		} else {
			trendIndex.append("None result trend chart was generated successfully."
					+ "<br><br>NOTES: CloudTest result trend chart report requests JFreeChart library, visit http://www.jfree.org/jfreechart/ to find more information about JFreeChart. ");
		}
		trendIndex.append(getCopyRightBottom());
		return trendIndex;
	}

	public static class TestResultStatisticsIndexComparable implements Comparator<String> {

		public int compare(String f1, String f2) {

			// if (f1.getTimestamp() == null || null == f2.getTimestamp()) {
			// return 0;
			// } else {
			// return f1.getTimestamp().compareTo(f2.getTimestamp());
			// }

			return f1.compareTo(f2);
		}
	}

	class ResultTrendHighChartsGenerator {

		public String[] generateCharts() throws Exception {

			String path = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + "lib";

			if (!isAllLibFilesExist(path)) {
				try {
					CommonUtils.copyFiles(ConfigurationProxy.getCloudTestRootPath() + "/Config/lib", path);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			String reprotHtml = fetchHTMLContent("/lib/trend.html");

			String dateTime = getCurrentDate() + "";
			StringBuffer passRateDataset = new StringBuffer();
			StringBuffer runTimeDataset = new StringBuffer();
			StringBuffer durationDataset = new StringBuffer();
			StringBuffer caseCountDataset = new StringBuffer();

			Map<String, CloudTestOutput.ResultStatistics> indexMap = new ResultIndexManager().getIndex();

			if (null == indexMap) {
				indexMap = new HashMap<String, CloudTestOutput.ResultStatistics>();
			}

			List<String> keySet = new ArrayList<String>();
			for (String cr : indexMap.keySet()) {
				keySet.add(cr);
			}

			Collections.sort(keySet, new TestResultStatisticsIndexComparable());

			int i = 0;
			for (String key : keySet) {

				CloudTestOutput.ResultStatistics r = indexMap.get(key);

				if (i != 0) {
					passRateDataset.append("," + r.getPassRate() * 100);
					durationDataset.append("," + r.getDurationTime());
					runTimeDataset.append("," + r.getTotalRunTime());
					caseCountDataset.append("," + r.getTotalAmount());
				} else {
					passRateDataset.append(r.getPassRate() * 100);
					durationDataset.append(+r.getDurationTime());
					runTimeDataset.append(r.getTotalRunTime());
					caseCountDataset.append(r.getTotalAmount());
				}
				i++;
			}

			reprotHtml = reprotHtml.replace("#{REPORT_TIMESTAMP}#", dateTime)
					.replace("#{CASE_COUNT_DATASET}#", caseCountDataset)
					.replace("#{DURATION_TIME_DATASET}#", durationDataset)
					.replace("#{RUN_TIME_DATASET}#", runTimeDataset).replace("#{PASS_RATE_DATASET}#", passRateDataset)
					.replace("#{COPYRIGHT_INFO}#", getCopyRightBottom());

			return new String[] { reprotHtml };
		}

		private boolean isAllLibFilesExist(String path) {

			File file = new File(path);

			if (!file.exists()) {
				return false;
			}

			boolean rst = true;
			String[] files = new String[] { "/highcharts.js", "/jquery.min.js", "/trend.html" };

			for (String s : files) {
				File f = new File(path + s);
				if (!f.exists()) {
					return false;
				}
			}

			return rst;
		}
	}

	class ResultTrendJFreeChartGenerator {

		public String[] generateCharts() throws Exception {

			try {
				Class.forName("org.jfree.chart.JFreeChart");
			} catch (ClassNotFoundException e1) {
				return new String[0];
			}

			String[] charts = new String[] { "Pass Rate Trend", "Performance Time Trend", "Case Count Trend" };
			try {
				Map<String, CloudTestOutput.ResultStatistics> indexMap = new ResultIndexManager().getIndex();

				if (null == indexMap) {
					indexMap = new HashMap<String, CloudTestOutput.ResultStatistics>();
				}

				DefaultCategoryDataset passRateDataSet = new DefaultCategoryDataset();
				DefaultCategoryDataset durationDataSet = new DefaultCategoryDataset();
				DefaultCategoryDataset caseCountDataSet = new DefaultCategoryDataset();

				List<String> keySet = new ArrayList<String>();
				for (String cr : indexMap.keySet()) {
					keySet.add(cr);
				}

				Collections.sort(keySet, new TestResultStatisticsIndexComparable());

				int i = 1;
				for (String key : keySet) {
					String x = String.valueOf(i++);
					CloudTestOutput.ResultStatistics r = indexMap.get(key);

					passRateDataSet.addValue(r.getPassRate() * 100, "", x);

					durationDataSet.addValue(r.getDurationTime(), "1", x);
					durationDataSet.addValue(r.getTotalRunTime(), "2", x);

					caseCountDataSet.addValue(r.getTotalAmount(), "", x);
				}

				drow(charts[0], "Pass Rate(%)", Color.RED, passRateDataSet);
				drow(charts[1], "Run Time(s)", Color.YELLOW, durationDataSet);
				drow(charts[2], "Case Count", Color.BLUE, caseCountDataSet);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("Cloud test result trend chart was generated failure.", e);
				throw e;
			}
			return charts;
		}

		void drow(String chartTitle, String yName, Color color, DefaultCategoryDataset dataset) throws Exception {

			JFreeChart chart = getChartInstance(chartTitle, yName, dataset);

			CategoryPlot plot = chart.getCategoryPlot();
			CategoryItemRenderer renderer = plot.getRenderer();

			renderer.setSeriesPaint(0, color);
			renderer.setSeriesPaint(1, Color.CYAN);

			renderer.setSeriesStroke(0, new BasicStroke(3F));// line size
			renderer.setSeriesStroke(1, new BasicStroke(3F));// line size

			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
			rangeAxis.setAutoRangeIncludesZero(false);
			rangeAxis.setAutoTickUnitSelection(true);
			rangeAxis.setUpperMargin(0.2);
			rangeAxis.setLowerMargin(0.2);
			rangeAxis.setLabelAngle(Math.PI);

			try {
				String pngFilePath = getReportDir(REPORT_FOLDER_NAME_HTML) + File.separator + chartTitle + ".png";
				ChartUtilities.saveChartAsPNG(new File(pngFilePath), chart, 1280, 480);
			} catch (Exception e) {
				throw e;
			}
		}

		private JFreeChart getChartInstance(String chartTitle, String yName, DefaultCategoryDataset dataset)
				throws Exception {

			JFreeChart chart = ChartFactory.createLineChart3D(chartTitle, // chart
					// title
					"", // X name
					yName, // Y name
					dataset, // data set
					PlotOrientation.VERTICAL, // direction
					false, // display legend
					true, // display tooltip
					false);// generate URL;

			String chartType = PluginConfigProxy
					.getParamValueByName(CloudTestConstants.CLOUDTEST_REPORT_ENGINE_JFREECHART_CHART);

			if (!CommonUtils.isNullOrEmpty(chartType)
					&& CloudTestConstants.CLOUDTEST_REPORT_ENGINE_JFREECHAR_CHART_LineChart
							.equalsIgnoreCase(chartType)) {
				chart = ChartFactory.createLineChart(chartTitle, // chart
						// title
						"", // X name
						yName, // Y name
						dataset, // data set
						PlotOrientation.VERTICAL, // direction
						false, // display legend
						true, // display tooltip
						false);// generate URL
			}

			return chart;
		}
	}

	public void setReportFolderName(String reportFolder) throws Exception {

		if (!CommonUtils.isNullOrEmpty(reportFolder)) {
			CURRENT_REPORT_PATH_NAME = REPORT_PATH_NAME + File.separator + reportFolder;
		} else {
			CURRENT_REPORT_PATH_NAME = REPORT_PATH_NAME + File.separator + "default";
		}
	}
}
