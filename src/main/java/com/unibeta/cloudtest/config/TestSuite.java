package com.unibeta.cloudtest.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "testsuite")
public class TestSuite {

    @XmlAttribute
    public long errors = 0;
    @XmlAttribute
    public long failures = 0;
    @XmlAttribute
    public String hostname ="";
    @XmlAttribute
    public String name = "";
    @XmlAttribute
    public long tests = 0;
    @XmlAttribute
    public double time = 0.0;
    @XmlAttribute
    public String timestamp = "";

    @XmlElement(name = "property")
    @XmlElementWrapper (name = "properties")
    public List<Property> properties = new ArrayList<TestSuite.Property>();
    @XmlElement
    public Error error = new Error();

    public static class Property {

        @XmlAttribute
        public String name = "";
        @XmlAttribute
        public String value = "";
    }

    public static class Error {

        @XmlAttribute
        public String message = "";
        @XmlAttribute
        public String type = "";
        @XmlValue
        public String errorMessage = "";
    }
}
