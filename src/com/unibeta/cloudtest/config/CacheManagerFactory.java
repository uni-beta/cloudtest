package com.unibeta.cloudtest.config;

import com.unibeta.cloudtest.config.impl.CloudTestContextImpl;
import com.unibeta.cloudtest.config.impl.EHCacheManagerImpl;


public class CacheManagerFactory {

    public static CacheManager getInstance(){
        
        try {
            Class.forName("net.sf.ehcache.CacheManager");
            return new EHCacheManagerImpl();
        } catch (ClassNotFoundException e) {
            return new CloudTestContextImpl();
        }
    }
}
