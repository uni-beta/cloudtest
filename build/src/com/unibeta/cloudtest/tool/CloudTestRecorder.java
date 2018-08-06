package com.unibeta.cloudtest.tool;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

import com.unibeta.cloudtest.config.CloudTestCase;
import com.unibeta.cloudtest.config.CloudTestCase.Case;
import com.unibeta.cloudtest.config.CloudTestCase.Case.Parameter;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Recorder;
import com.unibeta.cloudtest.config.plugin.PluginConfig.SignatureRegex;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.cloudtest.util.ObjectDigester;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.XmlUtils;
import static com.unibeta.cloudtest.tool.Java2TestCases.*;

/**
 * CloudTest recorder is AOP oriented, pointcut into particular aspect that has
 * been defined in aop:config and signature regex matches. Before usage recorder
 * function, please do configure below aop:config to enable. For example,<br>
 * 
 * <pre>
 * &lt;aop:aspectj-autoproxy proxy-target-class="true"/&gt;
 * 
 * &lt;bean name="cloudTestRecorder"
 *      class="com.unibeta.cloudtest.tool.CloudTestRecorder" /&gt;
 *  &lt;aop:config&gt;
 *      &lt;aop:aspect id="cloudtestRecorderAop" ref="cloudTestRecorder"&gt;
 *          &lt;aop:pointcut id="target"
 *              expression="execution(public * com..*.*(..))" /&gt;
 *          &lt;aop:around method="record" pointcut-ref="target" /&gt;
 *      &lt;/aop:aspect&gt;
 * &lt;/aop:config&gt;
 * </pre>
 * 
 * Then, configure case-recorder in PluginConfig.xml <br>
 * 
 * <pre>
 * &lt;case-recorders&gt;
 *      &lt;recorder id = "aaa" poweroff = "false" targetCaseFilePath = "bbb.tc.xml" desc= ""&gt;
 *          &lt;signatureRegex&gt;
 *              &lt;className&gt;.*&lt;/className&gt;
 *              &lt;modifiers&gt;.*&lt;/modifiers&gt;
 *              &lt;methodName&gt;.*&lt;/methodName&gt;
 *          &lt;/signatureRegex&gt;
 *          &lt;signatureRegex&gt;
 *              &lt;className&gt;.*&lt;/className&gt;
 *              &lt;modifiers&gt;.*&lt;/modifiers&gt;
 *              &lt;methodName&gt;.*&lt;/methodName&gt;
 *          &lt;/signatureRegex&gt;
 *      &lt;/recorder&gt;
 * &lt;/case-recorders&gt;
 * </pre>
 * 
 * @author jordan.xue
 */
public class CloudTestRecorder {

    private static Logger log = Logger.getLogger(CloudTestRecorder.class);

    private static boolean stopped = true;
    private static CloudTestCase cloudTestCase = null;

    private static final String COMMENTS = "<!--"
            + BR
            + "$Powered By CloudTest-AutoCaseRecorder engine Automatically On "
            + NEW_JAVA_UTIL_DATE_TO_STRING
            + "$"
            + BR
            + "Visit http://sourceforge.net/projects/cloudtest/ to download the latest version"
            + BR + "" + BR + "Author: " +ConfigurationProxy.getOsUserName() + BR + ""
            // + "msn/email: jordan.xue@hotmail.com" + BR + ""

            + "-->" + BR;

    /**
     * Records the cases if recorder is power-on and started.
     * 
     * @param pjp
     *            ProceedingJoinPoint
     * @return the point-cut method returned value.
     * @throws Throwable
     */
    public Object record(ProceedingJoinPoint pjp) throws Throwable {

        long start = System.currentTimeMillis();
        Object result = pjp.proceed(pjp.getArgs());
        long end = System.currentTimeMillis();

        double timeCost = (end - start) / 1000.00;

        if (!stopped) {
            Recorder recorder = PluginConfigProxy
                    .getSinglePowerOnCaseRecorder();

            if (null != recorder) {
                recordTestCases(pjp, recorder, result, timeCost);
            }
        }

        return result;

    }

