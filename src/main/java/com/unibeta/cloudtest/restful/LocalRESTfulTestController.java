package com.unibeta.cloudtest.restful;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestService;
import com.unibeta.cloudtest.config.CloudTestCase;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * Test Service Controller for REST interface invoking. Only <cloudtest/> is
 * supported.
 * 
 * @author jordan.xue
 */
public class LocalRESTfulTestController {

	private static final String CLOUD_TEST_TAG = "cloudtest";
	private static final String REQUEST_CLOUD_TEST_CASE = "</" + CLOUD_TEST_TAG
			+ ">";
	private static Logger log = LoggerFactory
			.getLogger(LocalRESTfulTestController.class);

	/**
	 * invokes from request in string.
	 * 
	 * @param request
	 * @return CloudTestOutput instance or exception message.
	 */
	public static String invoke(String request) {

		log.debug("RESTful invoking input request is:\n" + request);

		String response = null;
		CloudTestOutput result = new CloudTestOutput();

		if (CommonUtils.isNullOrEmpty(request)) {
			result.setStatus(false);
			result.setErrorMessage("restful request is none.");

			try {
				response = ObjectSerializer.marshalToXml(result);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return StringEscapeUtils.unescapeXml(response);
		}

		
		
		request = request.replace("cloudTestCase", CLOUD_TEST_TAG);

		int indexOfStart = request.indexOf("<" + CLOUD_TEST_TAG);
		int indexOfEnd = request.indexOf(REQUEST_CLOUD_TEST_CASE);

		try {
			if (!CommonUtils.isNullOrEmpty(request) && indexOfStart >= 0
					&& indexOfEnd > 0) {

				String caseStr = request.substring(indexOfStart, indexOfEnd
						+ REQUEST_CLOUD_TEST_CASE.length());

				CloudTestCase cloudTestCase = (CloudTestCase) ObjectSerializer
						.unmarshalToObject(caseStr, CloudTestCase.class);

				// PluginConfigProxy.refresh();

				result = new CloudTestService().doTest(cloudTestCase);
			} else {
				
				result.setStatus(false);
				result.setErrorMessage("request format is invalid, No <cloudtest/> was found in below content:\n "
						+ StringEscapeUtils.escapeXml(request));
				log.error(result.getErrorMessage());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.setStatus(false);
			result.setErrorMessage(CloudTestUtils.printExceptionStackTrace(e));
		} finally {
			CloudTestOutput output = null;
			
			try {
				if (result.getTestCaseResults() != null
						&& result.getTestCaseResults().size() == 1) {
					Object fromXML = ObjectDigester.fromXML(result
							.getTestCaseResults().get(0).getReturns());
					if (fromXML instanceof CloudTestOutput) {
						output = (CloudTestOutput) fromXML;
					} else {
						output = result.getTestCaseResults().get(0);
					}
				} else {
					output = result;
				}
				
				output.setErrorMessage(StringEscapeUtils.escapeXml(output
						.getErrorMessage()));
				
				CloudTestUtils.processResultStatistics(output, false);
				response = ObjectSerializer.marshalToXml(output);

			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage(), e);
				response = CloudTestUtils.printExceptionStackTrace(e)
						+ result.getErrorMessage() == null ? ""
						: "\n with nested exception:\n"
								+ result.getErrorMessage();
			}
		}

		return StringEscapeUtils.unescapeXml(response);
	}
}
