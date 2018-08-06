package com.unibeta.cloudtest.config.plugin.elements;

import java.util.Map;

import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;


public interface RemoteTestServiceProxyPlugin extends CloudTestPlugin {

    public Map<String,TestService> create();
}
