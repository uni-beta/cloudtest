package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.lang.reflect.Method;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.config.plugin.elements.CaseRunnerPlugin;

/**
 * An abstract case runner implementation for invoke method. It can be extended
 * by all kinds case runner implementation.
 * 
 * @author jordan.xue
 */
public abstract class AbstractCaseRunnerPluginImpl implements CaseRunnerPlugin {

    /**
     * Check current method is runnable or not by current runner.
     * 
     * @param beanObject
     * @param m
     * @return true, if runnable; otherwise return false.
     */
    protected abstract boolean isRunnable(Object beanObject, Method m);

    /**
     * Running given method with specified parameters with delegated runner.
     * 
     * @param beanObject
     * @param method
     * @param methodParamValueArray
     * @return
     */
    protected abstract CloudTestOutput delegatedRun(Object beanObject,
            Method method, Object[] methodParamValueArray);

    /**
     * Invokes the given method with specified object instance and parameter
     * values.
     * 
     * @param beanObject
     * @param method
     * @param methodParamValueArray
     * @return
     * @throws Exception
     */
    final public CloudTestOutput invoke(Object beanObject, Method method,
            Object[] methodParamValueArray) throws Exception {

        CloudTestOutput output = new CloudTestOutput();

        Object returnValue;
        Double runTime = null;

        long start = System.currentTimeMillis();
        long end = -1;

        returnValue = method.invoke(beanObject, methodParamValueArray);

        end = System.currentTimeMillis();
        runTime = (end - start) / 1000.00;

        output.setReturnValue(returnValue);
        output.setStatus(true);
        output.setRunTime(runTime);

        return output;
    }

    public CloudTestOutput run(Object beanObject, Method method,
            Object[] methodParamValueArray) throws Exception {

        CloudTestOutput output = new CloudTestOutput();
        output.setStatus(true);
        
        output = invoke(beanObject, method, methodParamValueArray);

        return output;
    }

}
