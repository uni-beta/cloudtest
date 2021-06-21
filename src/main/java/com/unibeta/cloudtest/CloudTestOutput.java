package com.unibeta.cloudtest;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.StringEscapeUtils;

import com.unibeta.cloudtest.assertion.AssertResult;
import com.unibeta.cloudtest.config.bind.XmlCDATAAdatper;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * <code>CloudTestOutput</code> a test response object.
 * 
 * @author jordan.xue
 */
@XmlRootElement(name= TestService.WEB_RESULT_CLOUD_TEST_RESULT)
public class CloudTestOutput implements Serializable {

    private static final long serialVersionUID = 1L;

    private CloudTestInput testCase;
     
    private String returns;
    private String className;
    private String casePath;
    private String group;

    private Boolean status;

    private Object returnValue;

    private String errorMessage;

    private ResultStatistics resultStatistics;

    private String caseId;

    private List<AssertResult> failedAssertResults;

    private List<CloudTestOutput> testCaseResults;

    // in second
    private Double runTime = 0.0;
    private Date timestamp = null;

    
    public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getClassName() {

        return className;
    }

    public void setClassName(String className) {

        this.className = className;
    }

    public String getCasePath() {

        return casePath;
    }

    public void setCasePath(String casePath) {

        this.casePath = casePath;
    }

    public String getCaseId() {

        return caseId;
    }

    public void setCaseId(String caseId) {

        this.caseId = caseId;
    }

    public List<CloudTestOutput> getTestCaseResults() {

        return testCaseResults;
    }

    public void setTestCaseResults(List<CloudTestOutput> testCaseOutputList) {

        this.testCaseResults = testCaseOutputList;
    }

    public ResultStatistics getResultStatistics() {

        return resultStatistics;
    }

    public void setResultStatistics(ResultStatistics resultStatistics) {

        this.resultStatistics = resultStatistics;
    }

    public List<AssertResult> getFailedAssertResults() {

        return failedAssertResults;
    }

    public void setFailedAssertResults(List<AssertResult> failedAssertResultList) {

        this.failedAssertResults = failedAssertResultList;
    }

    public Object getReturnValue() {

        return returnValue;
    }

    public void setReturnValue(Object returnValue) {

        this.returnValue = returnValue;
    }

    /**
     * Returned object in xml string
     * 
     * @return
     */
    @XmlJavaTypeAdapter(XmlCDATAAdatper.class)
    public String getReturns() {

        return returns;
    }

    public void setReturns(String returnValueInXml) {

        this.returns = returnValueInXml;
    }

    public Boolean getStatus() {

        return status;
    }

    public void setStatus(Boolean status) {

    	if(null == timestamp) {
    		timestamp = new Date();
    	}
    	
        this.status = status;
    }

    public String getErrorMessage() {
    	
       return StringEscapeUtils.escapeXml(errorMessage);
    }

    public void setErrorMessage(String errorMsg) {

        this.errorMessage = errorMsg;
    }

    public CloudTestInput getTestCase() {

        return testCase;
    }

    public void setTestCase(CloudTestInput testCase) {

        this.testCase = testCase;
    }

    /**
     * Running time in second
     * 
     * @return
     */
    public Double getRunTime() {

        return runTime;
    }

    public void setRunTime(Double performanceTimeCost) {

        this.runTime = performanceTimeCost;
    }

    /**
     * <code>ResultStatistics</code> is a common testing result statistics
     * object.
     * 
     * @author jordan.xue
     */
    public static class ResultStatistics {

        private Date timestamp = new Date();
        private Integer successfulAmount = 0;
        private Integer failedAmount = 0;
        private Double passRate = 0.0;
        private Integer totalAmount = 0;
        private Double totalRunTime = 0.0;
        private Double durationTime = 0.0;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {

            this.timestamp = timestamp;
        }

        public Double getDurationTime() {

            return durationTime;
        }

        public void setDurationTime(Double durationTime) {

            this.durationTime = durationTime;
        }

        public Double getTotalRunTime() {

            return totalRunTime;
        }

        public void setTotalRunTime(Double totalTimeCost) {

            this.totalRunTime = totalTimeCost;
        }

        public Integer getSuccessfulAmount() {

            return successfulAmount;
        }

        public void setSuccessfulAmount(Integer successfullAmount) {

            this.successfulAmount = successfullAmount;
        }

        public Integer getFailedAmount() {

            return failedAmount;
        }

        public void setFailedAmount(Integer failedAmount) {

            this.failedAmount = failedAmount;
        }

        public Double getPassRate() {

            return passRate;
        }

        public void setPassRate(Double passRate) {

            this.passRate = passRate;
        }

        public Integer getTotalAmount() {

            return totalAmount;
        }

        public void setTotalAmount(Integer totalAmount) {

            this.totalAmount = totalAmount;
        }

    }
}
