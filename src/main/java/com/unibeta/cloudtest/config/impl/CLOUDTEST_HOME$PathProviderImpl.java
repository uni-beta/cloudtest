package com.unibeta.cloudtest.config.impl;

import com.unibeta.cloudtest.config.CLOUDTEST_HOME$PathProvider;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * The default CLOUDTEST_HOME is located to 'classes' folder managed by java
 * runtime ClassLoader.
 * 
 * @author jordan.xue
 */
public class CLOUDTEST_HOME$PathProviderImpl implements
        CLOUDTEST_HOME$PathProvider {

    private static String cloudtestHomePath;

    public String getCLOUDTEST_HOME() throws Exception {

        if (null != cloudtestHomePath) {
            return cloudtestHomePath;
        }

        String pathFromEnvParam = System.getenv(CLOUDTEST_HOME);
        String pathFromProp = System.getProperty(CLOUDTEST_HOME_PROPERTY);

        if (!CommonUtils.isNullOrEmpty(pathFromEnvParam)) {
            cloudtestHomePath = pathFromEnvParam;
        } else if (!CommonUtils.isNullOrEmpty(pathFromProp)) {
            cloudtestHomePath = pathFromProp;
        } else {
            throw new Exception(
                    "It is not a bug or error, only a kindly warning. CloudTest might not be able to run properly.\n"
                            + "CLOUDTEST_HOME environment variables was not defined in current system context, please double check and configure it. \n"
                            + "It can also be initialized via below approaches in runtime: \n"
                            + "1. com.unibeta.cloudtest.config.ConfigurationProxy.setCLOUDTEST_HOME(String path);\n"
                            + "2. -Dcloudtest.home = \"$CLOUDTEST_HOME$\"\n"
                            + "3. Configure system environment variables via 'CLOUDTEST_HOME = \"$CLOUDTEST_HOME$\"'");

        }

        return cloudtestHomePath;
    }
}
