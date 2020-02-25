/**
 * $Id: C8Security.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AccessEnum;
import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.IntProp;
import com.cognos.developer.schemas.bibus._3.Permission;
import com.cognos.developer.schemas.bibus._3.Policy;
import com.cognos.developer.schemas.bibus._3.PolicyArrayProp;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.RefProp;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.config.AbstractSecurityObject;
import com.dai.mif.cocoma.config.GroupSecurity;
import com.dai.mif.cocoma.config.RoleSecurity;
import com.dai.mif.cocoma.config.SecurityData;
import com.dai.mif.cocoma.config.SecurityPermission;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class encapsulated the C8n security routines. Based on a given
 * SecurityData object, the Cognos Security is modified and enhanced.
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class CognosSecurity {

	private Logger log;

	private SecurityData securityData;

	private C8Access c8Access;

	private C8Utility c8Utility;

	/**
	 * Constructor for the C8Security class.
	 *
	 * @param secData  {@link SecurityData} object defining all security settings to
	 *                 be applied on the Cognos system.
	 * @param c8Access The {@link C8Access} object to be used for accessing the
	 *                 content store.
	 */
	public CognosSecurity(SecurityData secData, C8Access c8Access) {

		this.log = Logging.getInstance().getLog(this.getClass());
		this.securityData = secData;
		this.c8Access = c8Access;
		this.c8Utility = c8Access.getC8Utility();
	}

	/**
	 * Apply the security data that is defined by the {@link SecurityData} object
	 * held as member of this class to the C8 system. Users, Roles and Groups are
	 * modified and extended.
	 */
	public void applyCognosSecurity() throws CoCoMaC8Exception {
		// purge Cognos security and only keep the given fixed objects
		List<AbstractSecurityObject> fixedObjects = this.securityData.getFixedObjects();
		purgeCognosSecurity(fixedObjects);

		// create new Cognos security objects
		List<AbstractSecurityObject> securityObjects = this.securityData.getSecurityObjects();
		extendCognosSecurityObjects(securityObjects);

	}

	/**
	 * Apply the security data that is defined by the {@link SecurityData} field on
	 * the content of the C8 system. Permissions on folders and packages are set
	 * according to the current configuration.
	 */
	public void applyContentSecurity() throws CoCoMaC8Exception {
		// set content permissions as defined in the config
		List<SecurityPermission> permissions = this.securityData.getSecurityPermissions();
		setContentPermissions(permissions);
	}

	/**
	 * Set the permission on the content elements (folders, packages) in cognos
	 * according to the {@link SecurityPermission} objects given as argument.
	 *
	 * @param permissions The {@link SecurityPermission} objects to be applied
	 *                    succesively to the C8 system.
	 */
	private void setContentPermissions(List<SecurityPermission> permissions) {

		List<String> targetCache = new ArrayList<String>();

		for (SecurityPermission permission : permissions) {

			if (log.isDebugEnabled()) {
				debugPermissions(permission);
			}

			// prepare the query

			ContentManagerService_PortType cms = this.c8Access.getCmService();
			SearchPathMultipleObject spmo = new SearchPathMultipleObject(permission.getTargetName());
			PropEnum[] props = new PropEnum[] { PropEnum.policies, PropEnum.defaultName, PropEnum.searchPath };
			Sort[] sort = new Sort[] {};
			QueryOptions queryOptions = new QueryOptions();
			UpdateOptions updateOptions = new UpdateOptions();

			try {
				// find the target

				BaseClass[] bcs = cms.query(spmo, props, sort, queryOptions);
				if (bcs.length == 1) {

					BaseClass target = bcs[0];

					// build the permissions array
					Permission[] c8Permissions = buildPermissions(permission);

					// find the members and set the policies accordingly

					ArrayList<Policy> policies = new ArrayList<Policy>();

					// ... for the roles
					for (String roleName : permission.getRoleMembers()) {
						Role role = c8Utility.findRole(roleName);
						if (role != null) {
							Policy policy = c8Utility.createPolicy(role, c8Permissions);
							policies.add(policy);
						} else {
							String msg = "Role \"" + roleName + "\" could not be found.";
							CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
							log.error(msg);
						}
					}

					// ... for the groups
					for (String groupName : permission.getGroupMembers()) {
						Group group = c8Utility.findGroup(groupName);
						if (group != null) {
							Policy policy = c8Utility.createPolicy(group, c8Permissions);
							policies.add(policy);
						} else {
							String msg = "Group \"" + groupName + "\"c ould not be found.";
							CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
							log.error(msg);
						}
					}

					// ... and for single users
					for (String userName : permission.getUserMembers()) {
						Account account = c8Utility.findAccount(userName);
						if (account != null) {
							Policy policy = c8Utility.createPolicy(account, c8Permissions);
							policies.add(policy);
						} else {
							String msg = "Account \"" + userName + "\" could not be found.";
							CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
							log.error(msg);
						}
					}

					String targetPath = target.getSearchPath().getValue();

					// if the target is in cache already, there have already
					// been policy definition during this run, so we have to
					// extend the existing policies. If the target is not in
					// cache, we can discard any policies that the target
					// currently might have.
					if (targetCache.contains(targetPath)) {
						Policy[] existingPolicies = target.getPolicies().getValue();
						for (Policy existingPolicy : existingPolicies) {
							policies.add(existingPolicy);
						}
					} else {
						// target has not yet been in cache but should be cached
						// from now on
						targetCache.add(targetPath);
					}

					// set the policies to the target
					PolicyArrayProp policyProp = new PolicyArrayProp();

					policyProp.setValue(policies.toArray(new Policy[] {}));

					target.setPolicies(policyProp);

					// update the target
					cms.update(new BaseClass[] { target }, updateOptions);

				} else if (bcs.length > 1) {
					String msg = "Permission target " + permission.getTargetName() + " is ambiguous";
					CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
					log.error(msg);
				} else {
					String msg = "Permission target " + permission.getTargetName() + " does not exist";
					CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);
					log.error(msg);
				}
			} catch (RemoteException e) {
				String msg = "Error setting permissions on " + permission.getTargetName() + ": " + e.getMessage();
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);

				log.error(msg);
			}

		}

	}

	/**
	 * Convenience method to create an array of {@link Permission}. This array
	 * defines the permissions for read, write, execute, setPolicy and traverse
	 * rights as they are defined in the given {@link SecurityPermission} object.
	 *
	 * @param permissions {@link SecurityPermission} object defining the permissions
	 *                    as they are defined in the config.
	 *
	 * @return Array of {@link Permission} object representing exactly the same
	 *         permissions as defined in the given argument, but in Cognos manner.
	 */
	private Permission[] buildPermissions(SecurityPermission permission) {
		// prepare the permissions on this target
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

	/**
	 * Print information about the given {@link SecurityPermission} object to the
	 * debug channel of the current logging object.
	 *
	 * @param permission The {@link SecurityPermission} object that is to be
	 *                   debugged.
	 */
	private void debugPermissions(SecurityPermission permission) {
		log.debug("Setting permissions on " + permission.getTargetName());

		String permissionString = "Permissions: (";
		permissionString += "read: " + permission.getPermissionRead() + ", ";
		permissionString += "write: " + permission.getPermissionWrite() + ", ";
		permissionString += "execute: " + permission.getPermissionExecute() + ", ";
		permissionString += "setPolicy: " + permission.getPermissionSetPolicy() + ", ";
		permissionString += "traverse: " + permission.getPermissionTraverse() + ")";

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
	 * Add new elements to the Cognos security
	 *
	 * @param securityObjects List of {@link AbstractSecurityObject} instances that
	 *                        are to be added to the Cognos security.
	 */
	private void extendCognosSecurityObjects(List<AbstractSecurityObject> securityObjects) {
		ContentManagerService_PortType cms = this.c8Access.getCmService();

		SearchPathSingleObject spso = new SearchPathSingleObject();
		SearchPathMultipleObject spmo = new SearchPathMultipleObject();

		PropEnum[] props = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName, PropEnum.members };

		AddOptions addOptions = new AddOptions();
		addOptions.setReturnProperties(props);
		addOptions.setUpdateAction(UpdateActionEnum.replace);

		try {

			// add the new security objects as they are defined in the config

			log.debug("Adding new security objects.");

			String query = "";

			for (AbstractSecurityObject aso : securityObjects) {
				BaseClass bc = aso.createC8Object();

				if (bc != null) {

					String parent = "CAMID(\":\")";
					query = "//" + bc.getClass().getSimpleName().toLowerCase() + "[@name='" + aso.getName() + "']";

					spmo.set_value(query);

					QueryOptions queryOptions = new QueryOptions();

					RefProp membersProp = new RefProp();
					membersProp.setRefPropName(PropEnum.members);
					membersProp.setProperties(
							new PropEnum[] { PropEnum.defaultName, PropEnum.userName, PropEnum.searchPath });

					RefProp[] refProps = new RefProp[] { membersProp };

					queryOptions.setRefProps(refProps);

					String checkTarget = bc.getClass().getSimpleName() + " '" + aso.getName() + "'";
					log.debug("Checking if " + checkTarget + " already exists.");

					BaseClass[] bcs = cms.query(spmo, props, new Sort[] {}, queryOptions);

					boolean targetExists = false;
					if (bcs.length < 1) {
						log.debug(checkTarget + " does not exist yet.");
					} else if (bcs.length > 1) {
						log.debug(checkTarget + " is ambiguous. " + bcs.length + " results.");
					} else {
						log.debug(checkTarget + " already exists.");
						targetExists = true;
						aso.setC8Object(bcs[0]);
					}

					if (!targetExists) {
						log.debug("Adding " + bc.getClass().getSimpleName() + " " + aso.getName()
								+ " to Cognos namespace");

						spso.set_value(parent);

						bcs = cms.add(spso, new BaseClass[] { bc }, addOptions);
						if (bcs.length == 1) {
							aso.setC8Object(bcs[0]);
						} else {
							String msg = "Error creating security object. Not exactly one element returned.";
							CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
							log.error(msg);
						}
					}

					// Check if name of object is QSWorklistUser
					// remove UI Features "+" from userProfileSettings
					if (aso.getName().equalsIgnoreCase("QSWorklistUser")) {
						removePlusButtonsUiFeatureFromRole(c8Utility, aso.getName());
					}

				} else {
					String msg = "Could not create Cognos object for " + aso.getClass().getSimpleName() + " "
							+ aso.getName();
					CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
					log.error(msg);
				}
			}

			log.debug("Finished adding new security objects.");

			// now that we can be sure that all new objects are available,
			// set the members of the newly created objects

			ArrayList<BaseClass> modifiedSecurityObjects = new ArrayList<BaseClass>();

			log.debug("Extending new security objects.");

			for (AbstractSecurityObject aso : securityObjects) {

				BaseClass bc = aso.getC8Object();

				if (aso instanceof RoleSecurity) {
					RoleSecurity rs = (RoleSecurity) aso;
					query = "//" + bc.getClass().getSimpleName().toLowerCase() + "[@name='" + aso.getName() + "']";
					rs.assignMembers(securityObjects, query);

					ArrayList<Account> accounts = new ArrayList<Account>();
					for (String userName : rs.getUserMembers()) {
						Account acc = c8Utility.findAccount(userName);
						if (acc != null) {
							accounts.add(acc);
						}
					}
					if (!accounts.isEmpty()) {
						rs.addSecurityNamespaceMembers(accounts.toArray(new Account[] {}));
					}

					if (log.isDebugEnabled()) {
						rs.dumpMembers();
					}

				} else if (aso instanceof GroupSecurity) {
					GroupSecurity gs = (GroupSecurity) aso;
					gs.assignMembers(securityObjects);

					ArrayList<Account> accounts = new ArrayList<Account>();
					for (String userName : gs.getUserMembers()) {
						Account acc = c8Utility.findAccount(userName);
						if (acc != null) {
							accounts.add(acc);
						}
					}
					if (!accounts.isEmpty()) {
						gs.addSecurityNamespaceMembers(accounts.toArray(new Account[] {}));
					}
					if (log.isDebugEnabled()) {
						gs.dumpMembers();
					}
				}

				modifiedSecurityObjects.add(bc);
			}

			log.debug("Finished extending new security objects.");

			log.debug("Updating extended security objects.");

			UpdateOptions updateOptions = new UpdateOptions();

			BaseClass[] bcs = cms.update(modifiedSecurityObjects.toArray(new BaseClass[] {}), updateOptions);
			if (bcs.length != modifiedSecurityObjects.size()) {
				String msg = "Error updating extended security objects. " + bcs.length + " of "
						+ modifiedSecurityObjects.size() + " have been updated.";
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
				log.error(msg);
			} else {
				log.debug("Finished updating extended security objects.");
			}

		} catch (RemoteException e) {
			log.debug("Error extending Cognos security: " + e.getMessage());
		}

	}

	private void removePlusButtonsUiFeatureFromRole(C8Utility ch, String RoleName) {

		BaseClass[] bcResult = ch.fetchObjectsWithQueryOptions("CAMID(':" + RoleName + "')", ch.setPropEnum(),
				new Sort[] {}, ch.setQORefProps());

		log.debug("Found Results:" + bcResult.length);
		Role myRole = ((Role) bcResult[0]);
		String onlineRoleProfileSettings = myRole.getUserProfileSettings().getValue();
		log.debug("Active Settings:" + onlineRoleProfileSettings);

		String myDefaultUserProfileSettings = "{\"ui_excludedFeatures\":{\"ids\":[\"com.ibm.bi.glass.common.createMenu\",\"com.ibm.bi.authoring.createTemplate\",\"com.ibm.bi.dashboard.createDashboard\",\"com.ibm.bi.storytelling.createStory\",\"com.ibm.bi.ca-modeller.ca-modeller\",\"com.ibm.bi.glass.common.createOther\",\"com.ibm.bi.ca-uploadModeller.ca-modeller-glass-upload\",\"com.ibm.admin.jobs.new_job\"]}}";
		log.debug("Default Settings:" + myDefaultUserProfileSettings);

		if (!myDefaultUserProfileSettings.equalsIgnoreCase(onlineRoleProfileSettings)) {
			log.debug("UserProfile Settings needs to be updated");
			AnyTypeProp userProfileSettings = myRole.getUserProfileSettings();
			userProfileSettings.setValue(myDefaultUserProfileSettings);
			Role updatedRole = new Role();
			updatedRole.setSearchPath(myRole.getSearchPath());
			updatedRole.setUserProfileSettings(userProfileSettings);
			// Set priority of role to "2" ... to overrule Query Users role
			IntProp i = myRole.getProfileRank();
			i.setValue(2);
			updatedRole.setProfileRank(i);
			try {
				BaseClass[] updatedItems = c8Access.getCmService().update(new BaseClass[] { updatedRole },
						new UpdateOptions());
				if (updatedItems.length > 0) {
					log.info("Successfully updated " + updatedRole.getSearchPath().getValue()
							+ " removenig the \"+\" button from UI and setting profile Priority to 2.");
					return;
				}
			} catch (java.rmi.RemoteException remoteEx) {
				remoteEx.printStackTrace();
				System.out.println("Exception Caught:\n" + remoteEx.getMessage());
				return;
			}
		}

	}

	/**
	 * Purge the Cognos security by removing ALL BUT the given fixed objects.
	 * Additionally the remaining fixed security objects are modified and saved in
	 * Cognos security.
	 *
	 * @param fixedObjects List of {@link AbstractSecurityObject} instances defining
	 *                     those objects from the Cognos security that shall not be
	 *                     removed. Currently this list is defined in the
	 *                     {@link SecurityData} class.
	 */
	private void purgeCognosSecurity(List<AbstractSecurityObject> fixedObjects) throws CoCoMaC8Exception {

		List<BaseClass> updatedObjects = new ArrayList<BaseClass>();
		List<BaseClass> deletedObjects = new ArrayList<BaseClass>();

		ContentManagerService_PortType cms = this.c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject();
		PropEnum[] props = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName, PropEnum.disabled };
		Sort[] sort = new Sort[] {};
		QueryOptions options = new QueryOptions();

		try {
			spmo.set_value("CAMID(\":\")//role|CAMID(\":\")//group|CAMID(\":\")//account");
			BaseClass[] results = cms.query(spmo, props, sort, options);

			int fixedCount = 0;
			int deactivedCount = 0;

			for (BaseClass bc : results) {
				log.debug(bc.getClass().getSimpleName() + ": " + bc.getDefaultName().getValue() + " - Searchpath:"
						+ bc.getSearchPath().getValue());

				AbstractSecurityObject aso = securityData.getFixedObject(bc);
				if (aso != null) {
					fixedCount++;
					log.debug("---> is a fixed object");
					aso.setC8Object(bc);
					if (!aso.isEnabled()) {
						deactivedCount++;
						log.debug("---> object will be deactivated");
						aso.disable();
						updatedObjects.add(aso.getC8Object());
					}

				} else {
					log.debug("<--- not a fixed object, and will be deleted");
					deletedObjects.add(bc);
				}
			}

			log.info(fixedCount + " security objects will be kept (" + deactivedCount + " deactived)");

			// if there have been changes to the existing objects, update them
			if (!updatedObjects.isEmpty()) {

				log.info("Updating modified security objects (" + updatedObjects.size() + ")");

				UpdateOptions updateOptions = new UpdateOptions();
				cms.update(updatedObjects.toArray(new BaseClass[] {}), updateOptions);

			}

			// if there are objects that need to be deleted, actually delete
			// them now
			if (!deletedObjects.isEmpty()) {

				log.info("Deleting unwanted security objects (" + deletedObjects.size() + ")");

				DeleteOptions deleteOptions = new DeleteOptions();
				deleteOptions.setRecursive(true);
				cms.delete(deletedObjects.toArray(new BaseClass[] {}), deleteOptions);

			}

		} catch (RemoteException e) {
			log.debug("Error purging Cognos security: " + e.getMessage());
		}

	}

}
