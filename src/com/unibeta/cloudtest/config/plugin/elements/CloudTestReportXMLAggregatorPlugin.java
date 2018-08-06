package com.unibeta.cloudtest.config.plugin.elements;

import com.unibeta.cloudtest.CloudTestOutput;

public interface CloudTestReportXMLAggregatorPlugin {

    public static final String AGGREGATOR_TYPE_CLASS_NAME = "className";
    public static final String AGGREGATOR_TYPE_CASE_PATH = "casePath";

    public CloudTestReportXML aggregate(CloudTestOutput cloudTestOutput)
            throws Exception;
}
