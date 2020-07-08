package com.unibeta.cloudtest.config.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unibeta.cloudtest.config.CacheManager;
import com.unibeta.cloudtest.config.CacheManagerFactory;
import com.unibeta.cloudtest.config.ConfigurationProxy;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Param;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Plugin;
import com.unibeta.cloudtest.config.plugin.PluginConfig.Recorder;
import com.unibeta.vrules.parsers.ObjectSerializer;
import com.unibeta.vrules.utils.CommonUtils;
import com.unibeta.vrules.utils.XmlUtils;

/**
 * Manage all plugin config files's data, provide all configuration data
 * accessing.
 * 
 * @author jordan.xue
 */
public class PluginConfigProxy {

	private static PluginConfig pluginConfig = null;
	private static long lastModified = 0;

	private final static double BEAT_CHECK_INTERVAL_MAX_SECONDS = 60;
	private final static double BEAT_CHECK_INTERVAL_MIN_SECONDS = 5;
	private final static double BEAT_CHECK_INTERVAL_STEP_SECONDS = 1.01;
	private static long lastBeatCheck = 0;
	private static double currentBeatCheckInterval = BEAT_CHECK_INTERVAL_MIN_SECONDS;

	private static Map<String, Object> cloudTestPluginInstancesMap = new HashMap<String, Object>();
	private static Map<String, String> paramValueMap = new HashMap<String, String>();

	private static Logger logger = LoggerFactory.getLogger(PluginConfigProxy.class);

	/**
	 * Gets CloudTestPlugin plugin instance by id.
	 * 
	 * @param id
	 * @param clazz
	 * @return <code>CloudTestPlugin</code> instance.
	 * @throws Exception
	 *             if the given class is not assignable from return instance.
	 */
	public static CloudTestPlugin getCloudTestPluginInstance(String id) throws Exception {

		Object obj = PluginConfigProxy.getPluginObject(id);

		if (obj != null && obj instanceof CloudTestPlugin) {
			return (CloudTestPlugin) obj;
		} else {
			throw new Exception("plguin id '" + id + "' was not found.");
		}

	}

	/**
	 * Gets plug-in common object that is configured in
	 * 'Config/PluginConfig.xml/plugin'.
	 * 
	 * @param id
	 *            refer to 'plugin/id'
	 * @return null if not found, otherwise return common object.
	 * @throws Exception
	 */
	public static Object getPluginObject(String id) throws Exception {

		Object cloudTestPlugin = null;

		if (CommonUtils.isNullOrEmpty(id)) {
			return null;
		} else {
			init();
		}

		cloudTestPlugin = cloudTestPluginInstancesMap.get(id);

		return cloudTestPlugin;
	}

	/**
	 * Get the parameter value by the given name, which is defined in
	 * PluginConfig.xml file.
	 * 
	 * @param paramName
	 * @return
	 * @throws Exception
	 */
	public static String getParamValueByName(String paramName) throws Exception {

		if (null == paramName) {
			return null;
		}

		init();

		return paramValueMap.get(paramName.toUpperCase());
	}

	/**
	 * Get the slave server info defined in PluginConfig.xml file.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Map<String, PluginConfig.SlaveServer> getSlaveServerMap() {

		Map<String, PluginConfig.SlaveServer> map = new HashMap<String, PluginConfig.SlaveServer>();
		List<PluginConfig.SlaveServer> list;
		try {
			list = loadGlobalPluginConfig().server;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return map;
		}

		for (PluginConfig.SlaveServer s : list) {
			map.put(s.id, s);

		}
		return map;
	}

	/**
	 * Get the case recorder info defined in PluginConfig.xml file.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Recorder> getCaseRecordersMap() {

		Map<String, Recorder> map = new HashMap<String, Recorder>();
		List<PluginConfig.Recorder> list;
		try {
			list = loadGlobalPluginConfig().recorder;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return map;
		}

		return map;
	}

	/**
	 * Get the case recorder info defined in PluginConfig.xml file.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Recorder getSinglePowerOnCaseRecorder() {

		List<PluginConfig.Recorder> list;
		try {
			list = loadGlobalPluginConfig().recorder;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}

		for (Recorder r : list) {
			if (!"true".equalsIgnoreCase(r.poweroff)) {
				return r;
			}
		}

		return null;
	}

	private static void initParamValueMap() throws Exception {

		paramValueMap.clear();
		List<Param> list = loadGlobalPluginConfig().param;

		for (Param e : list) {
			if (!CommonUtils.isNullOrEmpty(e.name)) {
				paramValueMap.put(e.name.toUpperCase(), e.value.trim());
			}
		}

	}

	/**
	 * Initial config data and return whether reloaded or not.
	 * 
	 * @return true as reloaded; false as no changes.
	 * @throws Exception
	 */
	private static boolean init() throws Exception {

		File CONFIG_FILE = new File(ConfigurationProxy.getConfigurationFilePath());

		double elaspe = (System.currentTimeMillis() - lastBeatCheck) / 1000.00;

		if (elaspe > currentBeatCheckInterval && CONFIG_FILE.exists() && lastModified != CONFIG_FILE.lastModified()) {
			initCloudTestPluginInstancesMap();
			initParamValueMap();

			lastModified = CONFIG_FILE.lastModified();
			lastBeatCheck = System.currentTimeMillis();
			currentBeatCheckInterval = BEAT_CHECK_INTERVAL_MIN_SECONDS;
			return true;
		} else {
			currentBeatCheckInterval *= BEAT_CHECK_INTERVAL_STEP_SECONDS;
			if (currentBeatCheckInterval > BEAT_CHECK_INTERVAL_MAX_SECONDS) {
				currentBeatCheckInterval = BEAT_CHECK_INTERVAL_MIN_SECONDS;
			}
			return false;
		}
	}

