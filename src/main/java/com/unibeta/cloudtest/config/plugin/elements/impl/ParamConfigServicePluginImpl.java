package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.util.Random;

import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.ParamConfigServicePlugin;
import com.unibeta.vrules.utils.CommonUtils;
import static com.unibeta.cloudtest.constant.CloudTestConstants.*;

/**
 * Default implementation.
 * 
 * @author jordan.xue
 */
public class ParamConfigServicePluginImpl implements
        ParamConfigServicePlugin {

	public Boolean getAutomationTestSwitchFlag() throws Exception {

        String flag = PluginConfigProxy
                .getParamValueByName(CLOUDTEST_SCHEDULER_ENABLED);

        if ("true".equalsIgnoreCase(flag) || "yes".equalsIgnoreCase(flag)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public String getMailUserAddress() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_USER_ADDRESS);
    }

    public String getMailHost() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_HOST);
    }

    public String getMailUsername() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_USERNAME);
    }

    public String getMailUserPassword() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_USER_PASSWORD);
    }

    public String getMailPop3Host() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_POP3_HOST);
    }

    public Integer getMailPop3Port() throws Exception {

        return Integer.valueOf(PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAIL_POP3_PORT));
    }

    public String getMailStoreProtocal() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_MAIL_STORE_PROTOCAL);
    }

    public String getMailRobotServiceDeployedServerName() throws Exception {

        return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAIL_ROBOT_SERVICE_HOST_USERNAME);
    }

    public int getMaxDetailedLoadTestResponseAmount() throws Exception {

        String value = PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAX_DETAILED_LOAD_TEST_RESPONSE_AMOUNT);
        if (CommonUtils.isNullOrEmpty(value)) {
            return 100000;
        } else {
            return Integer.valueOf(value);
        }

    }

    public String getUserTransactionJNDI() throws Exception {

        return PluginConfigProxy.getParamValueByName(CLOUDTEST_USER_TRANSACTION_JNDI);
    }

    public String getWebServiceEndpointAddress() throws Exception {

        return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_WEB_SERVICE_ENDPOINT_ADDRESS);
    }

    public int getAutomationTestIntervalHours() throws Exception {

        int i;
        
        String value = PluginConfigProxy
                .getParamValueByName(CLOUDTEST_SCHEDULER_CRON);
        
        if (CommonUtils.isNullOrEmpty(value)) {
            Random random = new Random();
            i = random.nextInt(6);

        }else{
            i = Integer.valueOf(value);
        }

         if(i<=0){
             Random random = new Random();
             i = random.nextInt(6);
         }
        

        return i;
    }

	@Override
	public boolean isMailRobotServiceEnabled() throws Exception {
		 String enabled= PluginConfigProxy
	                .getParamValueByName(CLOUDTEST_MAILSERVICE_ENABLED);
		 return "true".equalsIgnoreCase(enabled);
	}

	@Override
	public String getMailRobotServiceCriteriaOfSubjectPrefix() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_CRITERIA_SUBJECT_PREFIX);
	}

	@Override
	public String getMailRobotServiceOperationOfPostFlag() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_OPERATION_POST_FLAG);
	}

	@Override
	public String getMailRobotServiceRefreshIntervalSeconds() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_REFRESH_INTERVAL_SECONDS);
	}

	@Override
	public String getMailRobotServiceResponseTemplatePathOnFailed() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_TEMPLATE_PATH_ON_FAILED);
	}

	@Override
	public String getMailRobotServiceStoreFolder() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_STORE_FOLDER);
	}

	@Override
	public String getMailRobotServiceReceiveCount() throws Exception {
		return PluginConfigProxy
                .getParamValueByName(CLOUDTEST_MAILSERVICE_RECEIVE_COUNT);
	}
}
