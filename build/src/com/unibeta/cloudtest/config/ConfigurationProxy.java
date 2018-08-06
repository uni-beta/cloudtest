package com.unibeta.cloudtest.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestInput.CloudTestParameter;
import com.unibeta.cloudtest.config.CloudTestCase.Case;
import com.unibeta.cloudtest.config.CloudTestCase.Case.Parameter;
import com.unibeta.cloudtest.config.impl.CLOUDTEST_HOME$PathProviderImpl;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * <code>CloudTestConfigProxy</code> is a test case configuration service proxy,
 * provided all configuration accessing interface.
 * 
 * @author jordan.xue
 */

public class ConfigurationProxy {

    private static String CLOUDTEST_HOME;
    private static String rootFolderName = "cloudtest";

    private static Logger logger = Logger.getLogger(ConfigurationProxy.class);
    @SuppressWarnings("unused")
    private static boolean initStatus = initCommonConfig();

    /**
     * Gets the root path for cloud test configuration files. By default, it is
     * 'cloudtest', which can be changed by setRootFolderName in runtime.
     * 
     * @return root path
     */
    public static String getCloudTestRootPath() {

        return getCLOUDTEST_HOME() + File.separator + rootFolderName
                + File.separator;
    }

    /**
     * Gets the PluginConfig.xml file path.
     * 
     * @return PluginConfig.xml file path
     */
    public static String getConfigurationFilePath() {

        return getCloudTestRootPath() + File.separator + "Config"
                + File.separator + "PluginConfig.xml";
    }

    private static boolean initCommonConfig() {

        try {
            // File configFile = ConfigFileHelper
            // .getLocalConfigFile(CONFIGURATION_FILE_PATH);

            // currently it is empty
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * load all test case in the XML File which the FilePath is caseFileName
     * 
     * @param caseFileName
     * @throws Exception
     */

    public static CloudTestCase loadCloudTestCase(String caseFileName,
            String[] caseIds) throws Exception {

        initCommonConfig();

        CloudTestCase cloudTestCase = (CloudTestCase) ObjectSerializer
                .unmarshalToObject(
                        XmlUtils.paserDocumentToString(
                                XmlUtils.getDocumentByFileName(caseFileName))
                                .replace("<cloudTestCase", "<cloudtest")
                                .replace("cloudTestCase>", "cloudtest>"),
                        CloudTestCase.class);

        List<Case> allCaseList = cloudTestCase.testCase;

        if (null != caseIds && caseIds.length > 0 && caseIds[0] != null
                && !"".equals(caseIds[0].trim())
                && !"?".equals(caseIds[0].trim())) {
            List<Case> returnList = new ArrayList<Case>();

            for (String id : caseIds) {
                for (Case c : allCaseList) {
                    if (id != null && c.id.equals(id)) {
                        returnList.add(c);
                    }
                }
            }

            cloudTestCase.testCase = returnList;
        } else {
            cloudTestCase.testCase = allCaseList;
        }

        cloudTestCase.ns = new File(caseFileName).getPath();
        initCaseNameSpaceLibs(cloudTestCase);

        return cloudTestCase;
    }

    private static void initCaseNameSpaceLibs(CloudTestCase cloudTestCase) {

        if (null == cloudTestCase) {
            return;
        }

        StringBuffer nsLibs = new StringBuffer();
        String[] imports = cloudTestCase.imports.split(",");
        nsLibs.append(cloudTestCase.ns);

        for (String impt : imports) {
            String[] nsRefs = CloudTestUtils.resolveTestCaseImportsPath(
                    cloudTestCase.ns, impt);
            for (String ns : nsRefs) {
                nsLibs.append("," + new File(ns).getPath());
            }
        }

        cloudTestCase.nsLibs = nsLibs.toString();

        for (Case c : cloudTestCase.testCase) {
            c.nsLibs = cloudTestCase.nsLibs;
        }
    }

    /**
     * Converts Case to CloudTestInput.
     * 
     * @param testCase
     * @return
     */
    public static CloudTestInput converCaseToCloudTestInput(Case testCase) {

        CloudTestInput cloudTestInput = new CloudTestInput();

        if (testCase != null) {

            if (!"".equals(testCase.className))
                cloudTestInput.setClassName(testCase.className.trim());
            if (!"".equals(testCase.methodName))
                cloudTestInput.setMethodName(testCase.methodName.trim());

            Iterator iterator = testCase.parameter.iterator();
            List<CloudTestParameter> cloudParameterList = new ArrayList<CloudTestParameter>();

            while (iterator.hasNext()) {
                Parameter parameter = (Parameter) iterator.next();
                CloudTestParameter cloudTestParameter = new CloudTestParameter();

                if (!isEmpty(parameter.dataType_)) {

                    cloudTestParameter.setDataType(parameter.dataType_);
                } else {
                    cloudTestParameter.setDataType(parameter.dataType);
                }

                if (!isEmpty(parameter.parameterType_)) {
                    cloudTestParameter
                            .setParameterType(parameter.parameterType_);
                } else {
                    cloudTestParameter
                            .setParameterType(parameter.parameterType);
                }

                if (!isEmpty(parameter.value_)) {
                    cloudTestParameter.setValue(parameter.value_);
                } else {
                    cloudTestParameter.setValue(parameter.value);
                }

                if (!isEmpty(parameter.name_)) {
                    cloudTestParameter.setName(parameter.name_);
                } else {
                    cloudTestParameter.setName(parameter.name);
                }

                cloudParameterList.add(cloudTestParameter);
            }

            cloudTestInput.setParameter(cloudParameterList);
        }

        return cloudTestInput;

    }

    private static boolean isEmpty(String errorMsg) {

        return (null == errorMsg) || errorMsg.trim().length() == 0;
    }

    /**
     * Retrieve the local CLOUDTEST_HOME path. It is the only right path service
     * for entire cloud test framework.
     * 
     * @return CLOUDTEST_HOME
     */
    public static String getCLOUDTEST_HOME() {

        if (CommonUtils.isNullOrEmpty(CLOUDTEST_HOME)) {

            try {
                CLOUDTEST_HOME = new CLOUDTEST_HOME$PathProviderImpl()
                        .getCLOUDTEST_HOME();
            } catch (Exception e) {
                logger.error(e.getMessage(), e.getCause());
                return null;
            }

        }
        return CLOUDTEST_HOME;
    }

    /**
     * Set local CLOUDTEST_HOME path.
     * 
     * @param CLOUDTEST_HOME
     */
    public static void setCLOUDTEST_HOME(String CLOUDTEST_HOME) {

        ConfigurationProxy.CLOUDTEST_HOME = CLOUDTEST_HOME;
    }

    /**
     * Get the folder name, such as "cloudtest", where core config folder are
     * located.
     * 
     * @return file path. <br>
     *         eg, cloudtest
     */
    public static String getRootFolderName() {

        return rootFolderName;
    }

    /**
     * Get current operation system's user name
     * 
     * @return
     */
    public static String getOsUserName() {

        return System.getProperty("user.name");
    }

    public static void setRootFolderName(String rootFolderName) {

        ConfigurationProxy.rootFolderName = rootFolderName;
    }
}
