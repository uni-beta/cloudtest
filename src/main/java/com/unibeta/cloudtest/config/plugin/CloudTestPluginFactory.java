package com.unibeta.cloudtest.config.plugin;

import com.unibeta.cloudtest.config.plugin.elements.CaseRunnerPlugin;
import com.unibeta.cloudtest.config.plugin.elements.CloudTestReportXMLAggregatorPlugin;
import com.unibeta.cloudtest.config.plugin.elements.ParamConfigServicePlugin;
import com.unibeta.cloudtest.config.plugin.elements.RemoteTestServiceProxyPlugin;
import com.unibeta.cloudtest.config.plugin.elements.ReportGeneratorPlugin;
import com.unibeta.cloudtest.config.plugin.elements.SpringBeanFactoryPlugin;
import com.unibeta.cloudtest.config.plugin.elements.UserTransactionPlugin;
import com.unibeta.cloudtest.config.plugin.elements.impl.CloudTestReportXMLAggregatorPluginImpl;
import com.unibeta.cloudtest.config.plugin.elements.impl.JUnitCaseRunnerPluginImpl;
import com.unibeta.cloudtest.config.plugin.elements.impl.ParamConfigServicePluginImpl;
import com.unibeta.cloudtest.config.plugin.elements.impl.RemoteTestServiceProxyPluginImpl;
import com.unibeta.cloudtest.config.plugin.elements.impl.ReportGeneratorPluginImpl;
import com.unibeta.cloudtest.config.plugin.elements.impl.SpringBeanFactoryPluginImpl;

/**
 * CloudTestPluginFactory manages all plugin instance accessing.
 * 
 * @author jordan.xue
 */
public class CloudTestPluginFactory {

    public static ParamConfigServicePlugin getParamConfigServicePlugin() {

        ParamConfigServicePlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (ParamConfigServicePlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(ParamConfigServicePlugin.class
                            .getName());
        } catch (Exception e) {
            cloudTestPluginInstance = new ParamConfigServicePluginImpl();
        }

        return cloudTestPluginInstance;
    }

    public static UserTransactionPlugin getUserTransactionPlugin() {

        UserTransactionPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (UserTransactionPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(UserTransactionPlugin.class
                            .getName());
        } catch (Exception e) {
            // cloudTestPluginInstance = new UserTransactionPluginImpl();
            return null;
        }

        return cloudTestPluginInstance;
    }

    public static SpringBeanFactoryPlugin getSpringBeanFactoryPlugin() {

        SpringBeanFactoryPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (SpringBeanFactoryPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(SpringBeanFactoryPlugin.class
                            .getName());
        } catch (Exception e) {
        	try {
				Class.forName("org.springframework.beans.factory.BeanFactory");
				cloudTestPluginInstance = new SpringBeanFactoryPluginImpl();
			} catch (ClassNotFoundException e1) {
				cloudTestPluginInstance = null;
			}
            
        }

        return cloudTestPluginInstance;
    }

    public static ReportGeneratorPlugin getReportGeneratorPlugin() {

        ReportGeneratorPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (ReportGeneratorPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(ReportGeneratorPlugin.class
                            .getName());
        } catch (Exception e) {
            cloudTestPluginInstance = new ReportGeneratorPluginImpl();
        }

        return cloudTestPluginInstance;
    }

    public static CloudTestReportXMLAggregatorPlugin getCloudTestReportXMLAggregatorPlugin() {

        CloudTestReportXMLAggregatorPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (CloudTestReportXMLAggregatorPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(CloudTestReportXMLAggregatorPlugin.class
                            .getName());
        } catch (Exception e) {
            cloudTestPluginInstance = new CloudTestReportXMLAggregatorPluginImpl();
        }

        return cloudTestPluginInstance;
    }

    public static CaseRunnerPlugin getCaseRunnerPlugin() {

        CaseRunnerPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (CaseRunnerPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(CaseRunnerPlugin.class
                            .getName());
        } catch (Exception e) {
            cloudTestPluginInstance = new JUnitCaseRunnerPluginImpl();
        }

        return cloudTestPluginInstance;
    }

    public static RemoteTestServiceProxyPlugin getRemoteTestServiceProxyPlugin() {

        RemoteTestServiceProxyPlugin cloudTestPluginInstance = null;

        try {
            cloudTestPluginInstance = (RemoteTestServiceProxyPlugin) PluginConfigProxy
                    .getCloudTestPluginInstance(RemoteTestServiceProxyPlugin.class
                            .getName());
        } catch (Exception e) {
            cloudTestPluginInstance = new RemoteTestServiceProxyPluginImpl();
        }

        return cloudTestPluginInstance;
    }
}
