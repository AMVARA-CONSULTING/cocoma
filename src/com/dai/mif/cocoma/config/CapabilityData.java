/**
 * $Id: CapabilityData.java 93 2010-03-04 08:42:55Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import com.dai.mif.cocoma.exception.CoCoMaConfigException;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 93 $ ($Date:: 2010-03-04 09:42:55 +0100#$)
 */
public class CapabilityData {

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

	public CapabilityData(String name) {
		this.name = name;

		this.roleMembers = new ArrayList<String>();
		this.groupMembers = new ArrayList<String>();
		this.userMembers = new ArrayList<String>();

		this.capabilityFeatures = new ArrayList<CapabilityData>();
	}

	/**
	 * @param conf
	 * @param configKey
	 */
	public void readConfig(XMLConfiguration conf, String configKey)
			throws CoCoMaConfigException {
		try {
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
			throw new CoCoMaConfigException(e.getMessage());
		}

	}

	/**
	 * @param conf
	 * @param configKey
	 */
	private void readConfiguredFeatures(XMLConfiguration conf, String configKey)
			throws CoCoMaConfigException {

		String[] featureNames = conf.getStringArray(configKey
				+ ".features.feature.name");

		for (int i = 0; i < featureNames.length; i++) {
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

}
