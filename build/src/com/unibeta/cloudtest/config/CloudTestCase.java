package com.unibeta.cloudtest.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.unibeta.cloudtest.config.bind.XmlCDATAAdatper;

/**
 * Test case suit object. It is a common model for configuration serialization
 * from xml to java object.
 * 
 * @author jordan.xue
 */
@XmlRootElement(name = "cloudtest")
public class CloudTestCase {

    @XmlAttribute
    public String ns = null;
    @XmlAttribute
    public String nsLibs = null;

    @XmlAttribute
    public String assertRuleFile = "";
    @XmlAttribute
    public String imports = "";
    @XmlAttribute
    public String depends = "";
    @XmlAttribute
    public String ignore = "false";
    @XmlAttribute
    public String group = "";
    @XmlElement
    public List<Case> testCase = new ArrayList<Case>();

    /**
     * A common test case object.
     * 
     * @author jordan.xue
     */
    public static class Case {

        public static class Assertion {

            @XmlElement(name = "timeout")
            public Double timeout = -1.0;
            @XmlElement(name = "assert")
            public String assert_ = "true";
            @XmlElement(name = "message")
            public String message = "";
        }

        @XmlTransient
        public String ns = null;
        @XmlTransient
        public String nsLibs = null;
        @XmlAttribute
        public String id = "";
        @XmlAttribute
        public String group = "";
        @XmlAttribute
        public String ignore = "false";
        @XmlAttribute
        public String assertId = "";
        @XmlAttribute
        public String depends = "";
        @XmlAttribute
        public String foreach = "";
        @XmlAttribute
        public String eachvar = "";
        @XmlAttribute
        public String eachId = "";
        @XmlAttribute
        public String posts = "";
        @XmlAttribute
        public String returnFlag = "true";
        @XmlAttribute
        public String returnTo = "";
        @XmlAttribute
        public String desc = "";

        @XmlElement
        public String className = "";
        @XmlElement
        public String methodName = "";
        @XmlElement
        public List<Parameter> parameter = new ArrayList<Parameter>();
        @XmlElement
        public Assertion assertion = new Assertion();

        /**
         * <code>Parameter</code> is a parameter object in data binding level.
         * For <code>parameterType</code>, 2 options are below:
         * 
         * <pre>
         * 0 stands for the java plain value(e.g "string value...", 12,12.5D ) or xml formed data(e.g <string>string value</string>, <int>12</int>,<double>12.5</double>), also java code such as 'new String("string value")'.
         * 1 stands for loading data from xml data file, such as 'd:\\data.xml'.
         * </pre>
         * 
         * @author jordan.xue
         */
        public static class Parameter {

            @XmlElement
            public java.lang.String name = "";
            @XmlElement
            public java.lang.String dataType = "";
            @XmlElement
            public java.lang.String parameterType = "";
            @XmlElement
            @XmlJavaTypeAdapter(XmlCDATAAdatper.class)
            public java.lang.String value = "";

            @XmlAttribute
            @Deprecated
            public java.lang.String name_ = "";
            @XmlAttribute
            @Deprecated
            public java.lang.String dataType_ = "";
            @XmlAttribute
            @Deprecated
            public java.lang.String parameterType_ = "";
            @XmlAttribute
            @Deprecated
            public java.lang.String value_ = "";

        }
    }

}
