package com.unibeta.cloudtest.parallel.mapred;

import java.util.ArrayList;
import java.util.List;

import com.unibeta.cloudtest.CloudTestOutput;

public class Task {

    public static Integer STATUS_PENDING = 0;
    public static Integer STATUS_SUBMITTED = 1;
    public static Integer STATUS_INPROCESS = 2;
    public static Integer STATUS_DONE = 3;
    public static Integer STATUS_FAILED = 4;
    public static Integer STATUS_REJECTED = 5;

    private String caseUri;
    private String[] caseId;
    private int status = 0;
    private String owner;
    private List<String> historyOwners = new ArrayList<String>();
    private long blockSize;
    private String message;

    public String[] getCaseId() {
		return caseId;
	}

	public void setCaseId(String[] caseId) {
		this.caseId = caseId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getBlockSize() {

        return blockSize;
    }

    public void setBlockSize(long blockSize) {

        this.blockSize = blockSize;
    }

    public String getCaseUri() {

        return caseUri;
    }

    public void setCaseUri(String caseUri) {

        this.caseUri = caseUri;
    }

    public Integer getStatus() {

        return status;
    }

    public void setStatus(Integer status) {

        this.status = status;
    }

    public String getOwner() {

        return owner;
    }

    public void setOwner(String owner) {

        this.owner = owner;
    }

    public CloudTestOutput getResult() {

        return result;
    }

    public void setResult(CloudTestOutput result) {

        this.result = result;
    }

    private String id;

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public List<String> getHistoryOwners() {

        return historyOwners;
    }

    public void setHistoryOwners(List<String> historyOwners) {

        this.historyOwners = historyOwners;
    }

    private CloudTestOutput result;

    public boolean equals(Object o) {

        if (o != null && o instanceof Task && null != this.getId()) {
            Task t = (Task) o;
            return this.getId().equals(t.getId());
        } else {
            return false;
        }

    }
}
