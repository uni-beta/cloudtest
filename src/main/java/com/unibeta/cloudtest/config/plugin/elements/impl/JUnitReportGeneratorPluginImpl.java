package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.elements.ReportGeneratorPlugin;
import com.unibeta.cloudtest.util.CloudTestUtils;

/*
 * Junit report implementation in html.
 */
public class JUnitReportGeneratorPluginImpl extends ReportGeneratorPluginImpl
        implements ReportGeneratorPlugin {

    private static final String JUNIT_REPORT_PATH_NAME = REPORT_PATH_NAME + File.separator
            + "junit";
    private static final String PROPERTY_CLOUDTEST_REPORT_HTML = "cloudtest.report.html";
    private static final String PROPERTY_CLOUDTEST_REPORT_XML = "cloudtest.report.xml";
    private static final String ANT_JUNIT_REPORT_XML = "junit-report.xml";
 
    private static final Logger log = LoggerFactory
            .getLogger(JUnitReportGeneratorPluginImpl.class);
    
    static{
        CURRENT_REPORT_PATH_NAME = JUNIT_REPORT_PATH_NAME;
    }
    
    public ReportResult generateReport(CloudTestOutput cloudTestOutput)
            throws Exception {

        ReportResult reportResult = new ReportResult();

        try {
            Class.forName("org.apache.tools.ant.launch.AntMain");
            Class.forName("org.apache.tools.ant.Project");
            Class.forName("org.apache.tools.ant.ProjectHelper");

            reportResult.setContent(generateJunitReportInHtml(cloudTestOutput));
            return reportResult;
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("JunitReportGeneratorPluginImpl was enabled failure, using defult report generator "
                    + ReportGeneratorPluginImpl.class.getName()
                    + ". caused by " + e.getMessage());
            return super.generateReport(cloudTestOutput);
        }
    }

    private String generateJunitReportInHtml(CloudTestOutput cloudTestOutput)
            throws Exception {

        StringBuffer s = new StringBuffer();

        generateJunitXML(cloudTestOutput);
        s.append(buildReportHeader(null));
        s.append(buildReportResultStatistics(cloudTestOutput,null));

        s.append(transformsXML2HTML());

        s.append(buildReportInXML(cloudTestOutput));

        return s.toString();
    }

    private String transformsXML2HTML() throws Exception {

        Project project = new Project();

        try {
            CloudTestUtils.deleteFiles(getReportDir(REPORT_FOLDER_NAME_HTML));

            project.fireBuildStarted();
            project.init();

            ProjectHelper helper = ProjectHelper.getProjectHelper();
            File junitreport = new File(
                    ConfigurationProxy.getCloudTestRootPath() + File.separator
                            + "Config" + File.separator + ANT_JUNIT_REPORT_XML);
            helper.parse(project, junitreport);

            project.setProperty(PROPERTY_CLOUDTEST_REPORT_XML,
                    getReportDir(REPORT_FOLDER_NAME_XML));
            project.setProperty(PROPERTY_CLOUDTEST_REPORT_HTML,
                    getReportDir(REPORT_FOLDER_NAME_HTML));

            project.executeTarget(project.getDefaultTarget());
            project.fireBuildFinished(null);

        } catch (BuildException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            project.fireBuildFinished(e);
            throw e;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            throw e;
        }

        StringBuffer s = new StringBuffer();

        String css = "<style type= 'text/css'>\n"
                + fetchHTMLContent("stylesheet.css") + "</style>\n" + "</head>";
        s.append(fetchHTMLContent("package-summary.html"));

        return s.toString().replace("</head>", css)
                .replace("</head>", "</head><!--")
                .replace("<h2>Classes</h2>", "-->");
    }

    // public static void main(String[] args) throws Exception {
    //
    // ConfigurationProxy.setCLOUDTEST_HOME("D:\\eclipse\\cloudtest\\test");
    // JunitReportGeneratorPluginImpl g = new JunitReportGeneratorPluginImpl();
    //
    // CloudCaseInput input = new CloudCaseInput();
    // input.setFileName("TestCase/test");
    //
    // CloudTestOutput rst = new CloudTestService().doTest(input);
    // g.generateReport(rst);
    // }
}
