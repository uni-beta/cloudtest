/*
 * vRules, copyright (C) 2007-2010 www.uni-beta.com. vRules is free software;
 * you can redistribute it and/or modify it under the terms of Version 2.0
 * Apache License as published by the Free Software Foundation. vRules is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the Apache License for more details below or at
 * http://www.apache.org/licenses/ Licensed to the Apache Software Foundation
 * (ASF) under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. </pre>
 */
package com.unibeta.cloudtest.config.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.config.CacheManagerFactory;
import com.unibeta.vrules.constant.VRulesConstants;
import com.unibeta.vrules.utils.CommonUtils;

/**
 * <code>CloudTestClassLoader</code> is a subclass of <code>ClassLoader</code>.
 * It is responsible for get the instance from java source file.
 * 
 * @author jordan
 */
public class CloudTestClassLoader extends URLClassLoader {

    private static Logger log = LoggerFactory.getLogger(CloudTestClassLoader.class);

    private String fileName = null;
    private String packageName = null;

    public CloudTestClassLoader(URL[] urls, String fileName, String pkg) {

        super(urls, Thread.currentThread().getContextClassLoader());
        this.fileName = fileName;
        this.packageName = pkg;
    }

    /**
     * Creates a new validation instance by given rules configuration file name.
     * 
     * @param fileName
     *            the rules configuration fiame name.
     * @return the instance of class
     * @throws Exception
     */
    public Object newInstance() throws Exception {

        Object instance = null;
        String classRUIName = buildClassPath();

        try {
            Class clazzDcc = loadClass(classRUIName, false);

            Object obj = clazzDcc.newInstance();
            instance = obj;

            synchronized (classRUIName) {
                CacheManagerFactory
                        .getThreadLocalInstance()
                        .put(CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_JAVA_INSTANCE,
                                classRUIName, instance);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }

        return instance;
    }

    /**
     * Generates the URLs by given vRuels configuration file name.
     * 
     * @param fileName
     *            vRuls configuration file name.
     * @return local APP-Classpath's URLs.
     * @throws MalformedURLException
     */
    public static URL[] generateURLs(String fileName)
            throws MalformedURLException {

        String classesPath = CommonUtils.getFilePathName(fileName)
                + File.separator + VRulesConstants.DYNAMIC_CLASSES_FOLDER_NAME
                + File.separator;

        File file = new File(classesPath);
        List list = new ArrayList();
//         CommonUtils.copyArrayToList(URLConfiguration.getClasspathURLs(),
//         list);
        list.add(file.toURL());

        URL[] urls = (URL[]) list.toArray(new URL[] {});
        return urls;
    }

    /**
     * Gets current availabe validation instance directly.
     * 
     * @return
     * @throws Exception
     */
    public Object getInstance() throws Exception {

        Object instance = null;

        String className = buildClassPath();

        instance = CacheManagerFactory.getThreadLocalInstance().get(
                CacheManagerFactory.getThreadLocalInstance().CACHE_TYPE_JAVA_INSTANCE,
                className);

        if (null == instance) {
            instance = newInstance();

        }

        return instance;
    }

    private String buildClassPath() {

        String name = CommonUtils.getFileSimpleName(fileName);

        String className = name;
        if(CommonUtils.isNullOrEmpty(this.packageName)){
            className = name;
        }else{
            className = this.packageName + "." + name;
        }
        
        if (name != null && name.toLowerCase().endsWith(".java")) {
            className = className.substring(0, className.length() - 5);
        }

        return className.trim();
    }
}
