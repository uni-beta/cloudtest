package com.unibeta.cloudtest.util;

import static com.unibeta.cloudtest.constant.CloudTestConstants.ARRAY_TYPE_TAG;
import static com.unibeta.cloudtest.constant.CloudTestConstants.ARRAY_YYPE_SYMBOL;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.springframework.aop.framework.Advised;

import com.unibeta.cloudtest.CloudTestOutput;
import com.unibeta.cloudtest.CloudTestOutput.ResultStatistics;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;
import com.unibeta.vrules.utils.CommonUtils;

public class CloudTestUtils {

	private static final Project DEFAULT_PROJECT = new Project();
	private static Logger log = Logger.getLogger(CloudTestUtils.class);
	private static Map<String, List<Class>> interfaceMap = new HashMap<String, List<Class>>();

	public static String printExceptionStackTrace(Throwable e) {

		if (null == e) {
			return null;
		}

		StringBuilder sBuilder = new StringBuilder();
		/*
		 * StackTraceElement[] stackTraceElements = null; String errorMessage =
		 * e.getClass().getName(); if (null != e.getMessage()) { errorMessage =
		 * errorMessage + ":" + e.getMessage(); } sBuilder.append(errorMessage); if
		 * (e.getCause() != null && e.getCause().getStackTrace() != null) {
		 * stackTraceElements = e.getCause().getStackTrace(); String error =
		 * "\nCaused by:\n" + e.getCause().toString() + "\n"; sBuilder.append(error);
		 * StringBuffer sb = new StringBuffer(); for (int i = 0; i <
		 * stackTraceElements.length; i++) { sb.append(stackTraceElements[i].toString()
		 * + "\n"); } sBuilder.append(sb.toString().trim()); } stackTraceElements =
		 * e.getStackTrace(); // if (sBuilder.toString().trim().length() > 0) { // //
		 * let it empty // // sBuilder.append("...\n" + stackTraceElements.length // //
		 * + " more exception stack traces:\n"); // } else if (null !=
		 * stackTraceElements) { // if (sBuilder.toString().contains(errorMessage)) { //
		 * sBuilder.append("\n..." + stackTraceElements.length + " more"); // }
		 * StringBuffer sb = new StringBuffer(); for (int i = 0; i <
		 * stackTraceElements.length; i++) { sb.append(stackTraceElements[i].toString()
		 * + "\n"); } if (!sBuilder.toString().contains(sb.toString().trim())) {
		 * sBuilder.append("\nCaused by:\n" + sb.toString().trim()); } }
		 */
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		sBuilder.append(sw.toString());

		return sBuilder.toString();
	}

	public static String trimString(String str) {

		return (null == str ? str : str.trim());
	}

	/**
	 * Check file whether it is valid, if it does not exist, create it
	 * automatically.
	 * 
	 * @param file
	 */
	public static void checkFile(String filePath) {

		File file = new File(filePath);

		if (null != file.getParent()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
		}
	}

