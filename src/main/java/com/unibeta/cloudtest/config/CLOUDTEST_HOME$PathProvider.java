package com.unibeta.cloudtest.config;

/**
 * <code>CLOUDTEST_HOME$PathProvider</code> provides basic configuration path
 * service.
 * 
 * @author jordan.xue
 */
public interface CLOUDTEST_HOME$PathProvider {

    /**
     * The environment parameter for cloud test home configuration. Parameter
     * name is "CLOUDTEST_HOME".
     */
    public final static String CLOUDTEST_HOME = "CLOUDTEST_HOME";
    public static final String CLOUDTEST_HOME_PROPERTY = "cloudtest.home";

    /**
     * Get the local config path,where cloudtest HOME folder located.
     * 
     * @return file path.
     */
    public String getCLOUDTEST_HOME() throws Exception;

}
