package com.unibeta.cloudtest;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.WebParam.Mode;

/**
 * Core test server for local and distributed/remote as net grid.
 * 
 * @author jordan.xue
 */
@WebService(name = TestService.WEB_SERVICE_NAME_CLOUD_TEST_SERVICE, targetNamespace = TestService.NAME_SPACE_CLOUDTEST_UNIBETA_COM)
public interface TestService {

    public static final String OPERATION_NAME_CLOUD_TEST_SERVICE_BY_TEST_CASE = "cloudTestServiceByTestCase";
    public static final String WEB_PARAM_TEST_CASE = "testCase";
    public static final String OPERATION_NAME_CLOUD_TEST_SERVICE_BY_PARAMETER = "cloudTestServiceByParameter";
    public static final String WEB_PARAM_CLOUD_CASE_INPUT = "cloudCaseInput";
    public static final String WEB_PARAM_CLOUD_LOAD_INPUT = "cloudLoadInput";
    public static final String WEB_RESULT_CLOUD_TEST_RESULT = "cloudTestResult";
    public static final String OPERATION_NAME_CLOUD_LOAD_TEST_SERVICE_BY_CASE = "cloudLoadTestServiceByCase";
    public static final String NAME_SPACE_CLOUDTEST_UNIBETA_COM = "cloudtest.unibeta.com";
    public static final String WEB_SERVICE_NAME_CLOUD_TEST_SERVICE = "CloudTestService";
    
    public static final String CLOUDTEST_ERROR_MESSAGE_NO_TEST_CASE_WAS_FOUND = "No TestCase was found!";

    /**
     * cloudLoadTestServiceByCase
     * 
     * @param loadTestInput
     * @return
     */
    @WebMethod(operationName = OPERATION_NAME_CLOUD_LOAD_TEST_SERVICE_BY_CASE)
    @WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
    public CloudTestOutput doLoadTest(
            @WebParam(name = WEB_PARAM_CLOUD_LOAD_INPUT, mode = Mode.IN)
            CloudLoadInput loadTestInput);

    /**
     * cloudTestServiceByTestCase
     * 
     * @param input
     * @return
     */
    @WebMethod(operationName = OPERATION_NAME_CLOUD_TEST_SERVICE_BY_TEST_CASE)
    @WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
    public CloudTestOutput doTest(
            @WebParam(name = WEB_PARAM_CLOUD_CASE_INPUT, mode = Mode.IN)
            CloudCaseInput input);

    /**
     * cloudTestServiceByTestCase
     * 
     * @param input
     * @return
     */
    @WebMethod(operationName = OPERATION_NAME_CLOUD_TEST_SERVICE_BY_PARAMETER)
    @WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
    public CloudTestOutput doTest(
            @WebParam(name = WEB_PARAM_TEST_CASE, mode = Mode.IN)
            CloudTestInput input);
}
