package com.unibeta.cloudtest.tool;

import static com.unibeta.cloudtest.constant.CloudTestConstants.ARRAY_TYPE_TAG;
import static com.unibeta.cloudtest.constant.CloudTestConstants.JAVA_BASIC_TYPE_LIST;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import com.unibeta.cloudtest.config.CloudTestCase;
import com.unibeta.cloudtest.config.CloudTestCase.Case;
import com.unibeta.cloudtest.config.CloudTestCase.Case.Parameter;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.elements.SpringBeanFactoryPlugin;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * A tool to digest the class to cloud test case in xml file.
 * 
 * @author jordan.xue
 */
public class Java2TestCases {

    static final String NEW_JAVA_UTIL_DATE_TO_STRING = "new java.util.Date().toString()";
    static final int MODIFIER_PRIVATE = 2;
    static final int MODIFIER_PROTECTED = 1;
    static final int MODIFIER_PUBLIC = 0;

    static final String FOLDER_TEST_DATA = "TestData";
    static final String FOLDER_TEST_CASE = "TestCase";

    // static XStream x = new ObjectDigesterStream(new DomDriver());

    static final String BR = "\r";
    private static final String COMMENTS = "<!--"
            + BR
            + "$Powered By CloudTest-Java2TestCases Digester Automatically On "
            + NEW_JAVA_UTIL_DATE_TO_STRING
            + "$"
            + BR
            + "Visit http://sourceforge.net/projects/cloudtest/ to download the latest version"
            + BR + "" + BR + "Author: " + ConfigurationProxy.getOsUserName()
            + BR + ""
            // + "msn/email: jordan.xue@hotmail.com" + BR + ""

            + "-->" + BR;

    // static Pattern emptyXmlPattern = Pattern.compile("<\\D.*/>");

    /**
     * Digests given className to xml data and save generated xml to fileName.
     * If fileName is null or invalid, only return xml string.
     * 
     * @param className
     *            the class name to be digested
     * @param fileName
     *            the target file name for xml data storage
     * @return
     * @throws Exception
     */
    public static String digestToMockXmlData(String className, String fileName)
            throws Exception {

        return new XmlDataDigester().toXml(className, fileName);
    }

    /**
     * Generate test cases' pay-load xml automatically. The case file would not
     * be overwritten, if the same file name existed already. <br>
     * If className is package name, will find all classes under given package
     * and generate all cases. <br>
     * If className is class name, will generate given class's cases only.
     * 
     * @param className
     *            the class/package name to be digested
     * @param destFileName
     *            the target file name for testcase storage.<br>
     * @param accessLevel
     *            the members' access level. 0: public; 1: protected,public;
     *            2:private,protected,public
     * @return test cases in xml
     * @throws Exception
     */
    public static String digestToTestCases(String className,
            String destFileName, int accessLevel) throws Exception {

        if (CommonUtils.isNullOrEmpty(className)) {
            return "";
        }

        StringBuffer tc = new StringBuffer();
        Set<Class<?>> classes = CloudTestUtils.getClassesFromPackage(className,
                null, true);

        if (null == classes || classes.size() == 0) {
            tc.append(degistOneClass(className, destFileName, accessLevel));
        } else {
            for (Class c : classes) {
                tc.append(degistOneClass(c.getName(), destFileName, accessLevel));
                tc.append("\n");
            }
        }

        return tc.toString();
    }

    private static StringBuffer degistOneClass(String className,
            String fileName, int accessLevel) throws Exception {

        StringBuffer tc = new StringBuffer();
        tc.append(COMMENTS.replace(NEW_JAVA_UTIL_DATE_TO_STRING,
                new java.util.Date().toString()));

        CloudTestCase testCase = new CloudTestCase();
        List<Case> cases = new ArrayList<Case>();

        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            Method[] methods = getMethods(clazz, accessLevel);

            if (methods == null || methods.length == 0) {
                return tc;
            }

            for (Method m : methods) {
                cases.add(buildCase(clazz, m));
            }

            testCase.assertRuleFile = clazz.getSimpleName() + ".assert.xml";
            testCase.testCase = cases;

            tc.append(ObjectSerializer.marshalToXml(testCase));

            String classCaseFileName = File.separator
                    + clazz.getPackage().getName().replace(".", File.separator)
                    + File.separator + clazz.getSimpleName() + ".tc.xml";

            if (null == fileName || fileName.trim().length() == 0) {
                String filePath = FOLDER_TEST_CASE + classCaseFileName;

                fileName = ConfigurationProxy.getCloudTestRootPath()
                        + File.separator + filePath;
            } else {
                File f = new File(fileName);
                if (!f.isAbsolute()) {
                    fileName = ConfigurationProxy.getCloudTestRootPath()
                            + FOLDER_TEST_CASE + File.separator + fileName
                            + classCaseFileName;
                } else if (f.isDirectory()) {
                    fileName = fileName + classCaseFileName;
                }

            }

            generateCaseFile(fileName, tc);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return tc;
    }

