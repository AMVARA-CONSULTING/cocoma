/**
 * $Id: C8Capabilities.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AccessEnum;
import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BooleanProp;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.Permission;
import com.cognos.developer.schemas.bibus._3.Policy;
import com.cognos.developer.schemas.bibus._3.PolicyArrayProp;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SecuredFeature;
import com.cognos.developer.schemas.bibus._3.SecuredFunction;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.config.CapabilityData;
import com.dai.mif.cocoma.config.SecurityPermission;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 3, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class C8Capabilities {

    private C8Access c8Access;
    private Logger log;
    private C8Utility c8Utiliy;

    /**
     * @param capabilities
     * @param c8Access
     */
    public C8Capabilities(C8Access c8Access) {
        this.c8Access = c8Access;
        this.c8Utiliy = c8Access.getC8Utility();
        this.log = Logging.getInstance().getLog(this.getClass());
    }

    /**
     * Apply the permissions on the secured functions that are represented by
     * the given list of {@link CapabilityData}.
     *
     * @param securedFunctions
     *            List of {@link CapabilityData} representing the secured
     *            functions for which the permissions shall be set.
     */
    public void applySecuredFunctionPermissions(
            List<CapabilityData> securedFunctions) {

        List<String> securedFunctionCache = new ArrayList<String>();

        ContentManagerService_PortType cmService = c8Access.getCmService();
        SearchPathMultipleObject spmo = new SearchPathMultipleObject();
        PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
                PropEnum.searchPath, PropEnum.policies, PropEnum.parent,
                PropEnum.userCapabilities, PropEnum.userCapability,
                PropEnum.userCapabilityPolicies,
                PropEnum.hidden, PropEnum.active};
        Sort[] sort = new Sort[] {};
        QueryOptions QueryOptions = new QueryOptions();
        UpdateOptions updateOptions = new UpdateOptions();

        log.debug("There are permissions to be set on "
                + securedFunctions.size() + " secured functions.");

        for (CapabilityData securedFunctionData : securedFunctions) {

            String securedFunctionName = securedFunctionData.getName();

            log.debug("Setting permissions on secured function: "
                    + securedFunctionName);
            log.debug("==========================================");

            if (log.isDebugEnabled()) {
                debugPermissions(securedFunctionData);
            }

            try {

                spmo.set_value("//securedFunction[@defaultName=\""
                        + securedFunctionName + "\"]");

                BaseClass[] results = cmService.query(spmo, props, sort,
                        QueryOptions);

                int resCount = results.length;

                if (resCount <= 0) {
                    String msg = "The secured function " + securedFunctionName
                            + " could not be found.";
                    CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                    log.error(msg);
                } else if (resCount > 1) {
                    String msg = "The name '"
                            + securedFunctionName
                            + "' for secured functions is ambiguous. Query returned "
                            + resCount + " results.";
                    CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                    log.error(msg);
                } else {

                    SecuredFunction securedFunction = (SecuredFunction) results[0];

                    Permission[] perms = buildPermissions(securedFunctionData);

                    // find the members and set the policies accordingly

                    ArrayList<Policy> policies = new ArrayList<Policy>();

                    // ... for the roles
                    for (String roleName : securedFunctionData.getRoleMembers()) {
                        Role role = c8Utiliy.findRole(roleName);
                        if (role != null) {
                            Policy policy = c8Utiliy.createPolicy(role, perms);
                            policies.add(policy);
                        } else {
                            String msg = "Role " + roleName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    // ... for the groups
                    for (String groupName : securedFunctionData
                            .getGroupMembers()) {
                        Group group = c8Utiliy.findGroup(groupName);
                        if (group != null) {
                            Policy policy = c8Utiliy.createPolicy(group, perms);
                            policies.add(policy);
                        } else {
                            String msg = "Group " + groupName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    // ... and for single users
                    for (String userName : securedFunctionData.getUserMembers()) {
                        Account account = c8Utiliy.findAccount(userName);
                        if (account != null) {
                            Policy policy = c8Utiliy.createPolicy(account,
                                    perms);
                            policies.add(policy);
                        } else {
                            String msg = "Account " + userName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    String securedFunctionPath = securedFunction
                            .getSearchPath().getValue();

                    // if the target is in cache already, there have already
                    // been policy definition during this run, so we have to
                    // extend the existing policies. If the target is not in
                    // cache, we can discard any policies that the target
                    // currently might have.
                    if (securedFunctionCache.contains(securedFunctionPath)) {
                        Policy[] existingPolicies = securedFunction
                                .getPolicies().getValue();
                        for (Policy existingPolicy : existingPolicies) {
                            policies.add(existingPolicy);
                        }
                    } else {
                        // target has not yet been in cache but should be cached
                        // from now on
                        securedFunctionCache.add(securedFunctionPath);
                    }

                    // set the policies to the target
                    PolicyArrayProp policyProp = new PolicyArrayProp();

                    policyProp.setValue(policies.toArray(new Policy[] {}));

                    securedFunction.setPolicies(policyProp);

                    BooleanProp capabilityhidden = new BooleanProp();
                    log.debug("Feature Hidden: "+securedFunctionData.isCapabilityhidden());
                    capabilityhidden.setValue(securedFunctionData.isCapabilityhidden());
                    securedFunction.setHidden(capabilityhidden );

                    // update the target
                    cmService.update(new BaseClass[] { securedFunction },
                            updateOptions);

                    // descend into the features that might be defined for the
                    // current secured function and set their permissions
                    // accordingly
                    log.debug("Now descending into securedFeatures for function: "+securedFunctionName);
                    this.applySecuredFeaturePermissions(securedFunctionData
                            .getCapabilityFeatures());

                }
            } catch (RemoteException re) {
                this.log.debug("Error updating capabilities: "
                        + re.getMessage());
            }
        }

    }

    /**
     * Print information about the given {@link SecurityPermission} object to
     * the debug channel of the current logging object.
     *
     * @param permission
     *            The {@link SecurityPermission} object that is to be debugged.
     */
    private void debugPermissions(CapabilityData permission) {

        String permissionString = "Permissions: (";
        permissionString += "read: " + permission.getPermissionRead() + ", ";
        permissionString += "write: " + permission.getPermissionWrite() + ", ";
        permissionString += "execute: " + permission.getPermissionExecute()
                + ", ";
        permissionString += "setPolicy: " + permission.getPermissionSetPolicy()
                + ", ";
        permissionString += "traverse: " + permission.getPermissionTraverse()
                + ")";

        log.debug(permissionString);

        if (!permission.getGroupMembers().isEmpty()) {
            String groups = "For groups ";
            for (String group : permission.getGroupMembers()) {
                groups += group + " ";
            }
            log.debug(groups);
        }

        if (!permission.getRoleMembers().isEmpty()) {
            String roles = "For roles ";
            for (String role : permission.getRoleMembers()) {
                roles += role + " ";
            }
            log.debug(roles);
        }

        if (!permission.getUserMembers().isEmpty()) {
            String users = "For users ";
            for (String user : permission.getUserMembers()) {
                users += user + " ";
            }
            log.debug(users);
        }
    }

    /**
     * Apply the permissions on the secured features that are represented by the
     * given list of {@link CapabilityData}.
     *
     * @param securedFeatures
     *            List of {@link CapabilityData} representing the secured
     *            features for which the permissions shall be set.
     */
    public void applySecuredFeaturePermissions(
            List<CapabilityData> securedFeatures) {

        List<String> securedFeaturesCache = new ArrayList<String>();

        ContentManagerService_PortType cmService = c8Access.getCmService();
        SearchPathMultipleObject spmo = new SearchPathMultipleObject();
        PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
                PropEnum.searchPath, PropEnum.policies, PropEnum.parent,
                PropEnum.userCapabilities, PropEnum.userCapability,
                PropEnum.userCapabilityPolicies, PropEnum.disabled};
        Sort[] sort = new Sort[] {};
        QueryOptions QueryOptions = new QueryOptions();
        UpdateOptions updateOptions = new UpdateOptions();

        if (securedFeatures.size()>0) {
        	log.debug("------------------------------");
        }
        log.debug("There are permissions to be set on "
                + securedFeatures.size() + " secured features.");
        if (securedFeatures.size()>0) {
        	log.debug("------------------------------");
        }

        for (CapabilityData securedFeatureData : securedFeatures) {
            String securedFeatureName = securedFeatureData.getName();

            log.debug("Setting permissions on secured feature: "
                    + securedFeatureName);

            if (log.isDebugEnabled()) {
                debugPermissions(securedFeatureData);
            }

            try {

                spmo.set_value("//securedFeature[@defaultName=\""
                        + securedFeatureName + "\"]");

                BaseClass[] results = cmService.query(spmo, props, sort,
                        QueryOptions);

                int resCount = results.length;

                if (resCount <= 0) {
                    String msg = "The secured feature " + securedFeatureName
                            + " could not be found.";
                    CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                    log.error(msg);
                } else if (resCount > 1) {
                    String msg = "The name '"
                            + securedFeatureName
                            + "' for secured features is ambiguous. Query returned "
                            + resCount + " results.";
                    CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                    log.error(msg);
                } else {

                    SecuredFeature securedFeature = (SecuredFeature) results[0];

                    Permission[] perms = buildPermissions(securedFeatureData);

                    // find the members and set the policies accordingly

                    ArrayList<Policy> policies = new ArrayList<Policy>();

                    // ... for the roles
                    for (String roleName : securedFeatureData.getRoleMembers()) {
                        Role role = c8Utiliy.findRole(roleName);
                        if (role != null) {
                            Policy policy = c8Utiliy.createPolicy(role, perms);
                            policies.add(policy);
                        } else {
                            String msg = "Role " + roleName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    // ... for the groups
                    for (String groupName : securedFeatureData
                            .getGroupMembers()) {
                        Group group = c8Utiliy.findGroup(groupName);
                        if (group != null) {
                            Policy policy = c8Utiliy.createPolicy(group, perms);
                            policies.add(policy);
                        } else {
                            String msg = "Group " + groupName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    // ... and for single users
                    for (String userName : securedFeatureData.getUserMembers()) {
                        Account account = c8Utiliy.findAccount(userName);
                        if (account != null) {
                            Policy policy = c8Utiliy.createPolicy(account,
                                    perms);
                            policies.add(policy);
                        } else {
                            String msg = "Account " + userName
                                    + " could not be found.";
                            CoCoMa.setErrorCode(
                                    CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
                            log.error(msg);
                        }
                    }

                    String securedFunctionPath = securedFeature.getSearchPath()
                            .getValue();

                    // if the target is in cache already, there have already
                    // been policy definition during this run, so we have to
                    // extend the existing policies. If the target is not in
                    // cache, we can discard any policies that the target
                    // currently might have.
                    if (securedFeaturesCache.contains(securedFunctionPath)) {
                        Policy[] existingPolicies = securedFeature
                                .getPolicies().getValue();
                        for (Policy existingPolicy : existingPolicies) {
                            policies.add(existingPolicy);
                        }
                    } else {
                        // target has not yet been in cache but should be cached
                        // from now on
                        securedFeaturesCache.add(securedFunctionPath);
                    }

                    // set the policies to the target
                    PolicyArrayProp policyProp = new PolicyArrayProp();

                    policyProp.setValue(policies.toArray(new Policy[] {}));

                    securedFeature.setPolicies(policyProp);
                    
                    BooleanProp capabilityHidden = new BooleanProp();
                    log.debug("Hidden: "+securedFeatureData.isCapabilityhidden());
                    capabilityHidden.setValue(securedFeatureData.isCapabilityhidden());
                    securedFeature.setHidden(capabilityHidden );
                    
                    BooleanProp capabilityDisabled = new BooleanProp();
                    capabilityDisabled.setValue(false);
                    securedFeature.setDisabled(capabilityDisabled);

                    // update the target
                    cmService.update(new BaseClass[] { securedFeature },
                            updateOptions);
                    log.debug("Update done");

                }
            } catch (RemoteException re) {
                this.log.debug("Error updating capabilities: "
                        + re.getMessage());
            }
        }

    }

    /**
     * Convenience method to create an array of {@link Permission}. This array
     * defines the permissions for read, write, execute, setPolicy and traverse
     * rights as they are defined in the given {@link CapabilityData} object.
     *
     * @param permissions
     *            {@link CapabilityData} object defining the permissions as they
     *            are defined in the config.
     *
     * @return Array of {@link Permission} object representing exactly the same
     *         permissions as defined in the given argument, but in Cognos
     *         manner.
     */
    private Permission[] buildPermissions(CapabilityData permission) {

        Permission[] c8Permissions = new Permission[5];

        Permission readPerm = new Permission();
        Permission writePerm = new Permission();
        Permission execPerm = new Permission();
        Permission setPolicyPerm = new Permission();
        Permission traversePerm = new Permission();

        readPerm.setName("read");
        if (permission.getPermissionRead()) {
            readPerm.setAccess(AccessEnum.grant);
        } else {
            readPerm.setAccess(AccessEnum.deny);
        }

        writePerm.setName("write");
        if (permission.getPermissionWrite()) {
            writePerm.setAccess(AccessEnum.grant);
        } else {
            writePerm.setAccess(AccessEnum.deny);
        }

        execPerm.setName("execute");
        if (permission.getPermissionExecute()) {
            execPerm.setAccess(AccessEnum.grant);
        } else {
            execPerm.setAccess(AccessEnum.deny);
        }

        setPolicyPerm.setName("setPolicy");
        if (permission.getPermissionSetPolicy()) {
            setPolicyPerm.setAccess(AccessEnum.grant);
        } else {
            setPolicyPerm.setAccess(AccessEnum.deny);
        }

        traversePerm.setName("traverse");
        if (permission.getPermissionTraverse()) {
            traversePerm.setAccess(AccessEnum.grant);
        } else {
            traversePerm.setAccess(AccessEnum.deny);
        }

        c8Permissions[0] = readPerm;
        c8Permissions[1] = writePerm;
        c8Permissions[2] = execPerm;
        c8Permissions[3] = setPolicyPerm;
        c8Permissions[4] = traversePerm;

        return c8Permissions;
    }

}
