package com.unibeta.cloudtest.config.plugin.elements;

import java.lang.reflect.Method;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;

/**
 * Case runner, it can be extended and customized to plugin any other unit
 * testing framework.
 * 
 * @author jordan.xue
 */
public interface CaseRunnerPlugin extends CloudTestPlugin {

    /**
     * Run given method via parameter array in beanObject instance.
     * 
     * @param beanObject
     * @param method
     * @param methodParamValueArray
     * @return
     * @throws Exception
     */
    public CloudTestOutput run(Object beanObject, Method method,
            Object[] methodParamValueArray) throws Exception;
}
