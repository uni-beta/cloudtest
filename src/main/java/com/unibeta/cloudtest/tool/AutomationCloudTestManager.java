package com.unibeta.cloudtest.tool;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.restful.LocalRESTfulTestController;
import com.unibeta.cloudtest.tool.MailManager.MailReceiverObject;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * AutomaticCloudTestManager manages automatic test tasks. Every 4hr~ 8hr will
 * trigger one time. <br>
 * 
 * @author jordan.xue
 */
public class AutomationCloudTestManager {

    private static final int RUNNING_DELAY_MINUTES = 10;
    private static final String SYSTEM_USER_NAME = System
            .getProperty("user.name");
    private static Timer timer = null;
    private static Boolean isDeployed = false;
    private static Logger log = LoggerFactory
            .getLogger(AutomationCloudTestManager.class);

    /**
     * Deploy the automatic could test service only when
     * 'CLOUD_TEST_SWITCH_FLAG' is 'true' in t_Gen_Const_Value table.
     * 
     * @throws Exception
     */
    public static void deploy() {

        Boolean switchFlag = Boolean.FALSE;

        try {
            switchFlag = CloudTestPluginFactory.getParamConfigServicePlugin()
                    .getAutomationTestSwitchFlag();
        } catch (Exception e) {
            switchFlag = Boolean.FALSE;
        }

        synchronized (isDeployed) {
            if (switchFlag && !isDeployed) {
                timer = getTimerInstance();
                Random random = new Random();

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, RUNNING_DELAY_MINUTES);// 5
                // minute
                // later
                Date firstTime = calendar.getTime();

                int n;

                try {
                    n = CloudTestPluginFactory.getParamConfigServicePlugin()
                            .getAutomationTestIntervalHours();
                } catch (Exception e) {
                    n = random.nextInt(6);
                }
                long period = 1000 * 60 * 60 * (2 + n); // the random period is
                // 2hr ~8hr

                timer.schedule(new CloudTestTimerTask(), firstTime, period);

                deployMailRobotService();

                isDeployed = true;
            }
        }
    }

    public void handleMailRequest() {

        new MailServiceCenterTimerTask().run();
    }

    public static void deployMailRobotService() {

        try {
            if (SYSTEM_USER_NAME.equalsIgnoreCase(CloudTestPluginFactory
                    .getParamConfigServicePlugin()
                    .getMailRobotServiceDeployedServerName())) {

                Calendar calendar = Calendar.getInstance();
                Date firstTime = calendar.getTime();

                new Timer("MailServiceCenter", true).schedule(
                        new MailServiceCenterTimerTask(), firstTime,
                        (1000 * 30));
            }
        } catch (Exception e) {
            log.error(
                    e.getMessage() + CloudTestUtils.printExceptionStackTrace(e),
                    e.getCause());
        }

    }

    /**
     * Deploy the automatic could test service only when
     * 'CLOUD_TEST_SWITCH_FLAG' is 'true' in t_Gen_Const_Value table. Automatic
     * Test Reportor will be triggered in every specified minutes.
     * 
     * @param everyMinutes
     *            trigger time in minutes.
     */
    public static void deploy(Integer everyMinutes) {

        if (null == everyMinutes || everyMinutes == 0) {
            return;
        }

        Boolean switchFlag = Boolean.FALSE;

        try {
            switchFlag = CloudTestPluginFactory.getParamConfigServicePlugin()
                    .getAutomationTestSwitchFlag();
        } catch (Exception e) {
            switchFlag = Boolean.FALSE;
        }

        synchronized (switchFlag) {
            if (switchFlag) {

                timer = getTimerInstance();
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, 0);
                Date firstTime = calendar.getTime();

                long period = 1000 * 60 * everyMinutes;

                timer.schedule(new CloudTestTimerTask(), firstTime, period);

                isDeployed = true;
            }
        }
    }

    private static void stopTimer() {

        if (null != timer) {
            timer.cancel();
            timer.purge();
            timer = null;
        }

        isDeployed = false;
    }

    static Timer getTimerInstance() {

        stopTimer();
        return new Timer("AutomaticCloudTester", true);
    }

    static class CloudTestTimerTask extends TimerTask {

        private static final String CONFIG_CLOUD_TEST_REPORTOR_XML = "Config/CloudTestReportor.xml";

        @Override
        public void run() {

            CloudCaseInput caseInput = new CloudCaseInput();
            caseInput.setFileName(CONFIG_CLOUD_TEST_REPORTOR_XML);
            new CloudTestService().doTest(caseInput);

        }
    }

    static class MailServiceCenterTimerTask extends TimerTask {

        private static Boolean isRunning = false;

        @Override
        public void run() {

            if (isRunning) {
                return;
            }

            synchronized (isRunning) {
                isRunning = true;
            }

            MailReceiverObject mailReceiverObject = null;

            try {

                mailReceiverObject = MailManager.receiveMails();

                if (null == mailReceiverObject
                        || null == mailReceiverObject.getMessages()
                        || null == mailReceiverObject.getStore()
                        || null == mailReceiverObject.getFolder()) {
                    return;
                }

                if (!mailReceiverObject.getFolder().isOpen()) {
                    mailReceiverObject.getFolder().open(Folder.READ_WRITE);
                }

            } catch (Exception e) {
            	LoggerFactory.getLogger(AutomationCloudTestManager.class).error(
                        e.getMessage()
                                + CloudTestUtils.printExceptionStackTrace(e),
                        e.getCause());
            }

            String originalContent = null;
            String subject = null;
            String from = null;

            try {
                for (MimeMessage m : mailReceiverObject.getMessages()) {

                    if (null == m) {
                        continue;
                    }

                    if (MailManager.isContainAttch(m)) {
                        saveAndUnzipMailAttachments(m);
                    }

                    if (!m.getFolder().isOpen()) {
                        m.getFolder().open(Folder.READ_WRITE);
                    }

                    m.setFlag(Flags.Flag.DELETED, true);

                    if (null == m.getContent()) {
                        continue;
                    }

                    from = convertAddress2String(m.getFrom());

                    subject = m.getSubject() + " --Cloud Response From "
                            + SYSTEM_USER_NAME;

                    if (!m.getSubject().startsWith("RE:")) {
                        subject = "RE:" + subject;
                    }

                    String content = MailManager.getMailContent(m,
                            new StringBuffer());

                    int indexOfHtml = content.indexOf("<html");
                    if (indexOfHtml > 0) {
                        content = content.substring(0, indexOfHtml);
                    }

                    originalContent = "\n\n-----Original Message-----\n"
                            + "From:" + from + "\n" + "Sent:" + m.getSentDate()
                            + "\n" + "To:"
                            + convertAddress2String(m.getReplyTo()) + "\n"
                            + "Subject:" + subject + "\n\n" + content;

                    int indexOfStart = content.indexOf("<cloudTestCase");
                    int indexOfEnd = content.indexOf("</cloudTestCase>");

                    if (!CommonUtils.isNullOrEmpty(content)
                            && indexOfStart >= 0 && indexOfEnd > 0) {

                        String caseStr = content.substring(indexOfStart,
                                indexOfEnd + 16);

                        CloudTestOutput output = (CloudTestOutput) ObjectSerializer
                                .unmarshalToObject(
                                         LocalRESTfulTestController
                                                .invoke(caseStr),
                                        CloudTestOutput.class);

                        new CloudTestReportor().sendReport(from, subject,
                                output);

                    } else {

                        String name = MimeUtility.decodeText(from);
                        name = name.substring(0, name.indexOf("<"));

                        String replyContent = "Dear " + name + ","
                                + FAILED_MAIL_CONTENT;
                        MailManager.sendMail(from, subject, replyContent
                                + originalContent, false,
                                MailManager.MAIL_PRIORITY_NORMAL);
                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                try {
                	LoggerFactory.getLogger(AutomationCloudTestManager.class)
                            .error(e.getMessage()
                                    + CloudTestUtils
                                            .printExceptionStackTrace(e),
                                    e.getCause());

                    MailManager.sendMail(from, subject + " From "
                            + SYSTEM_USER_NAME, e.getMessage() + "\n"
                            + CloudTestUtils.printExceptionStackTrace(e)
                            + originalContent, false,
                            MailManager.MAIL_PRIORITY_HIGHEST);
                } catch (Exception e1) {
                	LoggerFactory.getLogger(AutomationCloudTestManager.class)
                            .error(e1.getMessage()
                                    + CloudTestUtils
                                            .printExceptionStackTrace(e1),
                                    e1.getCause());
                }
            } finally {

                try {
                    Folder folder = mailReceiverObject.getFolder();
                    if (folder != null && folder.isOpen()) {
                        folder.close(true);

                    }
                    Store store = mailReceiverObject.getStore();
                    if (store != null && store.isConnected()) {
                        store.close();
                    }
                } catch (MessagingException e) {
                	LoggerFactory.getLogger(AutomationCloudTestManager.class)
                            .error(e.getMessage()
                                    + CloudTestUtils
                                            .printExceptionStackTrace(e),
                                    e.getCause());
                }
            }
            synchronized (isRunning) {
                isRunning = false;
            }
        }

        private void saveAndUnzipMailAttachments(MimeMessage m)
                throws Exception {

            try {
                String saveAttchPath = ConfigurationProxy
                        .getCloudTestRootPath();
                List<String> zipFilePaths = MailManager.saveAttachment(m,
                        saveAttchPath);

                for (String zipFilePath : zipFilePaths) {
                    if (null != zipFilePath
                            && zipFilePath.toLowerCase().endsWith(".zip")) {
                        CloudTestUtils.unzipFiles(new File(zipFilePath));
                    }
                }

            } catch (Exception e) {
                String err = "unzip mail attachment failed caused by "
                        + e.getMessage();
                log.error(err, e);
                // throw e;
            }
        }
    }

    private static String convertAddress2String(Address[] addresss) {

        StringBuffer s = new StringBuffer();

        for (Address a : addresss) {
            s.append(a.toString() + ";");
        }

        return s.toString();

    }

    /**
     * Whether current application instance has deployed cloud automatic test
     * service.
     * 
     * @return
     */
    public static Boolean isDeployed() {

        return isDeployed;
    }

    private static String TEST_CASE_TEMPLATE = null;

    private final static String FAILED_MAIL_CONTENT = "\n\nNone valid test case  was found in your request.  "
            + "\n\nThis mail was responded by Cloud Test Mail Service Center, "
            + "please do not reply it if you have no more request. "
            + "\nFor any support, please feel free to contact my author jordan.xue@ebaotech.com"
            + (!CommonUtils.isNullOrEmpty(TEST_CASE_TEMPLATE) ? "\n\n Below is a cloud test case payload example for your reference:"
                    + "\n" + getTestCaseTemplate()
                    : "")
            + "\n\nThanks & Best Regards "
            + "\nfrom Cloud Test Mail Service Center" + "";

    private static String getTestCaseTemplate() {

        if (!CommonUtils.isNullOrEmpty(TEST_CASE_TEMPLATE)) {
            return TEST_CASE_TEMPLATE;
        } else {
            try {
                TEST_CASE_TEMPLATE = XmlUtils.paserDocumentToString(XmlUtils
                        .getDocumentByFileName(ConfigurationProxy
                                .getCloudTestRootPath()
                                + "TestCase"
                                + File.separator + "testCaseTemplate.tc.xml"));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return TEST_CASE_TEMPLATE;
        }
    }

}
