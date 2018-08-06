package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;

import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.plugin.PluginConfig;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.config.plugin.elements.RemoteTestServiceProxyPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.restful.RemoteRESTfulTestServiceProxy;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

public class RemoteTestServiceProxyPluginImpl implements
        RemoteTestServiceProxyPlugin {

    Logger log = Logger.getLogger(this.getClass());

    public Map<String, TestService> create() {

        Map<String, TestService> services = new HashMap<String, TestService>();
        String callType = null;

        try {
            callType = PluginConfigProxy
                    .getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (CommonUtils.isNullOrEmpty(callType)) {
            callType = CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE_WEBSERVICE;
        }

        Map<String, PluginConfig.SlaveServer> slaveMap = PluginConfigProxy
                .getSlaveServerMap();

        Set<String> set = slaveMap.keySet();
        for (String id : set) {
            PluginConfig.SlaveServer ss = slaveMap.get(id);
            if (CommonUtils.isNullOrEmpty(ss.type)) {
                ss.type = callType;
            }

            if (CloudTestConstants.CLOUDTEST_PARALLEL_RPC_TYPE_RESTFUL
                    .equalsIgnoreCase(ss.type)) {
                TestService createByRestful;
                try {
                    createByRestful = createByRestful(ss);
                    services.put(ss.id, createByRestful);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            } else {
                TestService createByWebservice;
                try {
                    createByWebservice = createByWebservice(ss);
                    services.put(ss.id, createByWebservice);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            }
        }
        return services;

    }

    private TestService createByWebservice(PluginConfig.SlaveServer salve)
            throws Exception {

        if (null == salve) {
            throw new Exception("createByWebservice salve is null");
        }

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        factory.setServiceClass(TestService.class);
        factory.setAddress(salve.address);

        TestService service = (TestService) factory.create();
        String address = factory.getAddress() + "?wsdl";

        checkValidAndSetTimeout(salve.id, address, service);
        log.info("Creating TestService successfully from " + salve.id
                + " via 'webservice'");

        return service;
    }

    private TestService createByRestful(PluginConfig.SlaveServer salve)
            throws Exception {

        if (null == salve) {
            throw new Exception("createByRestful salve is null");
        }

        // log.info("Try create TestService from restful rpc...");

        TestService service = new RemoteRESTfulTestServiceProxy(salve.address);
        checkValidAndSetTimeout(salve.id, salve.address, service);
        log.info("Creating TestService successfully from " + salve.id
                + " via 'restful'");

        // log.info("Try create TestService from restful rpc done.");
        return service;
    }

    private void checkValidAndSetTimeout(String id, String address,
            TestService service) throws Exception {

        try {
            URL url;

            url = new URL(address);
            HttpURLConnection net = (HttpURLConnection) url.openConnection();

            String timeoutStr = PluginConfigProxy
                    .getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_REMOTE_NET_TIMEOUT);
            int timeout = CommonUtils.isNullOrEmpty(timeoutStr) ? 1000 * 10
                    : Integer.valueOf(timeoutStr);

            net.setReadTimeout(timeout);

            int state = net.getResponseCode();
            if (state == 200) {

                String ctStr = PluginConfigProxy
                        .getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_REMOTE_SERVICE_CONNECTION_TIMEOUT);
                String rtStr = PluginConfigProxy
                        .getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_REMOTE_SERVICE_RECEIVE_TIMEOUT);

                int ct = CommonUtils.isNullOrEmpty(ctStr) ? 60000 * 10
                        : Integer.valueOf(ctStr);
                int rt = CommonUtils.isNullOrEmpty(ctStr) ? 60000 * 10
                        : Integer.valueOf(rtStr);

                CloudTestUtils.setProxyTimeout(service, ct, rt);

            }
        } catch (Exception e) {
            String errorMsg = "Slave server node [id = "
                    + id
                    + ", address = "
                    + address
                    + "] is not accessible. Prapallel job will ignore this slave server node. Caused by "
                    + e.getMessage();
            log.warn(errorMsg);
            throw new Exception(errorMsg);

        }
    }

}
