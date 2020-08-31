package com.unibeta.cloudtest;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * User for load testing input.
 * 
 * @author jordan.xue
 */
@XmlRootElement
public class CloudLoadInput extends CloudCaseInput implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Long concurrentNumber;
    private Long concurrentSeconds;
    private Long maxThreadPoolSize;

    public Long getConcurrentNumber() {

        return concurrentNumber;
    }

    public void setConcurrentNumber(Long concurrentNumber) {

        this.concurrentNumber = concurrentNumber;
    }

    public Long getConcurrentSeconds() {

        return concurrentSeconds;
    }

    public void setConcurrentSeconds(Long concurrentSeconds) {

        this.concurrentSeconds = concurrentSeconds;
    }

    public Long getMaxThreadPoolSize() {

        return maxThreadPoolSize;
    }

    public void setMaxThreadPoolSize(Long maxThreadPoolSize) {

        this.maxThreadPoolSize = maxThreadPoolSize;
    }

}
