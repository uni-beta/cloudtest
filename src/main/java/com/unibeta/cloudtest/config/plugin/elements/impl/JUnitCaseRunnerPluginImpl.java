package com.unibeta.cloudtest.config.plugin.elements.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.assertion.AssertResult;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * JUnit case runner plugin.
 * 
 * @author jordan.xue
 */
public class JUnitCaseRunnerPluginImpl extends AbstractCaseRunnerPluginImpl {

    protected CloudTestOutput delegatedRun(Object beanObject, Method method,
            Object[] methodParamValueArray) {

        CloudTestOutput output = null;

        Result result = new JUnitCore().run(Request.method(
                beanObject.getClass(), method.getName()));
        output = convertJUnitResult2CloudTestOutput(result);

        return output;
    }

    private CloudTestOutput convertJUnitResult2CloudTestOutput(Result result) {

        CloudTestOutput output = new CloudTestOutput();

        if (result == null) {
            output.setStatus(false);
            output.setErrorMessage("JUnit returned null result.");

            return output;
        }

        if (result.wasSuccessful()) {
            output.setStatus(true);
        } else {
            output.setStatus(false);

            List<AssertResult> assertList = new ArrayList<AssertResult>();

            for (Failure f : result.getFailures()) {
                AssertResult ar = new AssertResult();
                ar.setErrorMessage(f.getMessage());
                assertList.add(ar);

                String exceptionTrace = f.getTrace();
//              exceptionTrace = CloudTestUtils.printExceptionStackTrace(f.getException());
                
                if (!CommonUtils.isNullOrEmpty(exceptionTrace)) {
                    if (!CommonUtils.isNullOrEmpty(output.getErrorMessage())) {
                        output.setErrorMessage(output.getErrorMessage() + "\nmore...\n"
                                + exceptionTrace);
                    } else {
                        output.setErrorMessage(exceptionTrace);
                    }
                }
            }

            output.setFailedAssertResults(assertList);
        }

        output.setRunTime(result.getRunTime() / 1000.00);

        return output;
    }

    protected boolean isRunnable(Object beanObject, Method m) {

        try {
            Class.forName("junit.framework.TestCase");
            if (beanObject instanceof TestCase
                    && m.getName().startsWith("test")) {
                return true;
            }

            Class.forName("org.junit.Test");
            if (m.getAnnotation(Test.class) != null) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            return false;
        }

        return false;
    }
    
    public CloudTestOutput run(Object beanObject, Method method,
            Object[] methodParamValueArray) throws Exception {

        CloudTestOutput output = new CloudTestOutput();
        output.setStatus(true);

        if (this.isRunnable(beanObject, method)) {
            output = this.delegatedRun(beanObject, method, methodParamValueArray);
        } else {
            output = super.run(beanObject, method, methodParamValueArray);
        }

        return output;
    }

}