    private void recordTestCases(ProceedingJoinPoint pjp, Recorder recorder,
            Object result, double timeCost) {

        boolean isUnmatchedSignature = isUnmatchedSignature(recorder,
                pjp.getSignature());

        if (stopped || isUnmatchedSignature) {
            return;
        }

        File targetFile = null;
        try {
            if (cloudTestCase == null) {
                cloudTestCase = new CloudTestCase();
            }
            targetFile = new File(ConfigurationProxy.getCloudTestRootPath()
                    + File.separator + "TestCase/" + recorder.id
                    + File.separator + recorder.targetCaseFilePath);

            cloudTestCase.assertRuleFile = targetFile.getName()
                    .replace(".tc.xml", ".assert.xml").replaceAll(" +", "_")
                    .replace("-+", "_");

            CloudTestCase.Case testCase = new CloudTestCase.Case();

            String[] beanNamesForType = CloudTestPluginFactory
                    .getSpringBeanFactoryPlugin().getBeanNamesForType(
                            pjp.getSignature().getDeclaringType());

            if (null != beanNamesForType && beanNamesForType.length > 0) {
                testCase.className = beanNamesForType[0];
            } else {
                testCase.className = pjp.getSignature().getDeclaringTypeName();
            }

            testCase.id = "Step" + (cloudTestCase.testCase.size() + 1) + "_"
                    + testCase.className.replace(".", "_") + "_"
                    + pjp.getSignature().getName();
            testCase.assertId = testCase.id;

            testCase.methodName = pjp.getSignature().getName();
            Method method = getMethod(pjp.getSignature().getDeclaringType(),
                    pjp.getSignature().getName(), pjp.getArgs());

            testCase.assertion.timeout = timeCost;
            if (null != method.getReturnType()
                    && void.class != method.getReturnType()) {

                Class returnType = method.getReturnType();
                String resultValue = CloudTestUtils
                        .formatSimpleName(returnType)
                        + CloudTestConstants.ASSERT_RESULT_SUFFIX;

                if (result != null) {
                    if (returnType.isPrimitive()) {
                        testCase.assertion.assert_ = resultValue + " == "
                                + result;
                    } else if (Boolean.class.isAssignableFrom(returnType)
                            || boolean.class.isAssignableFrom(returnType)) {
                        testCase.assertion.assert_ = resultValue + " == "
                                + result;
                    } else {
                        testCase.assertion.assert_ = resultValue + " != null";
                    }

                } else {
                    testCase.assertion.assert_ = resultValue + " == null";
                }

                /*
                 * if (!returnType.isPrimitive()) { testCase.assertion.assert_ =
                 * resultValue + " != null"; } else { if
                 * (Boolean.class.isAssignableFrom(returnType) ||
                 * boolean.class.isAssignableFrom(returnType)) {
                 * testCase.assertion.assert_ = resultValue + " == true"; } else
                 * { testCase.assertion.assert_ = resultValue + " != 0"; } }
                 */
                testCase.assertId = testCase.id;
                testCase.assertion.message = testCase.assertId
                        + " asserted failure.\\n\\nReturned result is:\\n#{"
//                        + ObjectDigester.class.getCanonicalName() + ".toXML("
//                        + resultValue + ")}\\n\\n@";
                        + resultValue + "}\\n\\n@";

                testCase.returnTo = (recorder.id + "_" + recorder.targetCaseFilePath)
                        .toUpperCase().replace(".TC.XML", "").replace("/", "_")
                        .replace("\\", "_").replace(".", "_")
                        + "_"
                        + method.getReturnType().getSimpleName()
                        + "Result";
            }

            testCase.parameter = Java2TestCases.buildParamaters(method, false);

            String removeDuplicated = PluginConfigProxy
                    .getParamValueByName(CloudTestConstants.CLOUDTEST_RECORDER_CASE_REMOVE_DUPLICATED_ENABLE);

            boolean isDuplicatedCases = resolveDuplicatedCases(testCase);
            if ("true".equalsIgnoreCase(removeDuplicated) && isDuplicatedCases) {
                return;
            }

            int i = 0;
            for (Parameter p : testCase.parameter) {
                String dataXml = ObjectDigester.toXML(pjp.getArgs()[i]);

                if ("0".equals(p.parameterType)) {
                    p.value = dataXml;
                } else {
                    p.value = "TestData/" + recorder.id + "/"
                            + p.dataType.replace(".", "/") + ".xml";
                    saveToXml(dataXml,
                            ConfigurationProxy.getCloudTestRootPath()
                                    + File.separator + p.value);
                }

                i++;
            }

            cloudTestCase.testCase.add(testCase);

            // save result to xml
            // saveToXml(
            // ObjectDigester.toXML(result),
            // ConfigurationProxy.getCloudTestRootPath()
            // + File.separator
            // + "Returns/"
            // + recorder.id
            // + File.separator
            // + recorder.targetCaseFilePath + File.separator
            // + testCase.id + "@" + testCase.returnTo + ".xml");

            saveToXml(ObjectDigester.toXML(result),
                    targetFile.getAbsolutePath() + "@Returns" + File.separator
                            + testCase.id + "@" + testCase.returnTo + "_in_"
                            + timeCost + "s.xml");

            if (null != targetFile) {
                try {
                    saveToXml(ObjectSerializer.marshalToXml(cloudTestCase),
                            targetFile.getAbsolutePath());

                } catch (Exception e) {
                    log.error("save " + targetFile.getAbsolutePath()
                            + " failed,", e);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

        }

    }

    private boolean resolveDuplicatedCases(Case testCase) throws Exception {

        if (cloudTestCase == null) {
            cloudTestCase = new CloudTestCase();
        }

        boolean isDuplicated = false;
        String ignoreDuplicated = PluginConfigProxy
                .getParamValueByName(CloudTestConstants.CLOUDTEST_RECORDER_CASE_IGNORE_DUPLICATED_ENABLE);
        String removeDuplicated = PluginConfigProxy
                .getParamValueByName(CloudTestConstants.CLOUDTEST_RECORDER_CASE_REMOVE_DUPLICATED_ENABLE);

        for (Case c : cloudTestCase.testCase) {
            if (c.className.equals(testCase.className)
                    && c.methodName.equals(testCase.methodName)) {

                if (c.parameter.size() != testCase.parameter.size()) {
                    continue;
                } else {
                    int i = c.parameter.size();
                    for (int j = 0; j < i; j++) {
                        if (c.parameter.get(j).dataType
                                .equals(testCase.parameter.get(j).dataType)) {

                            if ("true".equalsIgnoreCase(ignoreDuplicated)
                                    && !"true"
                                            .equalsIgnoreCase(removeDuplicated)) {
                                c.ignore = "true";
                            }

                            isDuplicated = true;
                        }
                    }
                }
            }
        }

        return isDuplicated;
    }

    private boolean isUnmatchedSignature(Recorder recorder, Signature signature) {

        if (recorder.signatureRegex == null
                || recorder.signatureRegex.size() == 0) {
            return false;
        }

        for (SignatureRegex sin : recorder.signatureRegex) {

            Pattern regrexClass = Pattern
                    .compile(sin.className, Pattern.DOTALL);
            Pattern regrexMethodName = Pattern.compile(sin.methodName,
                    Pattern.DOTALL);

            if (regrexClass.matcher(signature.getDeclaringTypeName()).matches()
                    && regrexMethodName.matcher(signature.getName()).matches()
                    && isModifierMatched(sin.modifiers,
                            signature.getModifiers())) {
                return false;
            }

        }

        return true;
    }

    private boolean isModifierMatched(String expectedModifier, int givenModifier) {

        if (String.valueOf(Java2TestCases.MODIFIER_PUBLIC).equals(
                expectedModifier)) {
            return Modifier.isPublic(givenModifier);
        } else if (String.valueOf(Java2TestCases.MODIFIER_PROTECTED).equals(
                expectedModifier)) {
            return Modifier.isPublic(givenModifier)
                    || Modifier.isProtected(givenModifier);
        } else {
            return true;
        }
    }

    private Method getMethod(Class declaringType, String methodName,
            Object[] args) {

        if (null == declaringType) {
            return null;
        }

        Method[] methods = declaringType.getDeclaredMethods();

        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                if (null != args && args.length > 0
                        && args.length == m.getParameterTypes().length) {
                    int i = 0;

                    for (Class clz : m.getParameterTypes()) {
                        if (null != args[i] && null != clz
                                && clz.isAssignableFrom(args[i].getClass())) {
                            // matched, check next
                        } else {
                            continue;
                        }

                        i++;
                    }

                    return m;

                } else if (m.getParameterTypes() == null
                        || m.getParameterTypes().length == 0) {
                    return m;
                }
            }
        }

        return null;
    }

    private static void saveToXml(String xml, String fileName) throws Exception {

        File file = new File(fileName);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        StringBuffer sb = new StringBuffer();

        sb.append(
                COMMENTS.replace(NEW_JAVA_UTIL_DATE_TO_STRING,
                        new java.util.Date().toString())).append(xml);
        XmlUtils.prettyPrint(StringEscapeUtils.unescapeXml(sb.toString()),
                file.getAbsolutePath());
    }

    /**
     * Power-on recorder
     * 
     * @return true if started success
     */
    public static boolean start() {

        stopped = false;
        cloudTestCase = null;

        return !stopped;
    }

    /**
     * Power-off recorder
     * 
     * @return true if stopped success
     */
    public static boolean stop() {

        stopped = true;
        cloudTestCase = null;

        return stopped;

    }

    /**
     * Gets recorder power status
     * 
     * @return true - poweron false - poweroff
     */
    public static boolean getStatus() {

        return !stopped;

    }

}
