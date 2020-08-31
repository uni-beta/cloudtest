package com.unibeta.cloudtest.tool;

import static com.unibeta.cloudtest.constant.CloudTestConstants.ARRAY_TYPE_TAG;
import static com.unibeta.cloudtest.constant.CloudTestConstants.ARRAY_YYPE_SYMBOL;
import static com.unibeta.cloudtest.constant.CloudTestConstants.JAVA_BASIC_TYPE_LIST;
import static com.unibeta.cloudtest.constant.CloudTestConstants.JAVA_NUMBERRIC_TYPE_LIST;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.reflect.ObjectReflector;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * Generates mock xml data by Class name.
 */
public class XmlDataDigester {

	// private static final int MOCK_ARRAY_SIZE = generateRandomArraySize();

	// private static final int STACK_OVERFLOW_INDEX_DEPTH = 10000;
	private static final int STACK_OVERFLOW_DEPTH = 5;

	private static final int INDENTING_SIZE = 4;
	private static final String DATE_FORMAT_STYLE = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final int RANDOM_STRING_MAX_LENGTH = 10;
	private static final int NUMBERIC_TYPE_MAX_VALUE = 100;

	private static Logger logger = LoggerFactory.getLogger(XmlDataDigester.class);

	private StringBuffer sb = new StringBuffer();

	private static int generateRandomArraySize() {

		int v = new Random().nextInt(3);

		if (v <= 0) {
			return 1;
		} else {
			return v;
		}
	}

