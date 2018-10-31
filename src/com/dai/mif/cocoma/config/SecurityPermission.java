/**
 * $Id: SecurityPermission.java 92 2010-03-03 15:41:41Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import com.dai.mif.cocoma.exception.ConfigException;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 2, 2010
 * @version $Revision: 92 $ ($Date:: 2010-03-03 16:41:41 +0100#$)
 */
public class SecurityPermission {

    public static final String RECORD_KEY_PREFIX = "security.permissions.permission";

    private String targetName;

    private boolean permissionRead;
    private boolean permissionWrite;
    private boolean permissionExecute;
    private boolean permissionSetPolicy;
    private boolean permissionTraverse;

    private List<String> roleMembers;
    private List<String> groupMembers;
    private List<String> userMembers;

    /**
     * @param targetName
     */
    public SecurityPermission(String targetName) {
        this.targetName = targetName;

        this.roleMembers = new ArrayList<String>();
        this.groupMembers = new ArrayList<String>();
        this.userMembers = new ArrayList<String>();
    }

    /**
     * @param conf
     * @param configKey
     */
    public void readConfig(XMLConfiguration conf, String configKey)
            throws ConfigException {
        try {
            this.permissionRead = conf.getBoolean(configKey + ".read", true);
            this.permissionWrite = conf.getBoolean(configKey + ".write", false);
            this.permissionExecute = conf.getBoolean(configKey + ".execute",
                    true);
            this.permissionSetPolicy = conf.getBoolean(
                    configKey + ".setPolicy", false);
            this.permissionTraverse = conf.getBoolean(configKey + ".traverse",
                    true);

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
        } catch (Exception e) {
            throw new ConfigException(e.getMessage());
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
}
