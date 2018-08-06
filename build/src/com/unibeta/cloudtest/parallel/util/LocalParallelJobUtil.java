package com.unibeta.cloudtest.parallel.util;

import java.util.Date;

import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.vrules.utils.CommonUtils;

public class LocalParallelJobUtil {

	static String currentUser = null;
	static long jobStartTimeMillis = -1;
	static String PARALLELJOB_TOKEN_FLAG = "@TOKEN:TESTCASES@";

	public static String wrapWithToken(String name) {
		String withToken = ConfigurationProxy.getOsUserName()
				+ PARALLELJOB_TOKEN_FLAG + name;
		return withToken;
	}

	public static String[] parseTokenAndValue(String name) {
		String[] strs = name.split(PARALLELJOB_TOKEN_FLAG);
		return strs;
	}

	public static String getCurrentUser() {

		return currentUser;
	}

	public synchronized static void setCurrentUser(String currentUser) {

		if (!CommonUtils.isNullOrEmpty(currentUser)) {
			LocalParallelJobUtil.jobStartTimeMillis = System
					.currentTimeMillis();
			LocalParallelJobUtil.currentUser = currentUser;
		} else {
			LocalParallelJobUtil.jobStartTimeMillis = -1;
			LocalParallelJobUtil.currentUser = null;
		}
	}

	public static Date getJobStartTime() {

		if (jobStartTimeMillis > 0) {
			return new Date(jobStartTimeMillis);
		} else {
			return null;
		}
	}

	public static long getJobRunningTime() {

		if (jobStartTimeMillis > 0) {
			return (System.currentTimeMillis() - jobStartTimeMillis) / 1000;
		} else {
			return -1;
		}
	}

	public static boolean isInParallelJobService() {

		if (!CommonUtils.isNullOrEmpty(currentUser)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getWarningMessage() {

		if (LocalParallelJobUtil.isInParallelJobService()) {
			String msg = null;
			msg = "[WARNING]current cloudtest service has been occupied by user[name='"
					+ LocalParallelJobUtil.getCurrentUser()
					+ "'] in ParallelJob service for "
					+ LocalParallelJobUtil.getJobRunningTime()
					+ "s from "
					+ LocalParallelJobUtil.getJobStartTime()
					+ ", potentially has some inconsistent TestCase and TestData issues."
					+ "\nCurrent used CLOUDTEST_HOME is '"
					+ ConfigurationProxy.getCLOUDTEST_HOME()
					+ "', please double check whether it belongs to you. if not, please try it again."
					+ "\n";

			return msg;
		} else {
			return null;
		}
	}

}
