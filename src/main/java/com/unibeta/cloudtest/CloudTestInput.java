package com.unibeta.cloudtest;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.unibeta.cloudtest.config.bind.XmlCDATAAdatper;
import com.unibeta.cloudtest.util.CloudTestUtils;

/**
 * <code>CloudTestInput</code> a single test request object, including
 * className, methodName and parameters.
 * 
 * @author jordan.xue
 */
public class CloudTestInput implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String className;
    private String methodName;
    private List<CloudTestParameter> parameter;

    public List<CloudTestParameter> getParameter() {

        return parameter;
    }

    public void setParameter(List<CloudTestParameter> parameter) {

        this.parameter = parameter;
    }

    public String getClassName() {

        return CloudTestUtils.trimString(className);
    }

    public void setClassName(String className) {

        this.className = className;
    }

    public String getMethodName() {

        return CloudTestUtils.trimString(methodName);
    }

    public void setMethodName(String methodName) {

        this.methodName = methodName;
    }

    /**
     * <code>CloudTestParameter</code> is a common parameter object for method
     * invoking.
     * 
     * @author jordan.xue
     */
    public static class CloudTestParameter implements Serializable {

        private static final long serialVersionUID = 1L;

        /*
         * name: Only for indicating parameter name, no functionality level
         * usage.
         */
        private String name;
        private String dataType;
        /*
         * parameterType "1": value is file path ; "0" : value is dataValue
         */
        private String parameterType = "0";

        private String value;

        public String getDataType() {

            return CloudTestUtils.trimString(dataType);
        }

        public void setDataType(String dataType) {

            this.dataType = dataType;
        }

        public String getParameterType() {

            return CloudTestUtils.trimString(parameterType);
        }

        public void setParameterType(String parameterType) {

            this.parameterType = parameterType;
        }

        @XmlJavaTypeAdapter(XmlCDATAAdatper.class)
        public String getValue() {

            return CloudTestUtils.trimString(value);
        }

        public void setValue(String value) {

            this.value = value;
        }

        public String getName() {

            return CloudTestUtils.trimString(name);
        }

        public void setName(String name) {

            this.name = name;
        }
    }

}
