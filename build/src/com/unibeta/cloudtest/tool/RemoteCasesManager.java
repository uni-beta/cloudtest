package com.unibeta.cloudtest.tool;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.xerces.impl.dv.util.Base64;

import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.util.CloudTestUtils;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * A tool to upload test cases from local client to target server.
 * 
 * @author jordan.xue
 */
public class RemoteCasesManager {

	private static final String IGNORE_CONFIG = "^Config$";
	private static final String TARGET_TEST_CASE$_TEST_DATA$ = "^TestCase$|^TestData$";

	/**
	 * Upload test case file or ziped package to target server. <br>
	 * If uploaded file is ziped package, it will be unziped automatically into
	 * CLOUDTEST_HOME folder.
	 * 
	 * @param attachmentContent
	 *            Base64 decoded binary data source in String format, only
	 *            Base64 decode is supported.
	 * @param targetFilePathName
	 *            file path name under CLOUDTEST_HOME folder. e.g
	 *            "/cloudTest.zip" ,"/newTest/cloudTest.zip" or
	 *            "cloudTest/TestCase/myCase.tc.xml"
	 * @throws Exception
	 */
	
	public static void upload(String attachmentContent,
			String targetFilePathName) throws Exception {
		if (null == attachmentContent) {
			throw new Exception("input data source is null.");
		}

		byte[] decodeData = Base64.decode(attachmentContent);

		if (null == decodeData) {
			throw new Exception("decoded data source is null.");
		}
		
		upload(decodeData,targetFilePathName);
	}
	
	public static void upload(byte[] attachmentContent,
			String targetFilePathName) throws Exception {

		if (null == attachmentContent) {
			throw new Exception("input data source is null.");
		}

		File f = new File(targetFilePathName);
		String fileName = targetFilePathName;
		if (f.isAbsolute()) {
			fileName = targetFilePathName;
		} else {
			fileName = ConfigurationProxy.getCLOUDTEST_HOME() + "/"
					+ targetFilePathName;
		}

		File file = new File(fileName);
		RandomAccessFile accessFile = null;
		try {

			CloudTestUtils.checkFile(fileName);

			accessFile = new RandomAccessFile(fileName, "rw");
			if (!file.exists()) {
				file.createNewFile();
			}

			accessFile.seek(accessFile.length());
			accessFile.write(attachmentContent);

			// if (file.getName().toUpperCase().endsWith(".ZIP")) {
			// CloudTestUtils.unzipFiles(file);
			// }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (null != accessFile) {
				accessFile.close();
			}
		}
	}

	/**
	 * Delete TestData and TestCase folders under CLOUDTEST_HOME path located in
	 * give folder.
	 * 
	 * @param folderName
	 */
	public static void clean(String folderName) {

		String fileName = ConfigurationProxy.getCLOUDTEST_HOME();

		if (!CommonUtils.isNullOrEmpty(folderName)) {
			fileName = fileName + File.separator + folderName;
		}

		List<String> files = CommonUtils.searchFilesUnder(fileName,
				TARGET_TEST_CASE$_TEST_DATA$, IGNORE_CONFIG, true);

		File home = new File(ConfigurationProxy.getCLOUDTEST_HOME());
		// File root = new File(fileName);

		for (String fn : files) {

			CloudTestUtils.deleteFiles(fn);

			File f = new File(fn);
			if (!home.equals(f)) {
				f.delete();
			}

		}
	}

	/**
	 * Clean all files in cloudtest home path, including cloudtest home.
	 */
	public static void cleanAll() {

		String fileName = ConfigurationProxy.getCloudTestRootPath();

		if (CommonUtils.isNullOrEmpty(fileName)) {
			return;
		}

		List<String> files = CommonUtils.searchFilesUnder(fileName, ".",
				"#@@#$%^&", true);

		for (String fn : files) {

			CloudTestUtils.deleteFiles(fn);

			File f = new File(fn);
			f.delete();

		}
	}
}
