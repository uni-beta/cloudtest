package com.unibeta.cloudtest.config;

import java.util.List;

/**
 * Default implementation is ehcache.
 * 
 * @author jordan.xue
 */
public interface CacheManager {

    /**
     * runtime_data
     */
    public static String CACHE_TYPE_RUNTIME_DATA = "runtime_data";
    /**
     * test_case
     */
    public static String CACHE_TYPE_TESTCASE = "test_case";
    /**
     * source_file
     */
    public static String CACHE_TYPE_SRC_FILE = "source_file";
    /**
     * java_instance
     */
    public static String CACHE_TYPE_JAVA_INSTANCE = "java_instance";
    /**
     * dependence_case_ranking
     */
    public static String CACHE_TYPE_DEPENDENCE_CASES_RANKING = "dependence_case_ranking";
    /**
     * tasks_queue
     */
    public static String CACHE_TYPE_TASKS_QUEUE = "tasks_queue";
    /**
     * running_status
     */
    public static String CACHE_TYPE_RUNNING_STATUS = "running_status";
    public static String CACHE_TYPE_RUNNING_STATUS_IS_BATCH_RUNNING = "isBatchRunning";

    public void clear();

    public Object get(String contextType, String key);

    public Object remove(String contextType, String key);

    public List<String> keySet(String contextType);

    public void put(String contextType, String key, Object value);
    public boolean isThreadLocalCache();
}
