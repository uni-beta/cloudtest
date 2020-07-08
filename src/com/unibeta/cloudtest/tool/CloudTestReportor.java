package com.unibeta.cloudtest.tool;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudLoadInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.elements.ReportGeneratorPlugin;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Cloud report service, provided mail report engine and mail service.
 * 
 * @author jordan.xue
 */
public class CloudTestReportor {

	private static final String DEFAULT_TEST_CASE_PATH = "TestCase/";
	private static Logger log = LoggerFactory.getLogger(CloudTestReportor.class);

	// private XStream ObjectDigester = new XStream(new DomDriver());

	/**
	 * It will execute specified test case under given folder or file and send
	 * the result via email.
	 * 
	 * @param moduleName
	 *            module name
	 * @param caseFileName
	 *            the specified test case folder to be executed.
	 * @param toMailsAddress
	 *            the mail address to be sent
	 * @param reportFolderName
	 *            The report location folder name, it will be "default" if it is
	 *            null or blank.
	 * @return the test result
	 * @throws Exception
	 */
	public static CloudTestOutput report(String moduleName,
			String caseFileName, String toMailsAddress, String reportFolderName) {

		CloudTestOutput cloudTestOutput = null;
		CloudCaseInput input = new CloudCaseInput();

		if (null == caseFileName || caseFileName.length() == 0) {
			caseFileName = DEFAULT_TEST_CASE_PATH;
		}

		input.setFileName(caseFileName);
		input.setCaseId(null);

		cloudTestOutput = new CloudTestService().doTest(input);
		cloudTestOutput = buildReport(moduleName, cloudTestOutput,
				toMailsAddress, reportFolderName);

		return cloudTestOutput;

	}

	/**
	 * It will execute specified test case under given folder or file and send
	 * the result via email.
	 * 
	 * @param moduleName
	 *            module name
	 * @param caseFileName
	 *            the specified test case folder to be executed.
	 * @param toMailsAddress
	 *            the mail address to be sent
	 * @return the test result
	 * @throws Exception
	 */
	public static CloudTestOutput report(String moduleName,
			String caseFileName, String toMailsAddress) {

		return report(moduleName, caseFileName, toMailsAddress, null);
	}

	private static CloudTestOutput buildReport(String moduleName,
			CloudTestOutput cloudTestOutput, String toMailsAddress,
			String reportFolderName) {

		try {
			String subject = moduleName + "Cloud Test Report@" + new Date();
			String userName = System.getProperty("user.name");

			if (!CommonUtils.isNullOrEmpty(userName)) {
				subject = subject + " From " + userName;
			}

			sendReport(toMailsAddress, subject, cloudTestOutput,
					reportFolderName);
		} catch (Exception e) {

			cloudTestOutput.setStatus(false);
			cloudTestOutput.setErrorMessage("Cloud Report Error: "
					+ e.getMessage() + " \n "
					+ cloudTestOutput.getErrorMessage() + "\n"
					+ CloudTestUtils.printExceptionStackTrace(e));

			log.error(cloudTestOutput.getErrorMessage(), e);
		}
		return cloudTestOutput;
	}

	/**
	 * It will execute specified load test service under given folder or file
	 * and send the result via email.
	 * 
	 * @param moduleName
	 *            module name
	 * @param cloudLoadInput
	 *            the specified load test case to be executed.
	 * @param toMailsAddress
	 *            the mail address to be sent
	 * @return the test result
	 * @throws Exception
	 */
	public static CloudTestOutput report(String moduleName,
			CloudLoadInput cloudLoadInput, String toMailsAddress) {

		return report(moduleName, cloudLoadInput, toMailsAddress, "load");
	}