    private static void generateCaseFile(String fileName, StringBuffer tc)
            throws IOException, Exception {

        {
            fileName = formatTestCaseFileName(fileName);

            File f = new File(fileName);
            String name = f.getName();

            String dir = fileName.substring(0, fileName.lastIndexOf(name));
            File dirsFile = new File(dir + File.separator);

            if (!dirsFile.exists()) {
                dirsFile.mkdirs();
            }
            if (f.exists()) {
                // if file already exist, do not overwrite it. just let it
                // be
            } else {
                f.createNewFile();
                XmlUtils.prettyPrint(
                        StringEscapeUtils.unescapeXml(tc.toString()), fileName);
            }
        }
    }

    private static String formatTestCaseFileName(String fileName) {

        if (!fileName.toLowerCase().endsWith(".tc.xml")
                && !fileName.toLowerCase().endsWith(".ut.xml")) {
            fileName = fileName.substring(0, fileName.toLowerCase()
                    .lastIndexOf("."))
                    + ".tc.xml";
        } else {
            // just let it be
        }
        return fileName;
    }

    private static Method[] getMethods(Class clazz, int modifierLevel) {

        List<Method> methods = new ArrayList<Method>();

        Method[] ms = clazz.getDeclaredMethods();
        for (Method m : ms) {

            switch (modifierLevel) {
            case MODIFIER_PUBLIC: {
                if (Modifier.isPublic(m.getModifiers())) {
                    methods.add(m);
                }
                break;
            }
            case MODIFIER_PROTECTED: {
                if (Modifier.isPublic(m.getModifiers())
                        || Modifier.isProtected(m.getModifiers())) {
                    methods.add(m);
                }
                break;
            }
            // case MODIFIER_PRIVATE: {
            // if (Modifier.isPublic(m.getModifiers())
            // || Modifier.isProtected(m.getModifiers())
            // || Modifier.isPrivate(m.getModifiers())) {
            // methods.add(m);
            // }
            // break;
            // }
            default: {

                methods.add(m);
                break;
            }
            }

        }

        return methods.toArray(new Method[0]);
        // return clazz.getDeclaredMethods();
    }

    private static String getBeanNameForType(Class clazz) {

        SpringBeanFactoryPlugin springBeanFactoryPlugin = CloudTestPluginFactory.getSpringBeanFactoryPlugin();
		String[] names = null;
		
		if (null != springBeanFactoryPlugin ) {
			names = springBeanFactoryPlugin.getBeanNamesForType(clazz);
		}
		if (names != null && names.length > 0) {
            return names[0];
        } else {
            return clazz.getCanonicalName();
        }
    }

    private static Case buildCase(Class clazz, Method m) throws Exception {

        Case c = new Case();

        c.id = clazz.getSimpleName() + "_" + m.getName();
        c.className = getBeanNameForType(clazz);
        c.desc = "";
        c.methodName = m.getName();
        c.returnFlag = "true";
        c.returnTo = "";

        c.parameter = buildParamaters(m, true);

        Class<?> returnType = m.getReturnType();
        String resultValue = CloudTestUtils.formatSimpleName(returnType)
                + CloudTestConstants.ASSERT_RESULT_SUFFIX;

        if (null != returnType
                && !"void".equalsIgnoreCase(returnType.getSimpleName())) {
            if (!returnType.isPrimitive()) {
                c.assertion.assert_ = resultValue + " != null";
            } else {
                if (Boolean.class.isAssignableFrom(returnType)
                        || boolean.class.isAssignableFrom(returnType)) {
                    c.assertion.assert_ = resultValue + " == true";
                } else {
                    c.assertion.assert_ = resultValue + " != 0";
                }
            }

            c.assertId = c.id;
            c.assertion.message = c.assertId
                    + " asserted failure.\\n\\nReturned result is:\\n#{"
                    + resultValue + "}\\n\\n@";
        } else {
            c.assertId = null;
            c.assertion.assert_ = null;
            c.assertion.message = null;
        }

        return c;
    }

