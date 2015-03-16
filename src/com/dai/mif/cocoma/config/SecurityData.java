/**
 * $Id: SecurityData.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.Role;
import com.dai.mif.cocoma.exception.CoCoMaConfigException;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class SecurityData {

    private List<AbstractSecurityObject> fixedObjects;

    private List<AbstractSecurityObject> securityObjects;

    private List<SecurityPermission> securityPermissions;

    /**
     * @param conf
     */
    @SuppressWarnings("unchecked")
    public SecurityData(XMLConfiguration conf) throws CoCoMaConfigException {
        fixedObjects = new ArrayList<AbstractSecurityObject>();
        securityObjects = new ArrayList<AbstractSecurityObject>();
        securityPermissions = new ArrayList<SecurityPermission>();

        // ATTENTION
        // the following list of fixed security objects is hard coded as defined
        // by the MIF Reporting security guideline. Any additional objects that
        // need to be kept rather than deleting them in cognos security or any
        // adaptions to names for the fixed objects need to be made here!

        fixedObjects.add(new GroupSecurity("All Authenticated Users"));
        fixedObjects.add(new GroupSecurity("Everyone"));

        fixedObjects.add(new UserSecurity("Anonymous", false));

        RoleSecurity sysAdminRole = new RoleSecurity("System Administrators");
        sysAdminRole.getRemovedGroupMembers().add("Everyone");

        fixedObjects.add(sysAdminRole);
        fixedObjects.add(new RoleSecurity("Report Administrators"));
        fixedObjects.add(new RoleSecurity("Consumers"));

        // read the additional security objects from config

        // --- roles

        List<Object> rolesList = conf.getList(RoleSecurity.RECORD_KEY_PREFIX
                + ".name");
        for (int i = 0; i < rolesList.size(); i++) {
            String roleName = (String) rolesList.get(i);
            String configKey = RoleSecurity.RECORD_KEY_PREFIX + "(" + i + ")";
            RoleSecurity roleData = new RoleSecurity(roleName);
            roleData.readConfig(conf, configKey);
            this.securityObjects.add(roleData);
        }

        // --- groups

        List<Object> groupsList = conf.getList(GroupSecurity.RECORD_KEY_PREFIX
                + ".name");
        for (int i = 0; i < groupsList.size(); i++) {
            String groupName = (String) groupsList.get(i);
            String configKey = GroupSecurity.RECORD_KEY_PREFIX + "(" + i + ")";
            GroupSecurity groupData = new GroupSecurity(groupName);
            groupData.readConfig(conf, configKey);
            this.securityObjects.add(groupData);
        }

        // --- permissions

        List<Object> permissionsList = conf
                .getList(SecurityPermission.RECORD_KEY_PREFIX + ".target");
        for (int i = 0; i < permissionsList.size(); i++) {
            String targetName = (String) permissionsList.get(i);
            String configKey = SecurityPermission.RECORD_KEY_PREFIX + "(" + i
                    + ")";
            SecurityPermission permissionData = new SecurityPermission(
                    targetName);
            permissionData.readConfig(conf, configKey);
            this.securityPermissions.add(permissionData);
        }
    }

    /**
     *
     * @return
     */
    public List<AbstractSecurityObject> getFixedObjects() {
        return this.fixedObjects;
    }

    /**
     * @param bc
     * @return
     */
    public AbstractSecurityObject getFixedObject(BaseClass bc) {

        AbstractSecurityObject result = null;

        for (AbstractSecurityObject aso : this.fixedObjects) {
            if (((bc instanceof Role) && (aso instanceof RoleSecurity))
                    || ((bc instanceof Group) && (aso instanceof GroupSecurity))
                    || ((bc instanceof Account) && (aso instanceof UserSecurity))) {

                if (aso.getName().equals(bc.getDefaultName().getValue())) {
                    result = aso;
                }

            }
        }

        return result;
    }

    /**
     * @return
     */
    public List<AbstractSecurityObject> getSecurityObjects() {
        return this.securityObjects;
    }

    /**
     * @return
     */
    public List<SecurityPermission> getSecurityPermissions() {
        return this.securityPermissions;
    }

}
