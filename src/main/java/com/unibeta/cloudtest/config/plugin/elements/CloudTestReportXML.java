package com.unibeta.cloudtest.config.plugin.elements;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.unibeta.cloudtest.CloudTestOutput.ResultStatistics;

@XmlRootElement
public class CloudTestReportXML {

	@XmlTransient
	public static final Map<String, Integer> CHANGE_TYPE_MAP = new HashMap<String, Integer>();

	@XmlTransient
	public static final String CHANGE_TYPE_NOCHANGED = "0";// #4169E1
	@XmlTransient
	public static final String CHANGE_TYPE_NEWFAILED = "red";// B22222
	@XmlTransient
	public static final String CHANGE_TYPE_NEWSUCCESS = "green";
	@XmlTransient
	public static final String CHANGE_TYPE_NEWADDED = "blue";

	{
		CHANGE_TYPE_MAP.put(CHANGE_TYPE_NOCHANGED, 0);
		CHANGE_TYPE_MAP.put(CHANGE_TYPE_NEWFAILED, 3);
		CHANGE_TYPE_MAP.put(CHANGE_TYPE_NEWSUCCESS, 2);
		CHANGE_TYPE_MAP.put(CHANGE_TYPE_NEWADDED, 1);
	}

	// in second
	@XmlAttribute
	public Double executionTime = 0D;
	@XmlAttribute
	public Boolean status = false;
	@XmlAttribute
	public String caseId = "";
	@XmlAttribute
	public String casePath = "";
	@XmlAttribute
	public String group = "";
	@XmlAttribute
	public String changeType = CHANGE_TYPE_NOCHANGED;
	@XmlAttribute
	public Date timestamp = null;

	@XmlElement
	public String testCase;
	@XmlElement(name = "return")
	public String returns;
	@XmlElement
	public String errorMessage;
	@XmlElement
	public String failedAssertResults;
	@XmlElement
	public ResultStatistics resultStatistics;
	@XmlElement
	public List<TestCaseResults> testCaseResults;

	public static class TestCaseResults {
		@XmlAttribute
		public String group;
		@XmlAttribute
		public String changeType = CHANGE_TYPE_NOCHANGED;
		@XmlElement
		public List<CloudTestReportXML> testCaseResult;
	}

}
