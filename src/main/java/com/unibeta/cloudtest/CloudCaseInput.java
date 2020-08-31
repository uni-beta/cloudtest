package com.unibeta.cloudtest;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.unibeta.cloudtest.util.CloudTestUtils;

/**
 * The input object for case testing service.
 */
@XmlRootElement
public class CloudCaseInput implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String fileName;
    private String[] caseId;

    public String getFileName() {

        return CloudTestUtils.trimString(fileName);
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public String[] getCaseId() {

        return caseId;
    }

    public void setCaseId(String[] caseId) {

        this.caseId = caseId;
    }

}
