/**
 * $Id: RoleSecurity.java 123 2010-03-23 10:13:49Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 123 $ ($Date:: 2010-03-23 11:13:49 +0100#$)
 */
public class RoleSecurity extends AbstractSecurityObject {

	public static final String RECORD_KEY_PREFIX = "security.roles.role";

	private Logger log;

	private List<String> roleMembers;
	private List<String> groupMembers;
	private List<String> userMembers;

	private List<String> removedRoleMembers;
	private List<String> removedGroupMembers;
	private List<String> removedUserMembers;

	private boolean keepExistingMembers;

	/**
	 * @param name
	 */
	public RoleSecurity(String name) {
		super(name);
		init();
	}

	/**
	 * @param name
	 * @param enabled
	 */
	public RoleSecurity(String name, boolean enabled) {
		super(name, enabled);
		init();
	}

	/**
	 *
	 */
	private void init() {

		this.log = Logging.getInstance().getLog(this.getClass());

		this.roleMembers = new ArrayList<String>();
		this.groupMembers = new ArrayList<String>();
		this.userMembers = new ArrayList<String>();

		this.removedRoleMembers = new ArrayList<String>();
		this.removedGroupMembers = new ArrayList<String>();
		this.removedUserMembers = new ArrayList<String>();

	}

	/**
	 * @see com.dai.mif.cocoma.config.AbstractSecurityObject#setEnabled(boolean)
	 */
	@Override
	protected void setEnabled(boolean enabled) {
		Role role = (Role) getC8Object();
		role.getDisabled().setValue(!enabled);
	}

	/**
	 * @see com.dai.mif.cocoma.config.AbstractSecurityObject#readConfig(org.apache.commons.configuration.XMLConfiguration,
	 *      java.lang.String)
	 */
	@Override
	public void readConfig(XMLConfiguration conf, String configKey) {

		this.keepExistingMembers = conf.getBoolean(configKey + ".keepExisting", false);

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

		String[] removedRoles = conf.getStringArray(configKey + ".removeMembers.role");
		if (removedRoles != null) {
			for (String role : removedRoles) {
				if (role.length() > 0) {
					removedRoleMembers.add(role);
				}
			}
		}

		String[] removedGroups = conf.getStringArray(configKey + ".removeMembers.group");
		if (removedGroups != null) {
			for (String group : removedGroups) {
				if (group.length() > 0) {
					removedGroupMembers.add(group);
				}
			}
		}

		String[] removedUsers = conf.getStringArray(configKey + ".removeMembers.user");
		if (removedUsers != null) {
			for (String user : removedUsers) {
				if (user.length() > 0) {
					removedUserMembers.add(user);
				}
			}
		}
	}

	/**
	 * @see com.dai.mif.cocoma.config.AbstractSecurityObject#createC8Object()
	 */
	@Override
	public BaseClass createC8Object() {

		log.debug("Creating Cognos role object");

		Role role = new Role();
		TokenProp nameProp = new TokenProp();
		nameProp.setValue(this.getName());
		role.setDefaultName(nameProp);

		setC8Object(role);

		return role;
	}

	/**
	 *
	 * @return
	 */
	public List<String> getRoleMembers() {
		return this.roleMembers;
	}

	/**
	 *
	 * @return
	 */
	public List<String> getGroupMembers() {
		return this.groupMembers;
	}

	/**
	 *
	 * @return
	 */
	public List<String> getUserMembers() {
		return this.userMembers;
	}

