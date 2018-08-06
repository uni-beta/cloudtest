package com.unibeta.cloudtest.config.plugin.elements;

import java.io.File;
import java.util.List;

import javax.activation.DataSource;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;

/**
 * The plugin interface for cloud test report generator.
 * 
 * @author jordan.xue
 */
public interface ReportGeneratorPlugin extends CloudTestPlugin {
    
    public static final String REPORT_PATH_NAME = ConfigurationProxy.getCLOUDTEST_HOME()
            + File.separator + "reports";
    public static final String REPORT_FOLDER_NAME_XML = "xml";
    public static final String REPORT_FOLDER_NAME_HTML = "html";
    public static final String CLOUDTEST_REPORT_JUNIT_XML_DIR = CloudTestConstants.CLOUDTEST_REPORT_JUNIT_XML_DIR;
    
    /**
     * Generates the report document by input test result.
     * 
     * @param cloudTestOutput
     * @return report content or object
     * @throws Exception
     */
    public ReportResult generateReport(CloudTestOutput cloudTestOutput)
            throws Exception;
    /**
     * Init the report folder name
     * @param reportFolder
     * @throws Exception
     */
    public void setReportFolderName(String reportFolderName)
            throws Exception;

    /**
     * Report Result
     * 
     * @author jordan.xue
     */
    public static class ReportResult {

        private String content;
        private List<DataSource> dataResources;

        /**
         * get report content in html or text.
         * 
         * @return
         */
        public String getContent() {

            return content;
        }

        /**
         * Set reprot context in html or text
         * 
         * @param content
         */
        public void setContent(String content) {

            this.content = content;
        }

        /**
         * Get report data resources, can be used as mail attachments
         * 
         * @return
         */
        public List<DataSource> getDataResources() {

            return dataResources;
        }

        /**
         * Set report data resources, can be used as mail attachments
         * 
         * @return
         */
        public void setDataResources(List<DataSource> dataResources) {

            this.dataResources = dataResources;
        }
    }
}