	/**
	 * Digests given className to xml data and save generated xml to fileName.
	 * If fileName is null or invalid, only return xml string.
	 * 
	 * @param className
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public String toXml(String className, String fileName) throws Exception {

		if (null == className || className.trim().length() == 0) {
			return null;
		}

		generateNode(className, true, null);

		if (null != fileName && fileName.length() > 0) {
			File f = new File(fileName);
			String name = f.getName();

			String dir = fileName.substring(0, fileName.lastIndexOf(name));
			File dirsFile = new File(dir + File.separator);

			if (!dirsFile.exists()) {
				dirsFile.mkdirs();
			}

			if (!f.exists()) {
				f.createNewFile();
			}
			XmlUtils.prettyPrint(sb.toString(), fileName);
		}
		return this.format(sb.toString());
	}

	// public static void main(String[] args) throws Exception {
	// String s1 = "string[]";
	// String s2 = "int[]";
	//
	// String xml = new XmlDataDigester().toXml(s1,"");
	// System.out.println(xml);
	//
	// XStream x = new XStream();
	// Object o = x.fromXML(xml);
	// System.out.println(x.toXML(o));
	// }

	private void generateNode(String className, boolean rootNode,
			List<String> objList) throws Exception {

		Class clazz;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			if (rootNode && clazz.isInterface()) {
				List<Class> impls = CloudTestUtils.findImplementations(clazz,
						new String[] { CloudTestUtils
								.getIndexedSearchPackageName(clazz) }, false);

				if (null != impls && impls.size() > 0) {
					className = impls.get(0).getCanonicalName()
							.replace(ARRAY_YYPE_SYMBOL, ARRAY_TYPE_TAG);
				}
			}
		} catch (Exception e) {
			clazz = CloudTestUtils.evalDataType(className);
		}

		String dataTypeName = null;

		if (clazz.isMemberClass()) {
			dataTypeName = CloudTestUtils.formatMemberClassCanonicalName(clazz);
		} else {
			dataTypeName = className.replace(ARRAY_YYPE_SYMBOL, ARRAY_TYPE_TAG);
		}

		if (null == objList) {
			objList = new ArrayList<String>();
		}
		if (!JAVA_BASIC_TYPE_LIST.contains(className)) {

			if (objList.toString().getBytes().length >= (1024 * 1024)
					- className.getBytes().length) {
				logger.warn("StackOverflowError might to be occurred due to huge Xml Data to be generated. '"
						+ className + "' will be ignored.");

				return;
			}

			objList.add(className);

			if (hasStackOverflowErrorRisk(objList)) {
				return;
			}
		}

		if (rootNode) {
			startNode(dataTypeName);

			if (clazz.isArray()) {
				// sb.append("<" + arrayTypeName + ">");
				if (dataTypeName.endsWith(ARRAY_TYPE_TAG)) {
					int i = dataTypeName.lastIndexOf(ARRAY_TYPE_TAG);

					for (int j = 0; j < generateRandomArraySize(); j++) {
						this.generateNode(dataTypeName.substring(0, i), true,
								objList);
					}

				} else {
					for (int i = 0; i < generateRandomArraySize(); i++) {
						this.generateNode(dataTypeName, true, objList);
					}
				}
			}

		}

		if (!JAVA_BASIC_TYPE_LIST.contains(clazz.getCanonicalName())) {
			// Field[] fields = ObjectReflector.getAllJavaBeanFields(clazz);
			Method[] methods = getBeanMethods(clazz);
			for (Method m : methods) {
				String name = null;

				if (m.getReturnType().isMemberClass()) {
					name = CloudTestUtils.formatMemberClassCanonicalName(m
							.getReturnType());
				} else {
					name = m.getReturnType().getCanonicalName();
				}
				if (!m.getReturnType().isPrimitive()) {
					if (m.getReturnType().isArray()) {
						if (this.isNewNodeForCurrentObject(objList, name)) {
							this.startNode(this.getAttributeName(m));

							if (name.length() > 2) {
								// objList.add(n);
								String ns = name
										.substring(0, name.length() - 2);

								for (int i = 0; i < generateRandomArraySize(); i++) {
									generateNode(ns, true, objList);
								}
							}

							this.endNode(this.getAttributeName(m));
						} else {
							this.addNode(m);
						}
					} else if (m.getReturnType().isAssignableFrom(
							Collection.class)
							|| m.getReturnType().isAssignableFrom(List.class)) {

						Map<String, Type[]> types = resolveGenericTypeOfMethod(
								clazz, m);

						this.startNode(this.getAttributeName(m));
						if (null != types && null != types.values()) {
							for (Type[] t : types.values()) {
								if (types.size() > 0) {
									Class cc = ObjectReflector
											.eraseAsClass(t[0]);
									if (cc == null) {
										continue;
									}

									for (int i = 0; i < generateRandomArraySize(); i++) {
										if (!cc.isMemberClass()) {
											generateNode(cc.getCanonicalName(),
													true, objList);
										} else {
											String replace = cc
													.getCanonicalName()
													.replace(
															"."
																	+ cc.getSimpleName(),
															"$"
																	+ cc.getSimpleName());
											generateNode(replace, true, objList);
										}
									}
								}
							}
						}
						this.endNode(this.getAttributeName(m));
					} else {
						{
							this.startNode(this.getAttributeName(m));
							if (this.isNewNodeForCurrentObject(objList, m
									.getReturnType().getCanonicalName())) {
								// objList.add(m.getReturnType()
								// .getCanonicalName());
								generateNode(name, false, objList);

							}
							this.endNode(this.getAttributeName(m));
						}
					}

				} else if (!m.getReturnType()
						.isAssignableFrom(Collection.class)) {
					addNode(m);
				}
			}
		} else {
			addValue(dataTypeName);

		}

		if (rootNode) {

			if (JAVA_BASIC_TYPE_LIST.contains(dataTypeName)
					|| "string".equals(dataTypeName)) {
				addValue(dataTypeName);
			}

			endNode(dataTypeName);
		}
	}

	private Map<String, Type[]> resolveGenericTypeOfMethod(Class clazz, Method m)
			throws Exception {
		Map<String, Type[]> types = null;
		String reflectAttributeNameByMethodName = null;

		try {
			reflectAttributeNameByMethodName = ObjectReflector
					.reflectAttributeNameByMethodName(m.getName());
			types = ObjectReflector.getGenericTypeOfField(clazz,
					reflectAttributeNameByMethodName);
		} catch (NoSuchFieldException e) {

			types = resolveGenericTypeOfMethod(clazz.getSuperclass(), m);
		} catch (Exception e1) {
			logger.warn("Can not resolve GenericType Element["
					+ reflectAttributeNameByMethodName + "] from Class["
					+ clazz + "], will be ignored.", e1);

		}

		return types;
	}

	private boolean hasStackOverflowErrorRisk(List nameList) {

		if (nameList == null || nameList.size() == 0) {
			return false;
		}

		int length = nameList.size() - 1;

		for (int i = 1; i <= length; i++) {
			int n = 0;
			for (int j = 0; j < length; j++) {

				if ((length - j) >= 0 && (length - j - i) >= 0) {

					Object obj1 = nameList.get(length - j - i);
					Object obj2 = nameList.get(length - j);

					if (!JAVA_BASIC_TYPE_LIST.contains(obj2)
							&& obj2.equals(obj1)) {
						n++;
						if (n >= STACK_OVERFLOW_DEPTH) {
							return true;
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}

		}

		return false;
	}

	private Method[] getBeanMethods(Class clazz) {

		if (null == clazz) {
			return new Method[0];
		}

		List<Method> list = new ArrayList<Method>();
		Map<String, Method> map = new HashMap<String, Method>();

		// Method[] ms = clazz.getDeclaredMethods();
		Method[] ms = clazz.getMethods();

		for (Method m : ms) {
			if (m.getName().startsWith("get")) {
				map.put("set" + m.getName().substring(3), m);
			}
			if (m.getName().startsWith("is")) {
				map.put("set" + m.getName().substring(2), m);
			}
		}

		for (Method m : ms) {
			if (m.getName().startsWith("set") && m.getParameterTypes() != null
					&& m.getParameterTypes().length == 1) {
				if (null != map.get(m.getName())
						&& !list.contains(map.get(m.getName()))) {
					list.add(map.get(m.getName()));
				}
			}
		}

		return list.toArray(new Method[0]);
	}

	private boolean isNewNodeForCurrentObject(List<String> objList, String name) {

		return true;
		// if (JAVA_BASIC_TYPE_LIST.contains(name)) {
		// return true;
		// }

		// if (objList.contains(name)) {
		// return false;
		// } else {
		// return true;
		// }

		// return true;
	}

	private void addNode(Method m) throws Exception {

		startNode(this.getAttributeName(m));

		addValue(m.getReturnType());

		endNode(getAttributeName(m));
	}

	private void addValue(Class<?> type) throws Exception {

		Random random = new Random();

		if (isNumberic(type.getCanonicalName())) {

			int i = random.nextInt(NUMBERIC_TYPE_MAX_VALUE);
			sb.append("" + i);
		} else if (type.isAssignableFrom(Date.class)
				|| type.isAssignableFrom(java.sql.Date.class)
				|| type.isAssignableFrom(Timestamp.class)) {

			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_STYLE);

			String date = format.format(new Date()) + " CST";
			sb.append(date);
		} else if (type.isAssignableFrom(Collection.class)
				|| type.isAssignableFrom(Map.class)
				|| type.isAssignableFrom(List.class)) {
			// for List or Map, set default value is null

		} else if (type.isAssignableFrom(boolean.class)
				|| type.isAssignableFrom(Boolean.class)) {
			sb.append("false");
		} else if (type.isAssignableFrom(char.class)
				|| type.isAssignableFrom(Character.class)) {
			String str = generateRandomString(1);
			sb.append(str);

		} else {
			String str = generateRandomString(random
					.nextInt(RANDOM_STRING_MAX_LENGTH + 1));
			sb.append(str);
		}
	}

	private void addValue(String clazz) throws Exception {

		Class<?> type = null;
		try {
			type = Thread.currentThread().getContextClassLoader().loadClass(clazz);
		} catch (ClassNotFoundException e) {
			type = CloudTestUtils.evalDataType(clazz);
		}

		this.addValue(type);
	}

	private String getAttributeName(Method m) {

		String name = m.getName();
		if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else if (name.startsWith("is")) {
			name = name.substring(2);
		}

		name = name.substring(0, 1).toLowerCase() + name.substring(1);

		return name;
	}

	private String generateRandomString(int length) {

		StringBuffer sb = new StringBuffer();

		Random random = new Random();
		int c = 97;

		for (int i = 0; i < length; i++) {
			int r = random.nextInt(26);
			sb.append((char) (r + c));
		}

		if (sb.toString().trim().length() == 0) {
			return "random";
		} else {
			return sb.toString();
		}
	}

	private boolean isNumberic(String name) {

		if (JAVA_NUMBERRIC_TYPE_LIST.contains(name)) {
			return true;
		}
		return false;
	}

	private void startNode(String className) {

		sb.append("<" + className + ">");
	}

	private void endNode(String className) {

		sb.append("</" + className + ">");
	}

	/**
	 * Format XML String to well-formed.
	 * 
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public String format(String str) throws Exception {

		if (null == str) {
			return null;
		}
		if ("".equals(str.trim())) {
			return "";
		}
		if ("<null/>".equalsIgnoreCase(str.trim())) {
			return "<null/>";
		}

		SAXReader reader = new SAXReader();
		StringReader in = new StringReader(str);

		Document doc = reader.read(in);
		OutputFormat formater = OutputFormat.createPrettyPrint();
		formater.setIndent(true);
		formater.setIndentSize(INDENTING_SIZE);
		// formater=OutputFormat.createCompactFormat();

		formater.setEncoding("UTF-8");

		StringWriter out = new StringWriter();
		XMLWriter writer = new XMLWriter(out, formater);

		writer.write(doc);
		writer.close();

		return out.toString();

	}

}