    protected static List<Parameter> buildParamaters(Method m,
            boolean needMockData) throws Exception {

        List<Parameter> paras = new ArrayList<Parameter>();
        if (m == null) {
            return paras;
        }

        Class[] types = m.getParameterTypes();

        for (Class t : types) {

            Parameter p = new Parameter();
            p.dataType_ = null;
            p.name_ = null;
            p.parameterType_ = null;
            p.value_ = null;

            if (t.isMemberClass()) {
                p.dataType = t.getCanonicalName().replace(
                        "." + t.getSimpleName(), "$" + t.getSimpleName());
            } else {
                p.dataType = t.getCanonicalName();
            }

            p.name = t.getSimpleName();
            if (!t.isArray()) {
                if (p.dataType.startsWith("java.")
                        || JAVA_BASIC_TYPE_LIST.contains(p.dataType)) {
                    p.parameterType = "0";

                    String xml = digestToMockXmlData(p.dataType, null);
                    if (xml != null && xml.length() > 0
                            && xml.indexOf("?>") > 0) {
                        xml = xml.substring(xml.indexOf("?>") + 2);
                    }
                    // boolean isEmptyXml = emptyXmlPattern.matcher(xml).find();
                    if (JAVA_BASIC_TYPE_LIST.contains(p.dataType)
                            && !Date.class.getCanonicalName()
                                    .equals(p.dataType)
                            && !java.util.Date.class.getCanonicalName().equals(
                                    p.dataType)
                            && !BigDecimal.class.getCanonicalName().equals(
                                    p.dataType)
                            && !StringBuffer.class.getCanonicalName().equals(
                                    p.dataType)) {

                        if (needMockData) {
                            p.value = generatePlainMockValue(xml);
                        }

                    } else {
                        if (needMockData) {
                            p.value = xml.trim();
                        }
                    }
                } else {
                    p.parameterType = "1";
                    String filePath = FOLDER_TEST_DATA + "/"
                            + p.dataType.replace(".", "/") + ".xml";
                    String fileName = ConfigurationProxy.getCloudTestRootPath()
                            + File.separator + filePath;
                    File file = new File(fileName);
                    if (!file.exists() && needMockData) {
                        digestToMockXmlData(p.dataType, fileName);
                    }
                    if (needMockData) {
                        p.value = filePath;
                    }

                }

            } else {

                String type = p.dataType;

                // if (null !=
                // XmlDataDigester.PRIMITIVE_TYPE_MAP.get(p.dataType)) {
                // type = XmlDataDigester.PRIMITIVE_TYPE_MAP.get(p.dataType);
                // } else {
                // type = p.dataType;
                // }
                Class c;
                try {
                    c = Thread.currentThread().getContextClassLoader().loadClass(type);
                } catch (Exception e) {
                    type = type.replace("[]", ARRAY_TYPE_TAG);
                    Object o = ObjectDigester.fromXML("<" + type + ">0</"
                            + type + ">");
                    c = o.getClass();
                }

                p.parameterType = "1";
                String filePath = FOLDER_TEST_DATA
                        + "/"
                        + c.getCanonicalName().replace("[]", ARRAY_TYPE_TAG)
                                .replace(".", "/") + "" + ".xml";

                String fileName = ConfigurationProxy.getCloudTestRootPath()
                        + File.separator + filePath;

                File file = new File(fileName);
                if (!file.exists() && needMockData) {
                    digestToMockXmlData(p.dataType, fileName);
                }
                if (needMockData) {
                    p.value = filePath;
                }
            }

            paras.add(p);
        }

        return paras;
    }

    private static String generatePlainMockValue(String xml) {

        String value = null;
        Object o = ObjectDigester.fromXML(xml);

        if (null != o
                && (Long.class.isAssignableFrom(o.getClass()) || long.class
                        .isAssignableFrom(o.getClass()))) {
            value = o.toString() + "L";
        } else if (null != o
                && (Double.class.isAssignableFrom(o.getClass()) || double.class
                        .isAssignableFrom(o.getClass()))) {
            value = o.toString() + "D";
        } else if (null != o
                && (Float.class.isAssignableFrom(o.getClass()) || float.class
                        .isAssignableFrom(o.getClass()))) {
            value = o.toString() + "F";
        } else if (String.class.isAssignableFrom(o.getClass())) {
            value = "\"" + o.toString() + "\"";
        } else {
            value = o.toString();
        }

        return value;
    }

    public static void main(String[] args) {

        System.out.println(COMMENTS.replace(NEW_JAVA_UTIL_DATE_TO_STRING,
                new java.util.Date().toString()));
        for (int i = 0; i < 1000000000; i++) {

        }
        System.out.println(COMMENTS.replace(NEW_JAVA_UTIL_DATE_TO_STRING,
                new java.util.Date().toString()));
    }
}
