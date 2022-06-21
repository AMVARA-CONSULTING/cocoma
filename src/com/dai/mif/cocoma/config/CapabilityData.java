/**
 * $Id: CapabilityData.java 93 2010-03-04 08:42:55Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 93 $ ($Date:: 2010-03-04 09:42:55 +0100#$)
 */
public class CapabilityData {

	private Logger log;
	
	public static final String RECORD_KEY_PREFIX = "capabilities.capability";

	private List<CapabilityData> capabilityFeatures;

	private String name;

	private ArrayList<String> roleMembers;
	private ArrayList<String> groupMembers;
	private ArrayList<String> userMembers;

	private boolean permissionRead;

	private boolean permissionWrite;

	private boolean permissionExecute;

	private boolean permissionSetPolicy;

	private boolean permissionTraverse;
	
	/* the hidden attribute is for hiding the capability from the user */
	private boolean capabilityHidden;
	

	public CapabilityData(String name) {
		
		this.log = Logging.getInstance().getLog(this.getClass());

		log.debug("Reading on: "+name);
		this.name = name;

		this.roleMembers = new ArrayList<String>();
		this.groupMembers = new ArrayList<String>();
		this.userMembers = new ArrayList<String>();

		this.capabilityFeatures = new ArrayList<CapabilityData>();
		this.capabilityHidden = false;
	}

	/**
	 * @param conf
	 * @param configKey
	 */
	public void readConfig(XMLConfiguration conf, String configKey)
			throws ConfigException {
		try {
			log.debug("Hidden:"+conf.getBoolean(configKey+".hidden", false));
			
			this.permissionRead = conf.getBoolean(configKey
					+ ".permission.read", true);
			this.permissionWrite = conf.getBoolean(configKey
					+ ".permission.write", false);
			this.permissionExecute = conf.getBoolean(configKey
					+ ".permission.execute", true);
			this.permissionSetPolicy = conf.getBoolean(configKey
					+ ".permission.setPolicy", false);
			this.permissionTraverse = conf.getBoolean(configKey
					+ ".permission.traverse", true);
			
			this.capabilityHidden = conf.getBoolean(configKey+".hidden", false);
			
			String[] roles = conf.getStringArray(configKey
					+ ".permission.members.role");
			if (roles != null) {
				for (String role : roles) {
					if (role.length() > 0) {
						roleMembers.add(role);
					}
				}
			}

			String[] groups = conf.getStringArray(configKey
					+ ".permission.members.group");
			if (groups != null) {
				for (String group : groups) {
					if (group.length() > 0) {
						groupMembers.add(group);
					}
				}
			}

			String[] users = conf.getStringArray(configKey
					+ ".permission.members.user");
			if (users != null) {
				for (String user : users) {
					if (user.length() > 0) {
						userMembers.add(user);
					}
				}
			}

			readConfiguredFeatures(conf, configKey);

		} catch (Exception e) {
			throw new ConfigException(e.getMessage());
		}

	}

	/**
	 * @param conf
	 * @param configKey
	 */
	private void readConfiguredFeatures(XMLConfiguration conf, String configKey)
			throws ConfigException {
		
		String[] featureNames = conf.getStringArray(configKey
				+ ".features.feature.name");

		for (int i = 0; i < featureNames.length; i++) {
			log.debug("Feature name: "+featureNames[i]);
			String featureKey = configKey + ".features.feature(" + i + ")";
			CapabilityData feature = new CapabilityData(featureNames[i]);
			feature.readConfig(conf, featureKey);
			this.capabilityFeatures.add(feature);
		}
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return
	 */
	public boolean getPermissionRead() {
		return this.permissionRead;
	}

	/**
	 * @return
	 */
	public boolean getPermissionWrite() {
		return this.permissionWrite;
	}

	/**
	 * @return
	 */
	public boolean getPermissionExecute() {
		return this.permissionExecute;
	}

	/**
	 * @return
	 */
	public boolean getPermissionSetPolicy() {
		return this.permissionSetPolicy;
	}

	/**
	 * @return
	 */
	public boolean getPermissionTraverse() {
		return permissionTraverse;
	}

	/**
	 * @return
	 */
	public List<String> getGroupMembers() {
		return groupMembers;
	}

	/**
	 * @return
	 */
	public List<String> getRoleMembers() {
		return roleMembers;
	}

	/**
	 * @return
	 */
	public List<String> getUserMembers() {
		return userMembers;
	}

	public List<CapabilityData> getCapabilityFeatures() {
		return this.capabilityFeatures;
	}

	public boolean isCapabilityhidden() {
		return capabilityHidden;
	}

	public void setCapabilityhidden(boolean capabilityhidden) {
		this.capabilityHidden = capabilityhidden;
	}

}