	/**
	 * @param securityObjects
	 */
	public void assignMembers(List<AbstractSecurityObject> securityObjects, String query) {
		Role role = (Role) getC8Object();

		ArrayList<BaseClass> members = new ArrayList<BaseClass>();

		if (this.keepExistingMembers) {
			BaseClass[] existingMembers = role.getMembers().getValue();
			if (existingMembers != null) {
				for (BaseClass member : existingMembers) {
					members.add(member);
				}

			}
		}

		for (AbstractSecurityObject aso : securityObjects) {
			String defaultName = aso.getC8Object().getDefaultName().getValue();
			if (this.roleMembers.contains(defaultName) || this.groupMembers.contains(defaultName)) {

				boolean alreadyMember = false;

				for (BaseClass member : members) {
					String memberPath = member.getSearchPath().getValue();
					String asoPath = aso.getC8Object().getSearchPath().getValue();
					if (memberPath.equals(asoPath)) {
						alreadyMember = true;
					}
				}

				if (!alreadyMember) {
					members.add(aso.getC8Object());
				}
			}
		}

		ArrayList<BaseClass> membersToBeRemoved = new ArrayList<BaseClass>();
		for (BaseClass member : members) {
			String memberName = "";
			//
			// Try to get Memeber Name ... could be null
			//
			try {
				if (member instanceof Account) {
					memberName = ((Account) member).getUserName().getValue();
				} else {
					memberName = member.getDefaultName().getValue();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error("Member with Username or Defaultname 'null' found with searchpath "
						+ member.getSearchPath().getValue() + " !!!");
				log.error("You should - review members of " + query + " !!!");
				// e.printStackTrace();
				String msg = "Member with Username or Defaultname 'null' found with searchpath "
						+ member.getSearchPath().getValue() + " in " + query
						+ ". Clean up by hand or use UMA function \"Inkonsistenzen loeschen\".";
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
			}
			if (((member instanceof Role) && this.removedRoleMembers.contains(memberName))
					|| ((member instanceof Group) && this.removedGroupMembers.contains(memberName))
					|| ((member instanceof Account) && this.removedUserMembers.contains(memberName))) {
				membersToBeRemoved.add(member);
			}
		}
		members.removeAll(membersToBeRemoved);

		role.getMembers().setValue(members.toArray(new BaseClass[] {}));
	}

	/**
	 * @param accounts
	 */
	public void addSecurityNamespaceMembers(Account[] accounts) {
		Role role = (Role) getC8Object();
		ArrayList<BaseClass> members = new ArrayList<BaseClass>();

		for (BaseClass bc : role.getMembers().getValue()) {
			members.add(bc);
		}
		for (Account acc : accounts) {
			boolean alreadyMember = false;

			for (BaseClass member : members) {
				String memberPath = member.getSearchPath().getValue();
				String accPath = acc.getSearchPath().getValue();
				if (memberPath.equals(accPath)) {
					alreadyMember = true;
				}
			}

			if (!alreadyMember) {
				members.add(acc);
			}
		}
		role.getMembers().setValue(members.toArray(new BaseClass[] {}));
	}

	/**
	 * Convenience method to dump the members of this role for debugging purposes
	 */
	public void dumpMembers() {
		log.debug("Members of role " + getName() + " (" + roleMembers.size() + " roles, " + groupMembers.size()
				+ " groups, " + userMembers.size() + " users):");
		if (!roleMembers.isEmpty()) {
			String roles = "Roles: ";
			for (String role : roleMembers) {
				roles += role + " ";
			}
			log.debug(roles);
		}
		if (!groupMembers.isEmpty()) {
			String groups = "Groups: ";
			for (String group : groupMembers) {
				groups += group + " ";
			}
			log.debug(groups);
		}
		if (!userMembers.isEmpty()) {
			String users = "Users: ";
			for (String user : userMembers) {
				users += user + " ";
			}
			log.debug(users);
		}
	}

	/**
	 * @return
	 */
	public List<String> getRemovedGroupMembers() {
		return this.removedGroupMembers;
	}

	/**
	 * @return
	 */
	public List<String> getRemovedRoleMembers() {
		return this.removedRoleMembers;
	}

	/**
	 * @return
	 */
	public List<String> getRemovedUserMembers() {
		return this.removedUserMembers;
	}

}
