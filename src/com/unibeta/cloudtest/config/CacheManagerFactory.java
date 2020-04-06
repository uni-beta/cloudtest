package com.unibeta.cloudtest.config;

import com.unibeta.cloudtest.config.impl.CloudTestContextImpl;
import com.unibeta.cloudtest.config.impl.EHCacheManagerImpl;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;

public class CacheManagerFactory {

	static Boolean hasEhcache = null;

	// static CacheManager ehCacheManagerImpl = new EHCacheManagerImpl();
	// static CacheManager cloudTestContextImpl = new CloudTestContextImpl();

	static CacheManager cacheManager = null;

	public static CacheManager getInstance() {
		
		if( null != cacheManager) {
			return cacheManager;
		}

		try {
			if (checkHasEhcache() && CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE_EHCACHE.equalsIgnoreCase(
					PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE))) {
				cacheManager = new EHCacheManagerImpl();
				return cacheManager;
			} else {
				cacheManager = new CloudTestContextImpl();
				return cacheManager;
			}
		} catch (Exception e) {
			cacheManager = new CloudTestContextImpl();
			return cacheManager;
		}
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