	/**
	 * It will execute specified load test service under given folder or file
	 * and send the result via email.
	 * 
	 * @param moduleName
	 *            module name
	 * @param cloudLoadInput
	 *            the specified load test case to be executed.
	 * @param toMailsAddress
	 *            the mail address to be sent
	 * @param reportFolderName
	 *            if it is null or blank, it will be located in 'load' by
	 *            default.
	 * @return the test result
	 * @throws Exception
	 */
	public static CloudTestOutput report(String moduleName,
			CloudLoadInput cloudLoadInput, String toMailsAddress,
			String reportFolderName) {

		CloudTestOutput cloudTestOutput = null;
		if (CommonUtils.isNullOrEmpty(moduleName)) {
			moduleName = "";
		}

		cloudTestOutput = new CloudTestService().doLoadTest(cloudLoadInput);
		cloudTestOutput = buildReport(moduleName + "[LoadTest]",
				cloudTestOutput, toMailsAddress, reportFolderName);

		return cloudTestOutput;
	}

	/**
	 * Generates the report file under 'reportFolderName' folder, and Send
	 * <code>CloudTestOutput</code> to mail report.
	 * 
	 * @param toMailsAddress
	 *            mail address, split with ';' for multiple email addresses.
	 * @param subject
	 *            mail subject
	 * @param cloudTestOutput
	 * @param reportFolderName
	 *            if it is null or blank, it will be located in 'default' by
	 *            default.
	 * @throws Exception
	 */
	public static void sendReport(String toMailsAddress, String subject,
			CloudTestOutput cloudTestOutput, String reportFolderName)
			throws Exception {

		int priority = MailManager.MAIL_PRIORITY_NORMAL;
		ReportGeneratorPlugin.ReportResult generateReport = null;

		if (null != cloudTestOutput) {
			priority = generatePriority(cloudTestOutput);

			ReportGeneratorPlugin generatorPlugin = CloudTestPluginFactory
					.getReportGeneratorPlugin();

			generatorPlugin.setReportFolderName(reportFolderName);
			generateReport = generatorPlugin.generateReport(cloudTestOutput);
			if (null == generateReport) {
				generateReport = new ReportGeneratorPlugin.ReportResult();
			}
		}

		try {
			if (!CommonUtils.isNullOrEmpty(toMailsAddress)
					&& generateReport != null) {
				log.info("start sending mail...");
				
				long start = System.currentTimeMillis();
				MailManager.sendMail(toMailsAddress, subject,
						generateReport.getContent(),
						generateReport.getDataResources(), true, priority);
				long end = System.currentTimeMillis();
				
				log.info("mail sending success in " + (end-start)/1000.00 + "s");
			}
		} catch (Exception e) {
			log.error("mail sending failed, caused by " + e.getMessage(), e);
		}
	}

	/**
	 * It will execute specified test case under given folder or file and send
	 * the result via email.
	 * 
	 * @param caseFileName
	 *            the specified test case folder to be executed.
	 * @param toMailsAddress
	 *            the mail address to be sent
	 * @return the test result
	 * @throws Exception
	 */
	public static CloudTestOutput report(String caseFileName,
			String toMailsAddress) {

		return report("", caseFileName, toMailsAddress);
	}

	private static int generatePriority(CloudTestOutput cloudTestOutput) {

		if (null == cloudTestOutput
				|| null == cloudTestOutput.getTestCaseResults()) {
			return MailManager.MAIL_PRIORITY_HIGHEST;
		}

		for (CloudTestOutput o : cloudTestOutput.getTestCaseResults()) {
			if (o.getStatus() != null && !o.getStatus()) {
				return MailManager.MAIL_PRIORITY_HIGH;
			}
		}

		return MailManager.MAIL_PRIORITY_NORMAL;
	}

	/**
	 * Generate the report file under default folder and send the email to
	 * specified mail addresses.
	 * 
	 * @param toMailsAddress
	 * @param subject
	 * @param cloudTestOutput
	 * @throws Exception
	 */
	public static void sendReport(String toMailsAddress, String subject,
			CloudTestOutput cloudTestOutput) throws Exception {

		sendReport(toMailsAddress, subject, cloudTestOutput, null);
	}
}
