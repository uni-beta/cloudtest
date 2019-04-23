package com.unibeta.cloudtest.config;

import com.unibeta.cloudtest.config.impl.CloudTestContextImpl;
import com.unibeta.cloudtest.config.impl.EHCacheManagerImpl;
import com.unibeta.cloudtest.config.plugin.PluginConfigProxy;
import com.unibeta.cloudtest.constant.CloudTestConstants;

public class CacheManagerFactory {

	public static CacheManager getInstance() {

		try {
			if (CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE_EHCACHE.equalsIgnoreCase(
					PluginConfigProxy.getParamValueByName(CloudTestConstants.CLOUDTEST_SYSTEM_CACHE_TYPE))) {
				Class.forName("net.sf.ehcache.CacheManager");
				return new EHCacheManagerImpl();
			} else {
				return new CloudTestContextImpl();
			}

		} catch (Exception e) {
			return new CloudTestContextImpl();
		}
	}
}