	/**
	 * unzip file to local folder. only zip type is supported.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static String unzipFiles(File file) throws Exception {

		String outfile = file.getCanonicalPath();

		if (!outfile.toLowerCase().endsWith(".zip")) {
			throw new Exception("file format is not supported.");
		}

		outfile = outfile.substring(0, (outfile.length() - 4));

		try {
			Class.forName("org.apache.tools.ant.taskdefs.Expand");
			antunzip(file, file.getParentFile());
		} catch (Exception e) {
			log.warn(e.getMessage() + ", try to use local unzip engine.");
			unzip(file);
		}

		return outfile;
	}

	private static void antunzip(File srcFile, File destFile) {
		Expand expand = new Expand();

		expand.setEncoding("UTF-8");
		expand.setProject(DEFAULT_PROJECT);
		expand.setSrc(srcFile);
		expand.setDest(destFile);
		expand.setOverwrite(true);

		expand.execute();

	}

	private static void unzip(File file) throws FileNotFoundException, ZipException, IOException {
		String parentPath = file.getParent() + File.separator;

		FileInputStream fileInputStream = new FileInputStream(file);
		ZipFile zipFile = new ZipFile(file);
		Enumeration e = zipFile.entries();

		while (e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) e.nextElement();

			if (!ze.isDirectory()) {
				InputStream is = zipFile.getInputStream(ze);
				String zipedFile = parentPath + ze.getName();
				checkFile(zipedFile);

				FileOutputStream fileOutputStream = new FileOutputStream(zipedFile);

				byte[] b = new byte[(int) ze.getSize()];

				is.read(b);
				fileOutputStream.write(b);

				is.close();
				fileInputStream.close();
			}
		}

		zipFile.close();
	}

	/**
	 * zip given files to zip file
	 * 
	 * @param inputFile
	 * @return zipped file
	 * @throws Exception
	 */
	public static File zipFiles(File inputFile) throws Exception {

		String name = inputFile.getName();
		int lastIndexOf = name.lastIndexOf(".");

		String zipName = lastIndexOf > 0 ? name.substring(0, lastIndexOf) : name;
		String zipFileName = inputFile.getParent() + "/" + zipName + ".zip";
		File zipFile = new File(zipFileName);

		try {
			Class.forName("org.apache.tools.ant.taskdefs.Zip");
			antzip(inputFile, zipFile);
		} catch (Exception e) {
			log.warn(e.getMessage() + ", try to use local zip engine.");
			zip(inputFile, zipFile);
		}

		return new File(zipFileName);
	}

	private static void antzip(File srcFile, File destFile) {

		Zip zip = new Zip();

		zip.setProject(DEFAULT_PROJECT);
		zip.setDestFile(destFile);
		zip.setEncoding("UTF-8");
		// zip.setBasedir(srcFile);
		zip.setCaseSensitive(true);

		FileSet fs = new FileSet();
		fs.setProject(DEFAULT_PROJECT);
		fs.setDir(srcFile.getParentFile());

		if (srcFile.isDirectory()) {
			fs.setIncludes(srcFile.getName() + "/**");
		} else {
			fs.setIncludes(srcFile.getName());
		}
		// fs.setExcludes("**/*.xml");

		zip.addFileset(fs);
		zip.execute();

	}

