package com.unibeta.cloudtest.config.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <code>PluginConfig</code> is a common model for plugin config xml data
 * binding.
 * 
 * @author jordan.xue
 * 
 */

@XmlRootElement
public class PluginConfig {
	@XmlElement
	public List<Plugin> plugin = new ArrayList<Plugin>();
	@XmlElement
	public List<Param> param = new ArrayList<Param>();
	@XmlElement
	
	@XmlElementWrapper(name = "slave-servers")
	public List<SlaveServer> server = new ArrayList<SlaveServer>();
	
	@XmlElementWrapper(name = "case-recorders")
	public List<Recorder> recorder = new ArrayList<Recorder>();

	/**
	 * Parameter definition in plugin configuration.
	 * 
	 * @author jordan.xue
	 */
	@XmlRootElement
	public static class Param {

		@XmlAttribute
		public String name = "";
		@XmlAttribute
		public String value = "";
	}

	/**
	 * Basic model for plguin defination.
	 * 
	 * @author jordan.xue
	 */
	public static class Plugin {

		@XmlAttribute
		public String id = "";
		@XmlAttribute
		public String desc = "";
		// @XmlElement
		// public String definition = "";
		@XmlElement
		public String className = "";

	}

	@XmlRootElement
	public static class SlaveServer {

		@XmlAttribute
		public String id = "";
		@XmlAttribute
		public String address = "";
		@XmlAttribute
        public String type = "webservice";
		@XmlAttribute
		public String desc = "";
	}

	@XmlRootElement
	public static class Recorder {

		@XmlAttribute
		public String id = "";
		@XmlAttribute
		public String poweroff = "false";
		@XmlAttribute
		public String desc = "";
		@XmlAttribute
		public String targetCaseFilePath = "";
		
		@XmlElement
		public List<SignatureRegex> signatureRegex = new ArrayList<SignatureRegex>();
	}

	@XmlRootElement
	public static class SignatureRegex {
		@XmlElement
		public String className = ".*";
		@XmlElement
		public String methodName = ".*";
		@XmlElement
		public String modifiers = ".*";
	}
}
