package com.unibeta.cloudtest.config.plugin.elements;

import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;

/**
 * The basic plugin interface for common parameters, such as mail information
 * and switch flag.
 * 
 * @author jordan.xue
 */
public interface ParamConfigServicePlugin extends CloudTestPlugin {

    /**
     * Gets User transaction JNDI name for J2EE transaction usage.
     * 
     * @return
     * @throws Exception
     */
    public String getUserTransactionJNDI() throws Exception;

    /**
     * Mail from name displayed in the mail head, such as 'xxx@abc.com'
     * 
     * @return
     * @throws Exception
     */
    public String getMailUserAddress() throws Exception;

    /**
     * Mail host name,such as 'mail.abc.com'
     * 
     * @return
     * @throws Exception
     */
    public String getMailHost() throws Exception;

    /**
     * Mail pop3 port,such as '995'
     * 
     * @return
     * @throws Exception
     */
    public Integer getMailPop3Port() throws Exception;

    /**
     * Mail pop3 protocol,such as 'pop3'
     * 
     * @return
     * @throws Exception
     */
    public String getMailStoreProtocal() throws Exception;

    /**
     * Mail pophost name,such as 'pop3.abc.com'
     * 
     * @return
     * @throws Exception
     */
    public String getMailPop3Host() throws Exception;

    /**
     * Mail account, such as 'xxx'
     * 
     * @return
     * @throws Exception
     */
    public String getMailUsername() throws Exception;

    /**
     * Mail account password, such as 'ABC1243'
     * 
     * @return
     * @throws Exception
     */
    public String getMailUserPassword() throws Exception;

    /**
     * Whether switch on automatic cloud test service.
     * 
     * @return
     * @throws Exception
     */
    public Boolean getAutomationTestSwitchFlag() throws Exception;

    /**
     * get the name which Mail robot service was deployed to
     * 
     * @return
     * @throws Exception
     */
    public String getMailRobotServiceDeployedServerName() throws Exception;
    
    /**
     * Check whether mail service is enabled or not.
     * 
     * @return
     * @throws Exception
     */
    public boolean isMailRobotServiceEnabled() throws Exception;
    
    /**
     * gets criteria of subject prefix for mail service.
     * 
     * @return
     * @throws Exception
     */
    public String getMailRobotServiceCriteriaOfSubjectPrefix() throws Exception;
    
    /**
     * gets post operation for mail service.
     * 
     * @return
     * @throws Exception
     */
    public String getMailRobotServiceOperationOfPostFlag() throws Exception;
    
    /**
     * gets refresh time in seconds for mail service.
     * 
     * @return
     * @throws Exception
     */
    public String getMailRobotServiceRefreshIntervalSeconds() throws Exception;
    
    /**
     * gets response template path on failed for mail service.
     * 
     * @return
     * @throws Exception
     */
    public String getMailRobotServiceResponseTemplatePathOnFailed() throws Exception;
    
    /**
     * gets mail store folder for mail service.
     * 
     * @return folder name
     * @throws Exception
     */
    public String getMailRobotServiceStoreFolder() throws Exception;
    
    /**
     * gets receive count for mail service.
     * 
     * @return folder name
     * @throws Exception
     */
    public String getMailRobotServiceReceiveCount() throws Exception;

    /**
     * Gets the max detailed load test response amount
     * 
     * @return
     * @throws Exception
     */
    public int getMaxDetailedLoadTestResponseAmount() throws Exception;

    /**
     * Get the Endpoint Address, which cloudtest web service is deployed.
     * 
     * @return the endpoint address. For example, the return value should be
     *         "http://localhost:80/myapp/services/CloudTestServices"
     * @throws Exception
     */
    public String getWebServiceEndpointAddress() throws Exception;

    /**
     * Gets the interval hours of automation test. By default, it is random
     * between 2hrs and 8hrs.
     * 
     * @return
     * @throws Exception
     */
    public int getAutomationTestIntervalHours() throws Exception;
}
