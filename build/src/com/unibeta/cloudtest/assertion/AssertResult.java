package com.unibeta.cloudtest.assertion;

import javax.xml.bind.annotation.XmlRootElement;

import com.unibeta.vrules.annotation.VRules4jAnnotations.ValidationErrorMessage;

/**
 * A basic assert result object, which will be returned back if validate failed.
 * 
 * @author jordan.xue
 */
@XmlRootElement
public class AssertResult {

    private String caseId ;
    private String module ;
    private String type ;

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getModule() {

        return module;
    }

    public void setModule(String module) {

        this.module = module;
    }

    @ValidationErrorMessage
    private String errorMessage ;

    private String ownerMails ;

    public String getCaseId() {

        return caseId;
    }

    public void setCaseId(String caseId) {

        this.caseId = caseId;
    }

    public String getErrorMessage() {

        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {

        this.errorMessage = errorMessage;
    }

    public String getOwnerMails() {

        return ownerMails;
    }

    public void setOwnerMails(String ownerMails) {

        this.ownerMails = ownerMails;
    }

}