	public static void refresh() throws Exception {

		initCloudTestPluginInstancesMap();
		initParamValueMap();
	}

	private static void initCloudTestPluginInstancesMap() throws Exception {

		pluginConfig = loadGlobalPluginConfig();
		List<Plugin> list = pluginConfig.plugin;
		cloudTestPluginInstancesMap.clear();

		for (Plugin e : list) {
			if (!CommonUtils.isNullOrEmpty(e.className) && !CommonUtils.isNullOrEmpty(e.id)) {
				try {
					Thread.currentThread().getContextClassLoader().loadClass(e.className.trim());
					Class implementation = Thread.currentThread().getContextClassLoader().loadClass(e.className.trim());
					Class defination = Thread.currentThread().getContextClassLoader().loadClass(e.id.trim());

					if (!defination.isAssignableFrom(implementation)) {
						throw new Exception("given plguin '" + e.id + "' is not assignable from '" + e.className + "'");
					} else {

						Object newInstance = implementation.newInstance();
						if (newInstance instanceof CloudTestPlugin) {
							cloudTestPluginInstancesMap.put((e.id), (CloudTestPlugin) newInstance);
						} else {
							throw new Exception("given " + implementation.getName()
									+ " Plugin component have to extends '" + CloudTestPlugin.class.getName() + "'. ");
						}
					}
				} catch (Exception e1) {
					try {
						Class implementation = Class.forName(e.className.trim());
						cloudTestPluginInstancesMap.put(e.id, implementation.newInstance());
					} catch (Exception e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

					continue;
				}
			}
		}
	}

	/**
	 * Loads <code>PluginConfig</code> from PluginConfig.xml file.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static PluginConfig loadGlobalPluginConfig() throws Exception {

		try {
			pluginConfig = (PluginConfig) ObjectSerializer.unmarshalToObject(
					XmlUtils.paserDocumentToString(
							XmlUtils.getDocumentByFileName(ConfigurationProxy.getConfigurationFilePath())),
					PluginConfig.class);
		} catch (Exception e) {
			logger.warn("PluginConfig.xml is not in place, " + e.getMessage());
		}

		return pluginConfig;
	}

	/**
	 * Gets cached <code>PluginConfig</code> from PluginConfig.xml file.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static PluginConfig getGlobalPluginConfig() throws Exception {

		init();

		if (pluginConfig == null) {
			pluginConfig = loadGlobalPluginConfig();
		}

		return pluginConfig;
	}

	/**
	 * check PluginConfig.xml file modified or not.
	 * 
	 * @return true or false
	 */
	public static boolean isModified() {
		boolean bool = false;
		try {
			Object batchRunning = CacheManagerFactory.getGlobalCacheInstance()
					.get(CacheManager.CACHE_TYPE_RUNNING_STATUS, CacheManager.CACHE_TYPE_RUNNING_STATUS);
			if (batchRunning == null) {
				batchRunning = false;
			}

			bool = !(Boolean) batchRunning && init();
		} catch (Exception e) {
			e.printStackTrace();
			bool = false;
		}
		return bool;
	}

	// public static void main(String[] args) {
	// PluginConfig pluginConfig = new PluginConfig();
	//
	// List<PluginElement> peList1 = new ArrayList<PluginElement>();
	// List<ParamElement> peList2 = new ArrayList<ParamElement>();
	//
	// PluginElement element1 = new PluginElement();
	// ParamElement element2 = new ParamElement();
	//
	// peList1.add(element1);
	// peList2.add(element2);
	//
	// pluginConfig.paramElement = peList2;
	// pluginConfig.pluginElement = peList1;
	//
	// try {
	// String str = ObjectSerializer.marshalToXml(pluginConfig);
	// // System.out.println(str);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// }
	//
	// }
}
