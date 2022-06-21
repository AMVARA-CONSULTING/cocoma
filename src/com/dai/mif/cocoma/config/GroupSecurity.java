/**
 * $Id: GroupSecurity.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class GroupSecurity extends AbstractSecurityObject {

    public static final String RECORD_KEY_PREFIX = "security.groups.group";

    private Logger log;

    private List<String> roleMembers;
    private List<String> groupMembers;
    private List<String> userMembers;

    private List<String> removedGroupMembers;
    private List<String> removedRoleMembers;
    private List<String> removedUserMembers;

    /**
     * @param name
     */
    public GroupSecurity(String name) {
        super(name);
        init();
    }

    /**
     * @param name
     * @param enabled
     */
    public GroupSecurity(String name, boolean enabled) {
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
        Group group = (Group) getC8Object();
        group.getDisabled().setValue(!enabled);
    }

    /**
     * @see com.dai.mif.cocoma.config.AbstractSecurityObject#readConfig(org.apache.commons.configuration.XMLConfiguration,
     *      java.lang.String)
     */
    @Override
    public void readConfig(XMLConfiguration conf, String configKey) {

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

    }

    /*
     * (non-Javadoc)
     *
     * @see com.dai.mif.cocoma.config.AbstractSecurityObject#createC8Object()
     */
    @Override
    public BaseClass createC8Object() {
        log.debug("Creating Cognos group object");

        Group group = new Group();
        TokenProp nameProp = new TokenProp();
        nameProp.setValue(this.getName());
        group.setDefaultName(nameProp);

        setC8Object(group);

        return group;
    }

    /**
     * @param securityObjects
     */
    public void assignMembers(List<AbstractSecurityObject> securityObjects) {
        Group group = (Group) getC8Object();

        ArrayList<BaseClass> members = new ArrayList<BaseClass>();

        for (AbstractSecurityObject aso : securityObjects) {
            String defaultName = aso.getC8Object().getDefaultName().getValue();
            if (this.roleMembers.contains(defaultName)
                    || this.groupMembers.contains(defaultName)) {
                if (!members.contains(aso.getC8Object())) {
                    members.add(aso.getC8Object());
                }
            }
        }

        group.getMembers().setValue(members.toArray(new BaseClass[] {}));
    }

    /**
     * @return
     */
    public List<String> getUserMembers() {
        return this.userMembers;
    }

    /**
     * @param accounts
     */
    public void addSecurityNamespaceMembers(Account[] accounts) {
        Group group = (Group) getC8Object();
        ArrayList<BaseClass> members = new ArrayList<BaseClass>();

        for (BaseClass bc : group.getMembers().getValue()) {
            members.add(bc);
        }
        for (Account acc : accounts) {
            members.add(acc);
        }
        group.getMembers().setValue(members.toArray(new BaseClass[] {}));
    }

    /**
     * Convenience method to dump the members of this role for debugging
     * purposes
     */
    public void dumpMembers() {
        log.debug("Members of group " + getName() + " (" + roleMembers.size()
                + " roles, " + groupMembers.size() + " groups, "
                + userMembers.size() + " users):");
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
