package com.unibeta.cloudtest.config.plugin.elements;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;

import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;

/**
 * Spring <code>BeanFactory</code> plugin interface.
 * 
 * @author jordan.xue
 * 
 */
public interface SpringBeanFactoryPlugin extends CloudTestPlugin, BeanFactory,ListableBeanFactory {
    public BeanFactory getBeanFactory();
}
