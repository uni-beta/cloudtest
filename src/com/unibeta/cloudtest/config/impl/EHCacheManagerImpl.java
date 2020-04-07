package com.unibeta.cloudtest.config.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.config.CacheManager;
import com.unibeta.cloudtest.config.ConfigurationProxy;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * Runtime cache service that supports ThreadLocal and global cache implementation.<br>
 * By default, EHCacheManagerImpl() using ThreadLocal mode.<br>
 * If need global cache service, please use EHCacheManagerImpl(false).
 * 
 * @author jordan.xue
 *
 */
public class EHCacheManagerImpl implements CacheManager {

	private static net.sf.ehcache.CacheManager ehcache = null;
	private static ThreadLocal<Set<String>> threadLocalCachedNameSet = new ThreadLocal<Set<String>>();

	private static Logger log = Logger.getLogger(EHCacheManagerImpl.class);
	private boolean isThreadLocal = true;

	public EHCacheManagerImpl(boolean isThreadLocal) {
		this.isThreadLocal = isThreadLocal;
	}

	public EHCacheManagerImpl() {
		this.isThreadLocal = true;
	}

	{
		InputStream ehcacheStream = null;
		try {
			try {
				File f = new File(ConfigurationProxy.getCloudTestRootPath() + "/Config/ehcache.xml");
				ehcacheStream = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				ehcacheStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ehcache.xml");
			}

			if (ehcacheStream != null) {
				ehcache = net.sf.ehcache.CacheManager.create(ehcacheStream);
			} else {
				log.warn("ehcache will use default configuration for cache management");
				ehcache = net.sf.ehcache.CacheManager.create();
			}

		} catch (Exception e) {
			log.error("Create CacheManager failed," + e.getMessage(), e);
		} finally {
			if (ehcacheStream != null) {
				try {
					ehcacheStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void clear() {

		List<String> taskSet = keySet(CACHE_TYPE_TASKS_QUEUE);
		for (String cacheName : threadLocalCachedNameSet.get()) {

			if (null != cacheName && this.getCache(cacheName) != null && (taskSet == null || taskSet.size() == 0)) {
				ehcache.removeCache(cacheName);
			}
		}

	}

	public Object get(String cacheName, String key) {

		cacheName = buildCacheName(cacheName);
		Element element = getCache(cacheName).get(key);
		if (null == element) {
			return null;
		} else {
			return element.getObjectValue();
		}
	}

	public Object remove(String cacheName, String key) {

		cacheName = buildCacheName(cacheName);
		return getCache(cacheName).remove(key);
	}

	public List<String> keySet(String cacheName) {

		cacheName = buildCacheName(cacheName);
		return (List<String>) getCache(cacheName).getKeys();
	}

	public void put(String cacheName, String key, Object value) {

		cacheName = buildCacheName(cacheName);
		getCache(cacheName).put(new Element(key, value));

	}

	private String buildCacheName(String cacheName) {

		if (this.isThreadLocal) {
			return Thread.currentThread().getName() + ":" + cacheName;
		} else {
			return cacheName;
		}
	}

	Cache getCache(String cacheName) {

		// cacheName = buildCacheName(cacheName);
		Cache cache = ehcache.getCache(cacheName);

		if (null == cache) {
			synchronized (cacheName) {
				ehcache.addCache(cacheName);
				if (threadLocalCachedNameSet.get() == null) {
					threadLocalCachedNameSet.set(new HashSet<String>());
				}

				threadLocalCachedNameSet.get().add(cacheName);
			}
			cache = ehcache.getCache(cacheName);
		}

		return cache;
	}

	@Override
	public boolean isThreadLocalCache() {
		return this.isThreadLocal;
	}
}