	private static void zip(File inputFile, File zipFileName) throws FileNotFoundException, Exception, IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));

		zip(out, inputFile, inputFile.getName());

		out.close();
	}

	static private void zip(ZipOutputStream out, File f, String base) throws Exception {

		if (f.isDirectory()) {
			File[] fl = f.listFiles();
			if (fl.length == 0) {
				out.putNextEntry(new ZipEntry(base + "/"));
			}
			for (int i = 0; i < fl.length; i++) {
				zip(out, fl[i], base + "/" + fl[i].getName());
			}
		} else {
			ZipEntry zipEntry = new ZipEntry(base);
			out.putNextEntry(zipEntry);
			FileInputStream in = new FileInputStream(f);
			BufferedInputStream bi = new BufferedInputStream(in);

			byte[] b = new byte[(int) f.length()];

			bi.read(b);
			out.write(b);

			bi.close();
			in.close();
		}
	}

	/**
	 * Retrieve zipped file content in byte[] and return it.
	 * 
	 * @param zipedFile
	 * @param targetFileName
	 * @return
	 * @throws Exception
	 */
	static public byte[] getZippedFileData(File zipedFile, String targetFileName) throws Exception {

		// String parentPath = zFile.getParent() + File.separator;

		ZipFile zipFile = new ZipFile(zipedFile);
		Enumeration e = zipFile.entries();

		while (e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) e.nextElement();

			if (!ze.isDirectory() && ze.getName().equals(targetFileName)) {

				InputStream is = zipFile.getInputStream(ze);

				// OutputStream outputStream = new ByteArrayOutputStream(
				// (int)ze.getSize());

				byte[] b = new byte[(int) ze.getSize()];

				is.read(b);
				// outputStream.write(b);

				is.close();
				zipFile.close();

				return b;
			}
		}

		zipFile.close();
		return new byte[0];
	}

	/*
	 * public static void main(String[] args) { try { // unzipFiles(new
	 * File("./test\\cloudTest\\TestData\\test.zip")); String s =
	 * transformToHtml("./test/cloudtest/Config/report.xml",
	 * "./test/cloudtest/Config/report.xsl", "./test/reports/default/report.html");
	 * System.out.println(s); } catch (Exception e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } }
	 */

	/*
	 * Find all classes under specified package name.
	 * 
	 * @param packageName package name
	 * 
	 * @param interfaceClass interface name implemented
	 * 
	 * @return Class set that implemented give interface name under given package.
	 * return all classes if interfaceClass is null.
	 */
	public static Set<Class<?>> getClassesFromPackage(String packageName, Class interfaceClass, boolean recursive)
			throws Exception {

		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		String pkg = packageName;
		String packageDirName = pkg.replace('.', '/');
		Enumeration<URL> dirs;

		try {

			dirs = Thread.currentThread().getContextClassLoader().getResources(

					packageDirName);
			while (dirs.hasMoreElements()) {

				URL url = dirs.nextElement();
				String protocol = url.getProtocol();

				if ("file".equals(protocol)) {

					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					classes.addAll(findClassesInPackageByFile(interfaceClass, pkg, filePath, recursive));

				} else if ("jar".equals(protocol)) {

					JarFile jar;

					try {

						jar = ((JarURLConnection) url.openConnection())

								.getJarFile();
						Enumeration<JarEntry> entries = jar.entries();

						while (entries.hasMoreElements()) {
							JarEntry entry = entries.nextElement();
							String name = entry.getName();

							if (name.charAt(0) == '/') {

								name = name.substring(1);

							}

							if (name.startsWith(packageDirName)) {

								int idx = name.lastIndexOf('/');

								if (idx != -1) {

									pkg = name.substring(0, idx)

											.replace('/', '.');

								}

								if ((idx != -1) || recursive) {

									if (name.endsWith(".class")

											&& !entry.isDirectory()) {
										String className = name.substring(pkg.length() + 1, name.length() - 6);
										try {
											if (!CommonUtils.isNullOrEmpty(pkg)) {

												String cn = (pkg + '.') + className;
												log.debug("index class " + cn);

												Class<?> loadClass = Thread.currentThread().getContextClassLoader()
														.loadClass(cn);

												if (interfaceClass == null) {
													classes.add(loadClass);
												} else if (checkImpled(interfaceClass, loadClass)) {
													classes.add(loadClass);
												} else {
													loadClass = null;
												}
											}

										} catch (Exception e) {
											e.printStackTrace();
											continue;

										}

									}

								}

							}

						}

					} catch (IOException e) {
						log.debug(e.getMessage(), e);
					}

				}

			}

		} catch (IOException e) {
			log.debug(e.getMessage(), e);

		}

		return classes;

	}

	private static boolean checkImpled(Class interfaceClass, Class<?> forName) {

		try {
			if (!Modifier.isAbstract(forName.getModifiers()) && !forName.isInterface()
					&& interfaceClass.isAssignableFrom(forName) && !interfaceClass.equals(forName)
					&& forName.getConstructor(null) != null) {
				return true;
			}
		} catch (Exception e) {
			// false
		}

		return false;
	}

	/*
	 * @param packageName
	 * 
	 * @param packagePath
	 * 
	 * @param recursive
	 * 
	 * @param classes
	 */

	private static List<Class<?>> findClassesInPackageByFile(Class interfaceClass, String packageName,
			String packagePath, final boolean recursive) {

		List<Class<?>> classes = new ArrayList<Class<?>>();
		File dir = new File(packagePath);

		if (!dir.exists() || !dir.isDirectory()) {

			return classes;

		}

		File[] dirfiles = dir.listFiles(new FileFilter() {

			public boolean accept(File file) {

				return (recursive && file.isDirectory())

						|| (file.getName().endsWith(".class"));

			}

		});

		for (File file : dirfiles) {
			if (file.isDirectory()) {

				classes.addAll(findClassesInPackageByFile(interfaceClass,
						(CommonUtils.isNullOrEmpty(packageName) ? "" : (packageName + ".")) + file.getName(),
						file.getAbsolutePath(), recursive

				));

			} else {
				String className = file.getName().substring(0,

						file.getName().length() - 6);

				try {

					if (!CommonUtils.isNullOrEmpty(packageName)) {

						String cn = (packageName + '.') + className;

						// Class loadClass = Class.forName(cn);
						Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(cn);

						if (interfaceClass == null) {
							classes.add(loadClass);
						} else if (checkImpled(interfaceClass, loadClass)) {
							classes.add(loadClass);
						} else {
							loadClass = null;
						}
					}

				} catch (ClassNotFoundException e) {
					log.debug(e.getMessage(), e);
				}

			}

		}

		return classes;

	}

	/**
	 * Search all implementations of given interface under pkgName package.
	 * 
	 * @param interfaceClass
	 * @param pkgName
	 * @param searchAll
	 * @return all implementations class if searchAll is true, return the first hit
	 *         implementation for give interface.
	 * @throws Exception
	 */
	public static List<Class> findImplementations(Class interfaceClass, String[] pkgs, boolean searchAll)
			throws Exception {

		if (interfaceMap.get(interfaceClass) != null) {
			return interfaceMap.get(interfaceClass);
		}

		List<Class> classes = new ArrayList<Class>();

		if (!searchAll) {

			Set<Class<?>> underSet = getClassesFromPackage(interfaceClass.getPackage().getName(), interfaceClass, true);
			if (underSet != null && underSet.size() > 0) {
				classes.addAll(underSet);
			}
		}

		if (classes.size() == 0) {

			for (String pkgName : pkgs) {

				Set<Class<?>> set = getClassesFromPackage(pkgName, interfaceClass, true);
				classes.addAll(set);

				if (!searchAll) {
					break;
				}
			}
		}

		if (classes != null && classes.size() > 0) {
			interfaceMap.put(interfaceClass.getCanonicalName(), classes);
		}

		return classes;

	}

	/**
	 * Delete all files under given file name or folder name.
	 * 
	 * @param fileName
	 */
	public static void deleteFiles(String fileName) {

		if (CommonUtils.isNullOrEmpty(fileName)) {
			return;
		}

		File file = new File(fileName);

		if (file.isDirectory()) {
			File[] files = file.listFiles();

			if (null != files) {
				for (File f : files) {
					if (f.isDirectory()) {
						deleteFiles(f.getPath());
						f.delete();
					} else {
						f.delete();
					}
				}
			}
		} else {
			file.delete();
		}

	}

	/**
	 * Transform xml to html by given xsl
	 * 
	 * @param xmlFilePath
	 * @param xslFilePath
	 * @param htmlFilePath
	 */
	public static String transformToHtml(String xmlFilePath, String xslFilePath, String htmlFilePath) {

		String html = "";
		try {
			TransformerFactory tFac = TransformerFactory.newInstance();
			Source xslSource = new StreamSource(xslFilePath);
			Transformer t = tFac.newTransformer(xslSource);

			File xmlFile = new File(xmlFilePath);
			File htmlFile = new File(htmlFilePath);
			Source source = new StreamSource(xmlFile);

			checkFile(htmlFilePath);
			StreamResult result = new StreamResult(htmlFile);
			result.setOutputStream(new ByteArrayOutputStream());

			t.transform(source, result);

			html = result.getOutputStream().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return html;
	}

	/**
	 * Format class simple name
	 * 
	 * @param name
	 * @return
	 */
	public static String formatSimpleName(Class clazz) {

		if (CommonUtils.isNullOrEmpty(clazz)) {
			return null;
		}

		String name = clazz.getSimpleName();

		if (clazz.isPrimitive()) {
			try {
				name = evalDataType(clazz.getCanonicalName()).getSimpleName();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
		}

		return name.replace("[]", "Array");
	}

	/**
	 * Evaluate the data type of input class canonical name.
	 * 
	 * @param className
	 * @return
	 * @throws Exception
	 */
	public static Class evalDataType(String className) throws Exception {

		if (CommonUtils.isNullOrEmpty(className)) {
			className = Object.class.getName();
		}

		if (className.equals(void.class.getName())) {
			return void.class;
		}

		Class clazz;
		String xml = className.replace("[]", CloudTestConstants.ARRAY_TYPE_TAG);
		Object o = null;

		try {
			clazz = Class.forName(className);
			return clazz;
		} catch (ClassNotFoundException e) {
			log.debug(e.getMessage());
			// XStream x = new XStream(new DomDriver());

			try {
				o = ObjectDigester.fromXML("<" + xml + ">0</" + xml + ">");
			} catch (Exception e1) {
				log.debug(e1.getMessage());
				try {
					o = ObjectDigester.fromXML("<" + xml + "/>");
				} catch (Exception e2) {
					log.debug(e2.getMessage());
					throw new Exception(e.getMessage() + "," + e1.getMessage(), e2);
				}
			}

			clazz = o.getClass();
			return clazz;
		}

	}

	/**
	 * Gets interface impls searching index. e.g, if the package name is
	 * "com.abc.foo.pkg", index value is 2, the searching package will be "com.abc".
	 * if index value is 3, the searching package will be "com".
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getIndexedSearchPackageName(Class clazz) {

		int level = 0;

		try {
			String value = PluginConfigProxy
					.getParamValueByName(CloudTestConstants.CLOUDTEST_INTERFACE_IMPLS_SEARCHING_INDEX);

			if (!CommonUtils.isNullOrEmpty(value)) {
				level = Integer.valueOf(value);
			}

		} catch (Exception e) {
			// empty
		}

		String pkg = clazz.getPackage().getName();

		for (int i = 0; i < level; i++) {

			int index = pkg.lastIndexOf(".");
			if (index > 0) {
				pkg = pkg.substring(0, index);
			}
		}

		return pkg;
	}

	/**
	 * Gets target Class via bean id, even is proxied by dynamic AOP.
	 * 
	 * @param beanId
	 * @return Class
	 */

	public static Class getProxiedTargetClass(String beanId) {

		Class clazz = null;

		Object obj = null;

		try {

			obj = CloudTestPluginFactory.getSpringBeanFactoryPlugin().getBean(beanId);

		} catch (Exception e1) {

			// let it empty

		}

		if (obj != null) {
			try {
				Class.forName("org.springframework.aop.framework.Advised");

				if (Advised.class.isAssignableFrom(obj.getClass())) {
					Advised advised = (Advised) obj;
					clazz = advised.getTargetSource().getTargetClass();
				} else {
					clazz = obj.getClass();

				}
			} catch (ClassNotFoundException e) {
				clazz = obj.getClass();
			}

		}

		return clazz;

	}

	/**
	 * read given pathName resources to byte[] map from classpath, including jar or
	 * zip files. <br>
	 * if pathName is a abstract file name, will read this file to byte and return.
	 * <br>
	 * if path name is folder name, will read all files under this folder and return
	 * byte[] map.
	 * 
	 * @param pathName
	 * @return Map<String,byte[]><br>
	 *         String: file name <br>
	 *         byte[]: file content in byte[]
	 * @throws Exception
	 */
	public static Map<String, byte[]> readFilesToBytes(String pathName) throws Exception {

		Map<String, byte[]> map = new HashMap<String, byte[]>();
		URL resource = null;

		if (null != CloudTestUtils.class.getClass().getClassLoader()) {
			resource = CloudTestUtils.class.getClass().getClassLoader().getResource(pathName);
		}

		String path = pathName;

		if (null != resource) {
			path = resource.getPath();
		} else {
			path = pathName;
		}

		String paths[] = path.split("!");

		if (paths.length == 2) {
			String zipFileName = paths[0].substring(6);
			String entry = paths[1].substring(1);

			map.putAll(readZipFilesToBytes(zipFileName, pathName));
		} else {
			Map<String, byte[]> readLocalFilesToBytes = readLocalFilesToBytes(new File(path));
			Set<String> set = readLocalFilesToBytes.keySet();
			int i = path.indexOf(pathName);

			for (String s : set) {
				String key = i > 0 ? s.substring(i - 1) : s;
				map.put(key, readLocalFilesToBytes.get(s));
			}
		}

		return map;
	}

	static private Map<String, byte[]> readLocalFilesToBytes(File f) throws IOException {

		Map<String, byte[]> map = new HashMap<String, byte[]>();

		if (null == f || !f.exists()) {
			return map;
		}
		if (f.isDirectory()) {
			File[] fs = f.listFiles();

			for (File file : fs) {
				if (file.isDirectory()) {
					map.putAll(readLocalFilesToBytes(file));
				} else {
					byte[] bytes = readFileToBytes(file.getPath());
					map.put(CloudTestUtils.wrapFilePath(file.getPath()), bytes);
				}
			}
		} else {
			byte[] bytes = readFileToBytes(f.getPath());
			map.put(CloudTestUtils.wrapFilePath(f.getPath()), bytes);
		}

		return map;
	}

	static private byte[] readFileToBytes(String pathName) throws IOException {

		// InputStream in = this.getClass().getClassLoader()
		// .getResourceAsStream(pathName);

		FileInputStream in = new FileInputStream(new File(pathName));

		byte bytes[] = new byte[in.available()];
		in.read(bytes);

		return bytes;
	}

	static private Map<String, byte[]> readZipFilesToBytes(String zipFile, String entry) throws Exception {

		if (null == entry) {
			entry = "";
		}
		Map<String, byte[]> map = new HashMap<String, byte[]>();

		ZipFile zip = new ZipFile(zipFile);
		Enumeration e = zip.entries();

		ZipEntry ze = new ZipEntry(entry);
		while (e.hasMoreElements()) {
			ZipEntry z = (ZipEntry) e.nextElement();

			boolean hitted = CommonUtils.isNullOrEmpty(entry) || ze.isDirectory() == true
					? z.getName().startsWith(entry)
					: z.getName().equals(entry);

			if (null != z && !z.isDirectory() && hitted) {

				InputStream is = zip.getInputStream(z);
				byte[] b = new byte[(int) z.getSize()];

				is.read(b);
				// outputStream.write(b);

				is.close();
				map.put(z.getName(), b);
			}

		}

		zip.close();
		return map;
	}

	public static List<String> getAllFilePathListInFolder(String caseFilePath, boolean tcOnly) {

		List filePathList = new ArrayList();
		File file = new File(caseFilePath);

		if (file.exists()) {
			// if the caseFilePath is directory , Traverse the directory
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					filePathList.addAll(getAllFilePathListInFolder(files[i].getPath(), tcOnly));
				}
			}
			// if the caseFilePath is file , add all filePath which include
			// ".tc." or
			// ".senior." into filePathList
			else {
				if (!tcOnly || caseFilePath.toLowerCase().endsWith(CloudTestConstants.POSTFIX_TEST_CASE_XML)
						|| caseFilePath.toLowerCase().endsWith(CloudTestConstants.POSTFIX_UT_CASE_XML)) {
					filePathList.add(file.getPath());
				}
			}
		}

		Collections.sort(filePathList);

		return filePathList;
	}

	public static void processResultStatistics(CloudTestOutput output, Boolean isDirectory) {

		List<CloudTestOutput> outputList = output.getTestCaseResults();

		ResultStatistics resultStatistics = new ResultStatistics();
		output.setResultStatistics(resultStatistics);

		if (null == outputList) {
			return;
		}

		int successNum = 0;
		int faildNum = 0;
		double cost = 0;

		if (outputList.size() > 0) {
			int total = outputList.size();
			for (int i = 0; i < total; i++) {
				CloudTestOutput cloudTestOutput = outputList.get(i);

				if (isDirectory) {
					cloudTestOutput.setReturnValue(null);
					cloudTestOutput.setReturns(null);
				}

				if (cloudTestOutput.getStatus()) {
					successNum++;
				} else {
					faildNum++;
				}
				Double runTime = cloudTestOutput.getRunTime();
				if (runTime == null) {
					runTime = 0D;
				}

				cost += runTime;
			}

			resultStatistics.setFailedAmount(faildNum);
			resultStatistics.setSuccessfulAmount(successNum);
			resultStatistics.setTotalAmount(total);
			resultStatistics.setPassRate(Double.valueOf(successNum) / Double.valueOf(total));
			resultStatistics.setTotalRunTime(cost);
			resultStatistics.setDurationTime(output.getRunTime());

			output.setRunTime(null);
			output.setResultStatistics(resultStatistics);

			Collections.sort(outputList, new TestCaseOutputComparator());
			output.setTestCaseResults(outputList);
		}
	}

	public static class TestCaseOutputComparator implements Comparator<CloudTestOutput> {

		public int compare(CloudTestOutput o1, CloudTestOutput o2) {

			if (null == o1.getStatus() || null == o2.getStatus()) {
				return 0;
			}

			if (o1.getStatus() == o2.getStatus()) {
				return 0;
			} else {
				if (o1.getStatus() == true) {
					return 1;
				} else {
					return -1;
				}
			}
		}
	}

	public static void setProxyTimeout(Object service, int connectionTimeout, int receiveTimeout) {

		if (service instanceof Proxy) {
			Client proxy = ClientProxy.getClient(service);

			if (null != proxy) {
				HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
				HTTPClientPolicy policy = new HTTPClientPolicy();
				policy.setConnectionTimeout(connectionTimeout);
				policy.setReceiveTimeout(receiveTimeout);
				conduit.setClient(policy);
			}
		}
	}

	public static String wrapFilePath(String path) {

		if (path != null) {
			return path.replace("\\", "/");
		} else {
			return null;
		}
	}

	public static String[] resolveTestCaseImportsPath(String filePath, String impt) {

		String[] strs;
		if (impt.startsWith("TestCase/") || impt.startsWith("TestCase\\")) {
			strs = new String[] { ConfigurationProxy.getCloudTestRootPath() + File.separator + impt };
		} else {
			strs = CommonUtils.fetchIncludesFileNames(impt, filePath);
		}
		return strs;
	}

	/**
	 * Format inner member class's canonical name for xml digester. NOTES: only
	 * support for xStream engine.
	 * 
	 * @param clazz
	 * @return
	 */
	public static String formatMemberClassCanonicalName(Class clazz) {

		String dataTypeName;
		dataTypeName = clazz.getCanonicalName().replace("." + clazz.getSimpleName(), "_-" + clazz.getSimpleName())
				.replace("$" + clazz.getSimpleName(), "_-" + clazz.getSimpleName())
				.replace(ARRAY_YYPE_SYMBOL, ARRAY_TYPE_TAG);
		return dataTypeName;
	}

	public static String readFileContent(String path) throws Exception {

		File file = new File(path);
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream io = new FileInputStream(file);

		try {
			io.read(bytes);
			io.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			if (null != io) {
				io.close();
			}
		}

		return new String(bytes);
	}

	public static String getMD5code(byte[] request) {
		MessageDigest digest = null;

		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return org.apache.xerces.impl.dv.util.Base64.encode(digest.digest(request));

	}

}
