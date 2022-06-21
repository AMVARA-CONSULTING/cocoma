/**
 * $Id: SecurityPermission.java 92 2010-03-03 15:41:41Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AccessEnum;
import com.cognos.developer.schemas.bibus._3.UserCapabilityEnum;
import com.cognos.developer.schemas.bibus._3.UserCapabilityPermission;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 2, 2010
 * @version $Revision: 92 $ ($Date:: 2010-03-03 16:41:41 +0100#$)
 */
public class SecurityPermission {

	public static final String RECORD_KEY_PREFIX = "security.permissions.permission";
	private Logger log;

	private String targetName;

	private boolean permissionRead;
	private boolean permissionWrite;
	private boolean permissionExecute;
	private boolean permissionSetPolicy;
	private boolean permissionTraverse;

	private List<String> roleMembers;
	private List<String> groupMembers;
	private List<String> userMembers;

	// ROLE CAPABILIESTIES CapName Access Value
	private HashMap<HashMap<String, String>, UserCapabilityPermission[]> userCapabilitiesPolicies = new HashMap<>();

	/**
	 * @param targetName
	 */
	public SecurityPermission(String targetName) {
		this.targetName = targetName;

		this.roleMembers = new ArrayList<>();
		this.groupMembers = new ArrayList<>();
		this.userMembers = new ArrayList<>();

		this.log = Logging.getInstance().getLog(this.getClass());
	}

	/**
	 * @param conf
	 * @param configKey
	 */
	public void readConfig(XMLConfiguration conf, String configKey) throws ConfigException {
		try {
			this.permissionRead = conf.getBoolean(configKey + ".read", true);
			this.permissionWrite = conf.getBoolean(configKey + ".write", false);
			this.permissionExecute = conf.getBoolean(configKey + ".execute", true);
			this.permissionSetPolicy = conf.getBoolean(configKey + ".setPolicy", false);
			this.permissionTraverse = conf.getBoolean(configKey + ".traverse", true);

			String[] roles = conf.getStringArray(configKey + ".members.role");
			if (roles != null) {
				for (String role : roles) {
					if (role.length() > 0) {
						roleMembers.add(role);
					}
				}
			}

			String[] groups = conf.getStringArray(configKey + ".members.group");
			if (groups != null) {
				for (String group : groups) {
					if (group.length() > 0) {
						groupMembers.add(group);
					}
				}
			}

			String[] users = conf.getStringArray(configKey + ".members.user");
			if (users != null) {
				for (String user : users) {
					if (user.length() > 0) {
						userMembers.add(user);
					}
				}
			}

			// get all policies from the document
			@SuppressWarnings("unchecked")
			List<HierarchicalConfiguration> policies = conf.configurationsAt(configKey + ".policies.policy");
			for (HierarchicalConfiguration policy : policies) {
				// capabilities hashmap
				ArrayList<UserCapabilityPermission> xmlCapabilities = new ArrayList<>();

				// get all the capabilities and loop over them
				@SuppressWarnings("unchecked")
				List<HierarchicalConfiguration> capabilities = policy.configurationsAt("capabilities.capability");
				for (HierarchicalConfiguration capability : capabilities) {
					// get capability name
					String name = capability.getRoot().getValue().toString();
					// get access value
					String access = capability.getRoot().getAttribute(0).getValue().toString();

					// create a permission capability
					UserCapabilityPermission cap = new UserCapabilityPermission();
					cap.setAccess(AccessEnum.fromString(access));
					cap.setUserCapability(UserCapabilityEnum.fromString(name));

					// append capability to the xmlCapabilities
					xmlCapabilities.add(cap);
				}

				// get securiyObject role tags
				String[] securityObjectsRoles = policy.getStringArray("role");
				// get securiyObject group tags
				String[] securityObjectsGroups = policy.getStringArray("group");
				// get securiyObject user tags
				String[] securityObjectsUsers = policy.getStringArray("user");

				// loop over each security object and add it to the userCapabilityPolicies
				addSecurityObject(securityObjectsRoles, "role",
						xmlCapabilities.toArray(new UserCapabilityPermission[0]));
				addSecurityObject(securityObjectsGroups, "group",
						xmlCapabilities.toArray(new UserCapabilityPermission[0]));
				addSecurityObject(securityObjectsUsers, "user",
						xmlCapabilities.toArray(new UserCapabilityPermission[0]));
			}
		} catch (Exception e) {
			throw new ConfigException(e.getMessage());
		}

	}

	/**
	 * Small function to help add securityObject to a HashMap
	 */
	private void addSecurityObject(String[] securityObjects, String tag,
			UserCapabilityPermission[] userCapabilityPermissions) {
		// loop over all securityObjects
		for (String securityObject : securityObjects) {
			HashMap<String, String> sO = new HashMap<>();
			// add tag and value
			sO.put(tag, securityObject);

			// put value to userCapabilityPolicies
			this.userCapabilitiesPolicies.put(sO, userCapabilityPermissions);
		}
	}

	/**
	 * @return
	 */
	public String getTargetName() {
		return this.targetName;
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

	public HashMap<HashMap<String, String>, UserCapabilityPermission[]> getUserCapabilitiesPolicies() {
		return userCapabilitiesPolicies;
	}

	public void setUserCapabilitiesPolicies(
			HashMap<HashMap<String, String>, UserCapabilityPermission[]> userCapabilitiesPolicies) {
		this.userCapabilitiesPolicies = userCapabilitiesPolicies;
	}

}
