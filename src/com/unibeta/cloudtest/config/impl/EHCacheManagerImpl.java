package com.unibeta.cloudtest.config.impl;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.config.CacheManager;
import com.unibeta.cloudtest.config.ConfigurationProxy;

public class EHCacheManagerImpl implements CacheManager {

    private static net.sf.ehcache.CacheManager ehcache = null;
    private static ThreadLocal<Set<String>> cacheNames = new ThreadLocal<Set<String>>();

    private static Logger log = Logger.getLogger(EHCacheManagerImpl.class);
    {
        try {
            File f = new File(ConfigurationProxy.getCloudTestRootPath()
                    + "/Config/ehcache.xml");

            if (f.exists()) {
                ehcache = net.sf.ehcache.CacheManager.create(f.getPath());
            } else {
                ehcache = net.sf.ehcache.CacheManager.create();
            }
        } catch (CacheException e) {
            log.warn(
                    "ehcache will use default configuration for cache management",
                    e);
            ehcache = net.sf.ehcache.CacheManager.create();
        }
    }

    public void clear() {

        List<String> taskSet = keySet(CACHE_TYPE_TASKS_QUEUE);
        for (String cacheName : cacheNames.get()) {

            if (null != cacheName && this.getCache(cacheName) != null
                    && (taskSet == null || taskSet.size() == 0)) {
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

        return Thread.currentThread().getName() + ":" + cacheName;
    }

    Cache getCache(String cacheName) {

        // cacheName = buildCacheName(cacheName);
        Cache cache = ehcache.getCache(cacheName);

        if (null == cache) {
            synchronized (cacheName) {
                ehcache.addCache(cacheName);
                if (cacheNames.get() == null) {
                    cacheNames.set(new HashSet<String>());
                }

                cacheNames.get().add(cacheName);
            }
            cache = ehcache.getCache(cacheName);
        }

        return cache;
    }
}
