package com.unibeta.cloudtest.config.plugin.elements.impl;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.unibeta.cloudtest.config.plugin.elements.SpringBeanFactoryPlugin;

public class SpringBeanFactoryPluginImpl extends DefaultListableBeanFactory implements SpringBeanFactoryPlugin,
		BeanFactory {

    public BeanFactory getBeanFactory() {
        return this;
    }
}
