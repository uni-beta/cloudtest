package com.unibeta.cloudtest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.unibeta.cloudtest.config.CacheManagerFactory;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.impl.CloudTestClassLoader;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfig;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.cloudtest.tool.Java2TestCases;
import com.unibeta.vrules.engines.dccimpls.DynamicCompiler;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

import bsh.EvalError;
import bsh.Interpreter;
import groovy.lang.GroovyShell;

/**
 * Java object marshal/unmarshal tool between object and xml.
 * 
 * @author jordan.xue
 */
public class ObjectDigester {

	private static final String JSON_ENGINE_FASTJSON = "fastjson";
	private static final String JSON_ENGINE_JACKSON = "jackson";
	
	protected static ObjectDigester this_ = new ObjectDigester();
	private static XStream x = new XStream(new DomDriver("UTF-8"));
	private static ThreadLocal<Object> bshThreadLocal = new ThreadLocal<Object>();
	private static ThreadLocal<Object> groovyThreadLocal = new ThreadLocal<Object>();
	private static ThreadLocal<Map> runtimeDataThreadLocal = new ThreadLocal<Map>();

	private static Object getBeanSheelEngine() {

		Interpreter v = (Interpreter) bshThreadLocal.get();
		if (v == null) {
			v = new Interpreter();
			bshThreadLocal.set(v);
		}

		return v;
	}

	private static Object getGroovyEngine() {

		GroovyShell v = (GroovyShell) groovyThreadLocal.get();
		if (v == null) {
			v = new GroovyShell();
			groovyThreadLocal.set(v);
		}

		return v;
	}

	/**
	 * Convert xml file to object instance
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static Object fromXMLFile(String fileName) throws Exception {

		String path = fileName;
		File f = new File(fileName);

		if (f.isAbsolute()) {
			path = fileName;
		} else {
			path = ConfigurationProxy.getCloudTestRootPath() + fileName;
		}

		return x.fromXML(XmlUtils.paserDocumentToString(XmlUtils.getDocumentByFileName(path)));
	}

	private static String jsonEngine = null;
	private static Object jackson = null;

	private static String getJsonEngine() throws Exception {
		if (null != jsonEngine) {
			return jsonEngine;
		} else {
			try {
				Class.forName("com.alibaba.fastjson.JSON");
				jsonEngine = JSON_ENGINE_FASTJSON;
			} catch (ClassNotFoundException e) {
				try {
					Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
					jsonEngine = JSON_ENGINE_JACKSON;
					jackson = new ObjectMapper();
				} catch (ClassNotFoundException e1) {
					throw new Exception("either fastjson or jackson is requested as runtime json engine");
				}
			}

			return jsonEngine;
		}
	}

	public static Object fromJson(String json, Class type) throws Exception {

		String jsonEngine2 = getJsonEngine();

		if (JSON_ENGINE_FASTJSON.equalsIgnoreCase(jsonEngine2)) {
			return JSON.parseObject(json, type);
		} else {
			ObjectMapper objectMapper = (ObjectMapper) jackson;
			return objectMapper.readValue(json, type);
		}

	}

	public static Object fromJsonFile(String jsonFile, Class type) throws Exception {

		String jsonEngine2 = getJsonEngine();

		if (CommonUtils.isNullOrEmpty(jsonFile)) {
			return null;
		}

		String fullPath = null;
		File f = new File(jsonFile);

		if (f.isAbsolute()) {
			fullPath = jsonFile;
		} else {
			fullPath = ConfigurationProxy.getCloudTestRootPath() + jsonFile;
		}

		String json = CloudTestUtils.readFileContent(fullPath);

		if (JSON_ENGINE_FASTJSON.equalsIgnoreCase(jsonEngine2)) {
			return JSON.parseObject(json, type);
		} else {
			ObjectMapper objectMapper =  (ObjectMapper) jackson;
			return objectMapper.readValue(json, type);
		}
	}

	public static String toJson(Object obj) throws Exception {

		String jsonEngine2 = getJsonEngine();

		if (JSON_ENGINE_FASTJSON.equalsIgnoreCase(jsonEngine2)) {
			return JSON.toJSONString(obj);
		} else {
			ObjectMapper objectMapper = (ObjectMapper) jackson;
			return objectMapper.writeValueAsString(obj);
		}

	}

	public static void toJsonFile(Object obj,String jsonFile) throws Exception {

		String jsonEngine2 = getJsonEngine();

		FileOutputStream fos = new FileOutputStream(new File(jsonFile));
		
		if (JSON_ENGINE_FASTJSON.equalsIgnoreCase(jsonEngine2)) {
			JSON.writeJSONString(fos, obj);
		} else {
			ObjectMapper objectMapper = (ObjectMapper) jackson;
			objectMapper.writeValue(fos, obj);
		}
		
		fos.close();

	}
	
	/**
	 * Convert xml string to object instance
	 * 
	 * @param xml
	 * @return
	 */
	public static Object fromXML(String xml) {

		while (null != xml && xml.startsWith(CloudTestConstants.CDATA_START)
				&& xml.endsWith(CloudTestConstants.CDATA_END)) {
			xml = xml.substring(xml.indexOf(CloudTestConstants.CDATA_START) + CloudTestConstants.CDATA_START.length(),
					xml.lastIndexOf(CloudTestConstants.CDATA_END));
		}

		if (null == xml) {
			return null;
		} else {
			return x.fromXML(xml);
		}
	}

