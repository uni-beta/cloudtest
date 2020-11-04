package com.unibeta.cloudtest.restful;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.CloudCaseInput;
import com.unibeta.cloudtest.CloudLoadInput;
import com.unibeta.cloudtest.CloudTestInput;
import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.TestService;
import com.unibeta.cloudtest.config.CloudTestCase;
import com.unibeta.cloudtest.config.CloudTestCase.Case;
import com.unibeta.cloudtest.config.CloudTestCase.Case.Parameter;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.restful.http.HttpClientUtils;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Restful implementation for TestService.
 * 
 * @author jordan.xue
 */
public class RemoteRESTfulTestServiceProxy implements TestService {

	
	private static final String UTF_8 = "UTF-8";
	
	private static Logger log = LoggerFactory
			.getLogger(RemoteRESTfulTestServiceProxy.class);
	private String url;

	public RemoteRESTfulTestServiceProxy(String url) {

		this.url = url;
	}

	@SuppressWarnings("unused")
	private RemoteRESTfulTestServiceProxy() {

	}

	/**
	 * cloudLoadTestServiceByCase
	 * 
	 * @param loadTestInput
	 * @return
	 */
	@WebMethod(operationName = OPERATION_NAME_CLOUD_LOAD_TEST_SERVICE_BY_CASE)
	@WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doLoadTest(
			@WebParam(name = WEB_PARAM_CLOUD_LOAD_INPUT, mode = Mode.IN) CloudLoadInput loadTestInput) {

		CloudTestCase cloudTestCase = buildLoadTestInputCase(loadTestInput);
		CloudTestOutput output = invoke(cloudTestCase);

		return output;
	}

	private CloudTestOutput invoke(CloudTestCase cloudTestCase) {

		CloudTestOutput output = new CloudTestOutput();

		if (CommonUtils.isNullOrEmpty(this.url)) {
			output.setStatus(false);
			output.setErrorMessage("target rpc url[" + this.url
					+ "] is invalid");
		}

		byte[] request = null;
		byte[] responses = null;
		try {
			request = ObjectSerializer.marshalToXml(cloudTestCase).getBytes(UTF_8);

			Map<String, String> header = new HashMap<String, String>();
			header.put(CloudTestConstants.CLOUDTEST_OPERATION,
					CloudTestConstants.CLOUDTEST_OPERATION_CLOUDTEST);
			
			// MD5 check had been disabled
			/*if ("true".equalsIgnoreCase(PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_RESTFUL_CHECK_MD5_ENABLE))) {
				header.put(CloudTestConstants.HTTP_HEADER_CONTENT_LENGTH, ""
						+ request.length);
				header.put(CloudTestConstants.HTTP_HEADER_CONTENT_MD5,
						CloudTestUtils.getMD5code(request));
				
			}*/
			
			String authToken = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_PARALLEL_AUTH_TOKEN);
			if(!CommonUtils.isNullOrEmpty(authToken)) {
				header.put(CloudTestConstants.AUTH_AUTHORIZATION, CloudTestConstants.AUTH_BEARER + authToken);
			}
			
			responses = HttpClientUtils.post(url, header, request);

			output = (CloudTestOutput) ObjectSerializer.unmarshalToObject(
					new String(responses,UTF_8), CloudTestOutput.class);

		} catch (Exception e) {
			e.printStackTrace();
			String string = "Remote RESTful Proxy invoking is failed from "
					+ url + "\n" + "cloudtest restful response is:"
					+ new String(responses) + "\n";
			log.error(string + e.getMessage(), e);

			output.setStatus(false);
			output.setErrorMessage(string
					+ CloudTestUtils.printExceptionStackTrace(e));
		}

		if (output == null) {
			CloudTestOutput out = new CloudTestOutput();
			out.setStatus(false);
			out.setErrorMessage("return null with unknown issue");

			return out;
		} else {
			return output;
		}

	}

	private CloudTestCase buildLoadTestInputCase(CloudLoadInput loadTestInput) {

		CloudTestCase cloudTestCase = new CloudTestCase();

		Case c = new Case();
		c.className = CloudTestService.class.getCanonicalName();
		c.methodName = "doLoadTest";

		Parameter p1 = new Parameter();
		p1.dataType = CloudLoadInput.class.getCanonicalName();
		p1.parameterType = "0";
		p1.value = ObjectDigester.toXML(loadTestInput);

		c.parameter.add(p1);

		cloudTestCase.testCase.add(c);
		return cloudTestCase;
	}

	/**
	 * cloudTestServiceByTestCase
	 * 
	 * @param input
	 * @return
	 */
	@WebMethod(operationName = OPERATION_NAME_CLOUD_TEST_SERVICE_BY_TEST_CASE)
	@WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doTest(
			@WebParam(name = WEB_PARAM_CLOUD_CASE_INPUT, mode = Mode.IN) CloudCaseInput input) {

		CloudTestCase cloudTestCase = buildCloudCaseInputCase(input);
		CloudTestOutput output = invoke(cloudTestCase);

		return output;
	}

	private CloudTestCase buildCloudCaseInputCase(CloudCaseInput input) {

		CloudTestCase cloudTestCase = new CloudTestCase();

		Case c = new Case();
		c.className = CloudTestService.class.getCanonicalName();
		c.methodName = "doTest";

		Parameter p1 = new Parameter();
		p1.dataType = CloudCaseInput.class.getCanonicalName();
		p1.parameterType = "0";
		p1.value = ObjectDigester.toXML(input);

		c.parameter.add(p1);

		cloudTestCase.testCase.add(c);
		return cloudTestCase;
	}

	/**
	 * cloudTestServiceByTestCase
	 * 
	 * @param input
	 * @return
	 */
	@WebMethod(operationName = OPERATION_NAME_CLOUD_TEST_SERVICE_BY_PARAMETER)
	@WebResult(name = WEB_RESULT_CLOUD_TEST_RESULT)
	public CloudTestOutput doTest(
			@WebParam(name = WEB_PARAM_TEST_CASE, mode = Mode.IN) CloudTestInput input) {

		CloudTestCase cloudTestCase = buildCloudTestInputCase(input);
		CloudTestOutput output = invoke(cloudTestCase);

		if (output.getTestCaseResults() != null
				&& output.getTestCaseResults().size() == 1) {
			return (CloudTestOutput) ObjectDigester.fromXML(output
					.getTestCaseResults().get(0).getReturns());
		} else {
			return output;
		}
	}

	private CloudTestCase buildCloudTestInputCase(CloudTestInput input) {

		CloudTestCase cloudTestCase = new CloudTestCase();

		Case c = new Case();
		c.className = CloudTestService.class.getCanonicalName();
		c.methodName = "doTest";

		Parameter p1 = new Parameter();
		p1.dataType = CloudTestInput.class.getCanonicalName();
		p1.parameterType = "0";
		p1.value = ObjectDigester.toXML(input);

		c.parameter.add(p1);

		cloudTestCase.testCase.add(c);
		return cloudTestCase;
	}

}
