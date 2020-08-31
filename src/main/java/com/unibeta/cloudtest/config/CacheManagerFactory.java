package com.unibeta.cloudtest.config;

import com.unibeta.cloudtest.config.impl.EHCacheManagerImpl;
import com.unibeta.cloudtest.config.impl.SimpleGlobalCacheImpl;
import com.unibeta.cloudtest.config.impl.SimpleThreadLocalCacheImpl;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;

public class CacheManagerFactory {

	private static Boolean hasEhcache = null;

	private static CacheManager threadLocalCacheManager = null;
	private static CacheManager globalCacheManager = null;

	/**
	 * Get cache manager instance of Threadlocal supported. It is of thread safety.
	 * 
	 * @return
	 */
	public static CacheManager getThreadLocalInstance() {

		if (null != threadLocalCacheManager) {
			return threadLocalCacheManager;
		}

		try {
			if (checkHasEhcache() && CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE_EHCACHE.equalsIgnoreCase(
					PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE))) {
				threadLocalCacheManager = new EHCacheManagerImpl();
				return threadLocalCacheManager;
			} else {
				threadLocalCacheManager = new SimpleThreadLocalCacheImpl();
				return threadLocalCacheManager;
			}
		} catch (Exception e) {
			threadLocalCacheManager = new SimpleThreadLocalCacheImpl();
			return threadLocalCacheManager;
		}
	}

	/**
	 * Gets global cache manager, it is not of thread safety.
	 * 
	 * @return
	 */
	public static CacheManager getGlobalCacheInstance() {

		if (null != globalCacheManager) {
			return globalCacheManager;
		}

		try {
			if (checkHasEhcache() && CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE_EHCACHE.equalsIgnoreCase(
					PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE))) {
				globalCacheManager = new EHCacheManagerImpl(false);
				return globalCacheManager;
			} else {
				globalCacheManager = new SimpleGlobalCacheImpl();
				return globalCacheManager;
			}
		} catch (Exception e) {
			globalCacheManager = new SimpleGlobalCacheImpl();
			return globalCacheManager;
		}
	}

	/**
	 * Get cache manager instance of Threadlocal supported. It is of thread safety.
	 * Same as getThreadLocalInstance().
	 * 
	 * @see getThreadLocalInstance()
	 * @return
	 */
	@Deprecated
	public static CacheManager getInstance() {
		return getThreadLocalInstance();
	}

	static boolean checkHasEhcache() {

		if (hasEhcache == null) {
			try {
				Class.forName("net.sf.ehcache.CacheManager");
				hasEhcache = true;
			} catch (ClassNotFoundException e) {
				hasEhcache = false;
			}
			return hasEhcache;
		} else {
			return hasEhcache;
		}
	}
}