	/**
	 * Convert object to xml string
	 * 
	 * @param obj
	 * @return
	 */
	public static String toXML(Object obj) {

		return x.toXML(obj);
	}

	/**
	 * Convert object to xml string and save to target file.
	 * 
	 * @param obj
	 * @param filePath
	 * @throws Exception
	 */
	public static void toXMLFile(Object obj, String filePath) throws Exception {
		XmlUtils.prettyPrint(x.toXML(obj), filePath);
	}

	/**
	 * Create object from java source code in string type. Using BeanShell engine by
	 * default.
	 * 
	 * @param src
	 *            java src code in string
	 * @return
	 * @throws Exception
	 */
	public static Object fromJava(String src) throws Exception {

		String scriptEngine = PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_SCRIPT_ENGINE);

		if (CommonUtils.isNullOrEmpty(scriptEngine)) {
			scriptEngine = CloudTestConstants.CLOUDTEST_SCRIPT_ENGINE_BEANSHELL;
		}

		if (CloudTestConstants.CLOUDTEST_SCRIPT_ENGINE_GROOVY.equalsIgnoreCase(scriptEngine)) {
			return runGroovyShell(src);
		} else if (CloudTestConstants.CLOUDTEST_SCRIPT_ENGINE_BEANSHELL.equalsIgnoreCase(scriptEngine)) {
			return runBeanShell(src);
		} else {
			throw new Exception(scriptEngine
					+ " script engine is not supported in current version, only groovy or beanshell are supported.");
		}

	}

	/**
	 * Force using groovy script engine to evaluate given script text, only in
	 * cloudtest runtime context. <br>
	 * All other data are not visible except cloudtest runtime context data.
	 * 
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public static Object evl(String str) throws Exception {

		return runGroovyShell(str);
	}

	/**
	 * Force using groovy script engine to evaluate given script file, only in
	 * cloudtest runtime context. <br>
	 * All other data are not visible except cloudtest runtime context data.
	 *
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public static Object evlFile(String fileName) throws Exception {

		String text = null;

		File file = new File(fileName);

		String fullPath = null;
		if (!file.isAbsolute()) {
			fullPath = ConfigurationProxy.getCloudTestRootPath() + File.separator + fileName;
		} else {
			fullPath = fileName;
		}

		text = CloudTestUtils.readFileContent(fullPath);

		return runGroovyShell(text);
	}

	private static Object runGroovyShell(String src) throws Exception {

		try {
			Class.forName("groovy.lang.GroovyShell");
		} catch (ClassNotFoundException e) {
			throw new Exception(
					"groovy.lang.GroovyShell class was not found error. Below java statement need 'groovy.lang.GroovyShell' to interprete: \n"
							+ src + "\nPlease configurate groovy.jar to classpath.",
					e);
		}

		GroovyShell bsh = (GroovyShell) getGroovyEngine();
		for (String k : CacheManagerFactory.getThreadLocalInstance()

				.keySet(CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_RUNTIME_DATA)) {

			bsh.setVariable(k, CacheManagerFactory.getThreadLocalInstance()
					.get(CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_RUNTIME_DATA, k));

		}

		Map<String, Object> vars = setDefaultRuntimeData();

		for (String k : vars.keySet()) {
			bsh.setVariable(k, vars.get(k));
		}

		if (null != CloudTestPluginFactory.getUserTransactionPlugin()) {
			String before = CloudTestPluginFactory.class.getCanonicalName() + ".getUserTransactionPlugin().before();";
			bsh.evaluate(before);
		}

		src = addEvl() + src;
		return bsh.evaluate(src);

	}

	private static Object runBeanShell(String src) throws Exception, EvalError {

		try {
			Class.forName("bsh.Interpreter");
		} catch (ClassNotFoundException e) {
			throw new Exception(
					"bsh.Interpreter class was not found error. Below java statement need 'bsh.Interpreter' to interprete: \n"
							+ src + ", Please configurate bsh.jar to classpath.",
					e);
		}

		Interpreter bsh = (Interpreter) getBeanSheelEngine();
		for (String k : CacheManagerFactory.getThreadLocalInstance()
				.keySet(CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_RUNTIME_DATA)) {

			bsh.set(k, CacheManagerFactory.getThreadLocalInstance()
					.get(CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_RUNTIME_DATA, k));
		}

		Map<String, Object> vars = setDefaultRuntimeData();

		for (String k : vars.keySet()) {
			bsh.set(k, vars.get(k));
		}

		if (null != CloudTestPluginFactory.getUserTransactionPlugin()) {
			String before = CloudTestPluginFactory.class.getCanonicalName() + ".getUserTransactionPlugin().before();";
			bsh.eval(before);
		}

		bsh.eval(addEvl());
		return bsh.eval(src);
	}

	private static String addEvl() {

		return "Object evl(String s){return " + com.unibeta.cloudtest.util.ObjectDigester.class.getCanonicalName()
				+ ".evl(s);}\n" + "Object eval(String s){return "
				+ com.unibeta.cloudtest.util.ObjectDigester.class.getCanonicalName() + ".evl(s);}\n";
	}

	private static Map<String, Object> setDefaultRuntimeData() throws EvalError {

		Map data = runtimeDataThreadLocal.get();

		if (data == null || PluginConfigProxy.isModified()) {
			data = buildRuntimeData();
			runtimeDataThreadLocal.set(data);
		}

		return data;
	}

	private static Map<String, Object> buildRuntimeData() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_GCACHE, CacheManagerFactory.getGlobalCacheInstance());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_CACHE, CacheManagerFactory.getThreadLocalInstance());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_CLOUD_OBJECT, this_);
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_ROOT_PATH, ConfigurationProxy.getCloudTestRootPath());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_JAVA2_TEST_CASES, new Java2TestCases());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_BEANS, CloudTestPluginFactory.getSpringBeanFactoryPlugin());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_BEAN_FACTORY, CloudTestPluginFactory.getSpringBeanFactoryPlugin());
		map.put(CloudTestConstants.CLOUDTEST_SYSTEM_PLUGIN_CONFIG, new PluginConfigProxy());

		try {
			PluginConfig config = PluginConfigProxy.getGlobalPluginConfig();

			if (null != config) {
				for (PluginConfig.Plugin p : config.plugin) {
					if (p.id.startsWith("$") && p.id.endsWith("$")) {
						map.put(p.id, PluginConfigProxy.getPluginObject(p.id));
					}
				} 
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return map;
	}

	/**
	 * Create object from java source file by script-engine.
	 * 
	 * @param javaFile
	 *            - file path
	 * @return Object instance
	 * @throws Exception
	 */
	public static Object fromJavaFile(String javaFile) throws Exception {

		if (CommonUtils.isNullOrEmpty(javaFile)) {
			return null;
		}

		String fullPath = null;
		File f = new File(javaFile);

		if (f.isAbsolute()) {
			fullPath = javaFile;
		} else {
			fullPath = ConfigurationProxy.getCloudTestRootPath() + javaFile;
		}

		String src = CloudTestUtils.readFileContent(fullPath);
		return fromJava(src);

	}

	/**
	 * Create object from java source file by dynamic compiling jave-src file.
	 * 
	 * @param javaFile
	 *            - file path
	 * @return Object instance
	 * @throws Exception
	 */
	public static Object compileJavaFile(String javaFile) throws Exception {

		if (CommonUtils.isNullOrEmpty(javaFile)) {
			return null;
		}

		String fullPath = null;
		File f = new File(javaFile);

		if (f.isAbsolute()) {
			fullPath = javaFile;
		} else {
			fullPath = ConfigurationProxy.getCloudTestRootPath() + javaFile;
		}

		boolean modified = isModified(fullPath);
		String pkg = resolvePackage(fullPath);

		if (modified) {

			String result = DynamicCompiler.compile(fullPath);

			if (null != result) {

				CacheManagerFactory.getGlobalCacheInstance().put(
						CacheManagerFactory.getGlobalCacheInstance().CACHE_TYPE_SRC_FILE, fullPath,
						System.currentTimeMillis());

				throw new Exception(
						"CloudTest dynamic compiling failed, please check the java file.\nThe invalid file is located in ["
								+ fullPath + "], compiling fatal errors were found below:\n" + result);
			}

			CacheManagerFactory.getGlobalCacheInstance().put(
					CacheManagerFactory.getGlobalCacheInstance().CACHE_TYPE_SRC_FILE, fullPath,
					new File(fullPath).lastModified());

		}

		@SuppressWarnings("resource")
		CloudTestClassLoader validationClassLoader = new CloudTestClassLoader(
				CloudTestClassLoader.generateURLs(fullPath), fullPath, pkg);

		if (modified) {
			return validationClassLoader.newInstance();

		} else {
			return validationClassLoader.getInstance();
		}
	}

	private static boolean isModified(String fullPath) {

		File f = new File(fullPath);
		Object o = CacheManagerFactory.getGlobalCacheInstance()
				.get(CacheManagerFactory.getGlobalCacheInstance().CACHE_TYPE_SRC_FILE, fullPath);

		if (null != o && (new Long(f.lastModified()).equals(new Long(o.toString())))) {
			return false;
		} else {
			return true;
		}
	}

	private static String resolvePackage(String fullPath) throws Exception {

		RandomAccessFile f = new RandomAccessFile(fullPath, "r");

		long length = f.length();
		StringBuffer sb = new StringBuffer();

		boolean find = false;
		while (f.getFilePointer() < length) {
			String line = f.readLine();
			// System.out.println(line);
			if (!find && line != null && line.contains("package") && line.trim().startsWith("package")
					&& !line.trim().startsWith("//") && line.trim().endsWith(";")) {
				// line = line.replace("package", "//package");
				// find = true;
				return line.trim().substring(7, line.trim().length() - 1).trim();
			}

			sb.append(line + "\n");
		}
		f.close();

		if (find) {
			File file = new File(fullPath);
			file.delete();

			FileWriter fw = new FileWriter(new File(fullPath));
			fw.write(sb.toString());
			fw.close();
		}

		return "";
	}
}
