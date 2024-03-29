/**
 * $Id: C8Utility.java 169 2011-09-01 07:29:00Z rroeber $
 */
package com.dai.mif.cocoma.cognos.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.Content;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.Dispatcher_Type;
import com.cognos.developer.schemas.bibus._3.Folder;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.Permission;
import com.cognos.developer.schemas.bibus._3.PingReply;
import com.cognos.developer.schemas.bibus._3.Policy;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.RefProp;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.UserCapabilityPermission;
import com.cognos.developer.schemas.bibus._3.UserCapabilityPolicy;
import com.cognos.developer.schemas.bibus._3._package;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since Mar 3, 2010
 * @version $Revision: 169 $ ($Date:: 2011-09-01 09:29:00 +0200#$)
 */
public class C8Utility {

	private C8Access c8Access;
	private Logger log;

	private ContentManagerService_PortType cmService = null;

	public C8Utility(C8Access c8Access) {
		this.c8Access = c8Access;
		this.log = Logging.getInstance().getLog(this.getClass());
	}

	// Constructor for the SecurityOverview Class
	/**
	 * Convenience method to find Cognos objects in {@link searchpath} with the
	 * given name within the Cognos namespace.
	 *
	 * @param searchPath The name of the {@link searchPath} that is to be found.
	 *
	 * @return BaseClass[] results with the given name, or NULL if the group could
	 *         not be found.
	 */
	public BaseClass[] findObjectsInSearchPath(String searchPath) {

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(searchPath);
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.members,
				PropEnum.parent };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results = null;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			log.debug("The query for the searched group returned " + results.length + " results.");
		} catch (RemoteException e) {
			String msg = "Error querying group " + searchPath + ": " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return results;
	}

	/**
	 * Convenience method to find a Cognos {@link Group} with the given name within
	 * the Cognos namespace.
	 *
	 * @param groupName The name of the {@link Group} that is to be found.
	 *
	 * @return {@link Group} with the given name, or NULL if the group could not be
	 *         found.
	 */
	public Group findGroup(String groupName) {
		Group theGroup = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject("//group[@defaultName=\"" + groupName + "\"]");
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.members };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			if (results.length == 1) {
				theGroup = (Group) results[0];
			} else {
				log.debug("The query for the searched group returned " + results.length + " results.");
			}
		} catch (RemoteException e) {
			String msg = "Error querying group " + groupName + ": " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return theGroup;
	}

	/**
	 * Convenience method to find a Cognos {@link Account} with the given name
	 * within the security namespace that is defined in the config.
	 *
	 * @param userName The name of the {@link Account} that is to be found.
	 *
	 * @return {@link Account} with the given name, or NULL if the account could not
	 *         be found.
	 */
	public Account findAccount(String userName) {

		Account theAccount = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		// FIXME As a workaround the account has to be searched manually. For
		// some reason a query of the form //account[@userName="foo"] does not
		// return the account as it normally should do.

		SearchPathMultipleObject spmo = new SearchPathMultipleObject("/directory/namespace");

		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name, PropEnum.defaultName, PropEnum.searchPath,
				PropEnum.options };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] namespaces;
		BaseClass[] results;

		// userName in different cases
		ArrayList<String> caseInSensitiveUserNames = new ArrayList<>();
		caseInSensitiveUserNames.add(userName);
		caseInSensitiveUserNames.add(userName.toLowerCase());
		caseInSensitiveUserNames.add(userName.toUpperCase());

		try {

			namespaces = cms.query(spmo, props, sort, queryOptions);

			for (BaseClass namespace : namespaces) {

				// check if there is a user with different cases of userName
				for (String caseInSensitiveUserName : caseInSensitiveUserNames) {

					String nsSearchPath = namespace.getSearchPath().getValue();
					spmo.set_value(nsSearchPath + "//account[@userName=\"" + caseInSensitiveUserName + "\"]");
					// spmo.setValue(nsSearchPath + "//account");

					results = cms.query(spmo, props, sort, queryOptions);

					int i = 0;

					for (BaseClass bc : results) {

						Account acc = (Account) bc;
						if (acc != null) {

							String accName = acc.getUserName().getValue();

							if (accName != null) {
								if (accName.equals(caseInSensitiveUserName)) {
									// output that we did not found a userName but we did found a user with
									// different letter case
									if (!caseInSensitiveUserName.equals(userName)) {
										log.info("Could not find user with name \"" + userName
												+ "\", but did find user with name \"" + caseInSensitiveUserName
												+ "\". Will use this one.");
									}
									return acc;
								}
							}
						} else {
							log.info("Account at position " + i + " is NULL!");
						}
						i++;
					}

				}

			}

		} catch (RemoteException e) {
			String msg = "Error querying account " + userName + ": " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		if (theAccount == null) {
			String msg = "An account for the user name " + userName
					+ " could not be found within the defined namespaces.";
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return theAccount;
	}

	/**
	 * Convenience method to find a Cognos {@link Role} with the given name within
	 * the Cognos namespace.
	 *
	 * @param roleName The name of the {@link Role} that is to be found.
	 *
	 * @return {@link Role} with the given name, or NULL if the role could not be
	 *         found.
	 */
	public Role findRole(String roleName) {

		Role theRole = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject("//role[@defaultName=\"" + roleName + "\"]");
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.members };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			if (results.length == 1) {
				theRole = (Role) results[0];
			} else {
				log.debug(
						"The query for the searched role [" + roleName + "] returned " + results.length + " results.");
			}
		} catch (RemoteException e) {
			String msg = "Error querying role " + roleName + ": " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return theRole;
	}

	/**
	 * Convenience method to create a {@link Policy} object on the given target
	 * holding the permissions provided by the given array of {@link Permission} .
	 *
	 * @param target        The target as {@link BaseClass} that the permission
	 *                      shall be applied on.
	 * @param c8Permissions Array of {@link Permission} objects defining the
	 *                      permissions for read, write, execute, setPolicy and
	 *                      traverse.
	 *
	 * @return {@link Policy} object encapsulating the permissions on the given
	 *         target.
	 */
	public Policy createPolicy(BaseClass target, Permission[] c8Permissions) {
		Policy policy = new Policy();
		policy.setSecurityObject(target);
		policy.setPermissions(c8Permissions);
		return policy;
	}

	/**
	 * Convenience method for testing purposes. While working on CoCoMa the problem
	 * has come up, that various accounts within the D4 namespace do nor have a
	 * proper userName set no is the defaultName or any localized name filled with
	 * the proper userName. This method queries all accounts from the security
	 * namespace and dumps the relevant account data to the logging debug channel.
	 */
	public void dumpAccounts() {

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject("//account");
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name, PropEnum.searchPath,
				PropEnum.defaultName };
		Sort[] sort = new Sort[] {};
		QueryOptions options = new QueryOptions();

		try {
			BaseClass[] results = cms.query(spmo, props, sort, options);

			if (results.length < 1) {
				log.error("Query for accounts did not return any results");
			} else {
				for (BaseClass bc : results) {
					Account acc = (Account) bc;

					String accData = "\nAccount: " + acc.getSearchPath().getValue() + "\n\tuserName: "
							+ acc.getUserName().getValue() + "\n\tdefaultName: " + acc.getDefaultName().getValue();

					MultilingualToken[] mlts = acc.getName().getValue();
					for (MultilingualToken mlt : mlts) {
						accData += "\n\tname (" + mlt.getLocale() + "): " + mlt.getValue();
					}

					log.debug(accData);

				}
			}

		} catch (RemoteException e) {
			log.error("Error querying account data. " + e.getMessage());
		}

	}

	/**
	 * Return the detailed error message of a RemoteException
	 *
	 * @return
	 */
	public String parseAxisFault(AxisFault af) {
		String message = "";
		String axisMessage = af.dumpToString();

		int start = axisMessage.indexOf("<messageString>");
		int end = axisMessage.indexOf("</messageString>");

		if (start < end) {
			message += axisMessage.substring(start + 15, end - 1);
		} else {
			message = af.getMessage();
		}

		return message;
	}

	public Boolean removeItems(String deleteItem) {
		ContentManagerService_PortType cmService = this.c8Access.getCmService();
		Boolean result = true;
		// Set searchPath
		SearchPathMultipleObject searchPobj = new SearchPathMultipleObject();
		searchPobj.set_value(deleteItem);
		// Set delete options
		DeleteOptions del = new DeleteOptions();
		del.setForce(true);
		del.setRecursive(true);
		try {
			BaseClass delObject[] = null;
			delObject = cmService.query(searchPobj, new PropEnum[] {}, new Sort[] {}, new QueryOptions());
			cmService.delete(delObject, del);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			log.error("!!! Severe Error !!!");
			log.error("Could not remove:" + deleteItem);
			log.error(e.getStackTrace());
			result = false;
		}
		return result;

	}

	public void queryDispatcher() {
		log = Logging.getInstance().getLog(this.getClass());
		try {

			// Create a Search Path that eliminates all but the dispatchers
			String searchPath = "/configuration/dispatcher";

			PropEnum[] confprop = { PropEnum.brsAffineConnections, PropEnum.brsAuditLevel, PropEnum.brsAuditNativeQuery,
					PropEnum.brsMaximumProcesses, PropEnum.brsNonAffineConnections, PropEnum.capacity,
					PropEnum.dispatcherAuditLevel, PropEnum.dispatcherPath, PropEnum.jsAuditLevel,
					PropEnum.msNonPeakDemandBeginHour, PropEnum.msNonPeakDemandMaximumTasks,
					PropEnum.msPeakDemandBeginHour, PropEnum.msPeakDemandMaximumTasks, PropEnum.rsAuditLevel,
					PropEnum.rsAuditNativeQuery, PropEnum.rsAffineConnections, PropEnum.rsMaximumProcesses,
					PropEnum.rsNonAffineConnections, PropEnum.rsQueueLimit, PropEnum.runningState, PropEnum.serverGroup,
					PropEnum.state, PropEnum.applicationGUID, PropEnum.applicationID, PropEnum.applicationURL };

			cmService = this.c8Access.getCmService();
			String activeCM = cmService.getActiveContentManager();

			log.info("--------------------------------------------------------------");
			log.info("Active ContentManager: " + activeCM);
			log.info(" ... this is the Dispatcher with the active ContentManager. Use it for deployments.");

			BaseClass[] dispatchers = cmService.query(new SearchPathMultipleObject(searchPath), confprop, new Sort[] {},
					new QueryOptions());

			// Print each object search path and class type
			for (int disp_index = 0; disp_index < dispatchers.length; disp_index++) {
				// For each dispatcher, identify the dispatcher with a number
				// and then output all information that defines that dispatcher
				// in terms of characteristics.

				Dispatcher_Type dispatch = (Dispatcher_Type) dispatchers[disp_index];
				int dispnum = disp_index + 1; // Convert from zero based.

				log.debug("");
				OutputDispatcherInformation(dispatch, dispnum);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// OutputDispatcherInformation: This is a procedure used to output all
	// information
	// that was specified by the PropEnum.

	/**
	 * Description: set the most common available Enums to a Cognos BaseClass, e.g.
	 * DefaultName, ancestors, detailTime, ... Technote 1341976
	 * 
	 * @author RROEBER
	 * @return
	 */
	public PropEnum[] setPropEnum() {
		PropEnum properties[] = new PropEnum[] { PropEnum.active, PropEnum.actualCompletionTime,
				PropEnum.actualExecutionTime, PropEnum.advancedSettings, PropEnum.ancestors, PropEnum.asOfTime,
				PropEnum.base, PropEnum.brsMaximumProcesses, PropEnum.brsNonAffineConnections, PropEnum.burstKey,
				PropEnum.businessPhone, PropEnum.canBurst, PropEnum.capabilities, PropEnum.capacity,
				PropEnum.connections, PropEnum.connectionString, PropEnum.consumers, PropEnum.contact,
				PropEnum.contactEMail, PropEnum.contentLocale, PropEnum.creationTime, PropEnum.credential,
				PropEnum.credentialNamespaces, PropEnum.credentials, PropEnum.dailyPeriod, PropEnum.data,
				PropEnum.dataSize, PropEnum.dataType, PropEnum.defaultDescription, PropEnum.defaultName,
				PropEnum.defaultOutputFormat, PropEnum.defaultScreenTip, PropEnum.defaultTriggerDescription,
				PropEnum.deployedObject, PropEnum.deployedObjectAncestorDefaultNames, PropEnum.deployedObjectClass,
				PropEnum.deployedObjectDefaultName, PropEnum.deployedObjectStatus, PropEnum.deployedObjectUsage,
				// PropEnum.deploymentOptions,
				PropEnum.description, PropEnum.detail, PropEnum.detailTime, PropEnum.disabled, PropEnum.dispatcherID,
				PropEnum.dispatcherPath, PropEnum.displaySequence, PropEnum.email, PropEnum.endDate, PropEnum.endType,
				PropEnum.eventID, PropEnum.everyNPeriods, PropEnum.executionFormat, PropEnum.executionLocale,
				PropEnum.executionPageDefinition, PropEnum.executionPageOrientation, PropEnum.executionPrompt,
				PropEnum.faxPhone, PropEnum.format, PropEnum.givenName, PropEnum.governors, PropEnum.hasChildren,
				PropEnum.hasMessage, PropEnum.height, PropEnum.homePhone, PropEnum.horizontalElementsRenderingLimit,
				PropEnum.identity, PropEnum.isolationLevel,
				// PropEnum.msNonPeakDemandBeginHour,
				/*
				 * PropEnum.msNonPeakDemandMaximumTasks, PropEnum.msPeakDemandBeginHour,
				 * PropEnum.msNonPeakDemandBeginHour, PropEnum.msPeakDemandMaximumTasks,
				 */
				PropEnum.lastConfigurationModificationTime, PropEnum.lastPage, PropEnum.loadBalancingMode,
				PropEnum.locale, PropEnum.location, PropEnum.asAuditLevel, PropEnum.asAuditLevel, PropEnum.members,
				PropEnum.metadataModel, PropEnum.metadataModelPackage, PropEnum.mobilePhone, PropEnum.model,
				PropEnum.modelName, PropEnum.modificationTime, PropEnum.monthlyAbsoluteDay, PropEnum.monthlyRelativeDay,
				PropEnum.monthlyRelativeWeek, PropEnum.name, PropEnum.namespaceFormat, PropEnum.objectClass,
				PropEnum.options, PropEnum.output, PropEnum.owner, PropEnum.packageBase, PropEnum.page,
				PropEnum.pageOrientation, PropEnum.pagerPhone, PropEnum.parameters, PropEnum.parent, PropEnum.paths,
				PropEnum.permissions, PropEnum.policies, PropEnum.portalPage, PropEnum.position, PropEnum.postalAddress,
				PropEnum.printerAddress, PropEnum.productLocale, PropEnum.qualifier, PropEnum.related,
				PropEnum.recipientsEMail, PropEnum.recipients, PropEnum.related, PropEnum.replacement,
				PropEnum.requestedExecutionTime, PropEnum.retentions, PropEnum.rsAffineConnections,
				PropEnum.rsMaximumProcesses, PropEnum.rsNonAffineConnections, PropEnum.rsQueueLimit,
				PropEnum.runAsOwner, PropEnum.runningState,
				// PropEnum.runOptions,
				PropEnum.screenTip, PropEnum.searchPath,
				// PropEnum.searchPathForURL,
				PropEnum.sequencing, PropEnum.serverGroup, PropEnum.severity, PropEnum.source, PropEnum.specification,
				PropEnum.startAsActive, PropEnum.startDate, PropEnum.state, PropEnum.status, PropEnum.stepObject,
				PropEnum.storeID, PropEnum.surname, PropEnum.target, PropEnum.taskID, PropEnum.timeZoneID,
				PropEnum.triggerDescription, PropEnum.triggerName, PropEnum.type, PropEnum.unit, PropEnum.uri,
				PropEnum.usage, PropEnum.user, PropEnum.userCapabilities, PropEnum.userCapability, PropEnum.userName,
				PropEnum.userProfileSettings, PropEnum.version, PropEnum.verticalElementsRenderingLimit,
				PropEnum.profileRank, PropEnum.viewed, PropEnum.weeklyFriday, PropEnum.weeklyMonday,
				PropEnum.weeklySaturday, PropEnum.weeklySunday, PropEnum.weeklyThursday, PropEnum.weeklyTuesday,
				PropEnum.weeklyWednesday, PropEnum.yearlyAbsoluteDay, PropEnum.yearlyAbsoluteMonth,
				PropEnum.yearlyRelativeDay, PropEnum.yearlyRelativeMonth, PropEnum.yearlyRelativeWeek,
				/**
				 * Addition History information
				 */
				PropEnum.detail, PropEnum.data, PropEnum.output, PropEnum.maximumDetailSeverity, PropEnum.message };
		return properties;
	}

	/**
	 * Returns QueryOptions for basic operations
	 * 
	 * @return qopsObjects
	 */
	public QueryOptions setQORefProps() {
		// set the QueryOptions referenceProps option

		PropEnum referenceProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.userName,
				PropEnum.portalPage, PropEnum.portalPages, PropEnum.policies, PropEnum.position, PropEnum.owner,
				PropEnum.mobilePhone, PropEnum.email, PropEnum.creationTime, PropEnum.storeID, PropEnum.contentLocale,
				PropEnum.productLocale };

		RefProp refPropArray[] = { new RefProp() };
		refPropArray[0].setProperties(referenceProps);
		refPropArray[0].setRefPropName(PropEnum.members);

		QueryOptions qops = new QueryOptions();
		qops.setRefProps(refPropArray);

		return qops;
	}

	/**
	 * Returns QueryOptions used for Account Objects
	 * 
	 * @return
	 */
	public QueryOptions setQORefPropsForAccounts() {
		// set the QueryOptions referenceProps option

		PropEnum referenceProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.userName,
				PropEnum.portalPage, PropEnum.portalPages, PropEnum.policies, PropEnum.position, PropEnum.owner,
				PropEnum.mobilePhone, PropEnum.email, PropEnum.creationTime, PropEnum.storeID, PropEnum.contentLocale,
				PropEnum.productLocale };

		RefProp refPropArray[] = { new RefProp() };
		refPropArray[0].setProperties(referenceProps);
		refPropArray[0].setRefPropName(PropEnum.members);

		QueryOptions qops = new QueryOptions();
		qops.setRefProps(refPropArray);

		return qops;
	}

	/**
	 * Fetches an Objects with possible props, sorting and queryOptions
	 * 
	 * @param searchPath
	 * @param props
	 * @author RROEBER
	 * @return
	 */
	public BaseClass[] fetchObjectsWithQueryOptions(String searchPath, PropEnum[] props, Sort[] sort,
			QueryOptions queryOptionsProps) {
		BaseClass[] bc = new BaseClass[] {};

		try {
			log.debug("\tQuery searchpath: " + searchPath);

			// Check that cmService is filled ... if not, get one
			if (cmService == null)
				cmService = c8Access.getCmService();

			// fetch the searchpathObject(s)
			bc = cmService.query(new SearchPathMultipleObject(searchPath), props, sort, queryOptionsProps);
			log.debug("\tFound " + bc.length + " object(s)");
			for (int i = 0; i < bc.length; i++) {
				log.debug("\t" + i + ".) " + bc[i].getSearchPath().getValue());
			}
		} catch (RemoteException e) {
			String errMsg = "Found remote exception when fetching objects from Cognos server. Check if " + searchPath
					+ " exists!";
			log.error(errMsg, e);
		}

		return bc;
	}

	public void OutputDispatcherInformation(Dispatcher_Type objDispatcher, int dispnum) {
		// Basic Dispatcher Information
		log.info("--------------------------------------------------------------");
		log.info("Dispatcher Number: " + dispnum);
		log.info("Dispatcher Path: " + objDispatcher.getDispatcherPath().getValue());

		log.info("ServerGroup: " + objDispatcher.getServerGroup().getValue());
		log.info("runningState: " + objDispatcher.getRunningState().getValue());
		log.info("capacity: " + objDispatcher.getCapacity().getValue());

		// Batch Report Service configurations.
		log.info("BrsAffinConnections: " + objDispatcher.getBrsAffineConnections().getValue());
		log.info("BrsMaximumProcesses: " + objDispatcher.getBrsMaximumProcesses().getValue());
		log.info("BrsNonAffineConnections: " + objDispatcher.getBrsNonAffineConnections().getValue());

		// Report Service Configuration
		log.info("rsMaximumProcesses: " + objDispatcher.getRsAffineConnections().getValue());
		log.info("rsQueueLimit: " + objDispatcher.getRsQueueLimit().getValue());
		log.info("rsAffineConnections: " + objDispatcher.getRsAffineConnections().getValue());
		log.info("rsNonAffineConnections: " + objDispatcher.getRsNonAffineConnections().getValue());

		// Job Services Configuration
		log.info("msPeakDemandBeginHour: " + objDispatcher.getMsPeakDemandBeginHour().getValue());
		log.info("msPeakDemandMaximumTasks: " + objDispatcher.getMsPeakDemandMaximumTasks().getValue());
		log.info("msNonPeakDemandBeginHour: " + objDispatcher.getMsNonPeakDemandBeginHour().getValue());
		log.info("msNonPeakDemandMaximumTasks: " + objDispatcher.getMsNonPeakDemandMaximumTasks().getValue());

		// Logging Configuration
		log.info("dispatcherAuditLevel: " + objDispatcher.getDispatcherAuditLevel().getValue());
		log.info("jsAuditLevel: " + objDispatcher.getJsAuditLevel().getValue());
		log.info("brsAuditLevel: " + objDispatcher.getBrsAuditLevel().getValue());
		log.info("brsAuditNativeQuery: " + objDispatcher.getBrsAuditNativeQuery().isValue());
		log.info("rsAuditLevel: " + objDispatcher.getRsAuditLevel().getValue());
		log.info("rsAuditNativeQuery: " + objDispatcher.getRsAuditNativeQuery().isValue());

	}

	public void dumpDispatchers() {
		// TODO Auto-generated method stub
		ContentManagerService_PortType cms = c8Access.getCmService();

		log.debug("Getting dispatchers...");
		SearchPathMultipleObject spmo = new SearchPathMultipleObject("/configuration/dispatcher");
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name, PropEnum.searchPath,
				PropEnum.defaultName };
		Sort[] sort = new Sort[] {};
		QueryOptions options = new QueryOptions();

		ArrayList<BaseClass> removeDispatchers = new ArrayList<>();

		try {
			BaseClass[] results = cms.query(spmo, props, sort, options);

			if (results.length < 1) {
				log.error("Query for dispatcher did not return any results");
			} else {
				for (BaseClass bc : results) {
					String accData = "Dispatcher: " + bc.getSearchPath().getValue();
					log.debug(accData);

					if (!isDispatcherRunning(bc.getSearchPath().getValue())) {
						removeDispatchers.add(bc);
					}

				}
			}

		} catch (RemoteException e) {
			log.error("Error querying dispatcher data. " + e.getMessage());
		}

		removeDispatchers(removeDispatchers);
	}

	private void removeDispatchers(ArrayList<BaseClass> removeDispatchers) {
		// TODO Auto-generated method stub
		for (BaseClass object : removeDispatchers) {
			try {
				DeleteOptions del = new DeleteOptions();
				del.setForce(true);
				del.setRecursive(true);

				int deleted = c8Access.getCmService().delete(new BaseClass[] { object }, del);
				if (0 >= deleted) {
					log.debug("No dispatchers where deleted...");
				} else {
					log.debug(object.getDefaultName().getValue() + " -> got deleted successfully...");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isDispatcherRunning(String searchPath) {

		SearchPathSingleObject sPath = new SearchPathSingleObject(searchPath);
		String pingResult = null;

		try {
			PingReply pg = c8Access.getDispatcherService().ping(sPath);
			if (pg != null) {
				pingResult = pg.getVersion();
			}
		} catch (Exception e) {
			log.debug("Dispatcher with searchPath: " + searchPath + ", could not find it's version number...");
			log.debug(e.getMessage());
		}

		return (pingResult != null ? true : false);

	}

	public void fixPersonalFolderPermissions(Boolean force) {

		// updated accounts array to save
		ArrayList<Account> updatedAccounts = new ArrayList<>();

		// get the content manager service instance
		ContentManagerService_PortType cms = c8Access.getCmService();

		// get all the accounts
		SearchPathMultipleObject spmo = new SearchPathMultipleObject("//account");

		// minimum propenums that are required for this to work
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name, PropEnum.searchPath, PropEnum.defaultName,
				PropEnum.policies };

		// query for the search path
		BaseClass[] results = null;
		try {
			results = cms.query(spmo, props, new Sort[] {}, new QueryOptions());
		} catch (RemoteException e) {
			log.error("Error querying account data. " + e.getMessage());
		}

		if (results == null || results.length < 1) {
			log.error("Query for accounts did not return any results");
		} else {
			log.info("Found " + results.length + " accounts will check which accounts needs policy fixing.");

			// loop over all accounts
			for (BaseClass bc : results) {

				// check if account comes from LDAP namespace
				if (!bc.getSearchPath().getValue().contains("LDAP:"))
					continue;

				log.debug("Working on: " + bc.getSearchPath().getValue());
				// get accounts releted to the searchpath
				BaseClass[] accounts = null;

				try {
					accounts = cms.query(new SearchPathMultipleObject(bc.getSearchPath().getValue()), props,
							new Sort[] {}, new QueryOptions());
				} catch (RemoteException e) {
					log.error("=> Error querying account data. " + e.getMessage());
				}

				if (accounts == null || accounts.length < 1) {
					log.debug("=> No object found for " + bc.getSearchPath().getValue());
					log.debug("");
					log.debug("");
					log.debug("");
					continue;
				}

				// convert it to Account object
				Account acc = (Account) accounts[0];

				// get the index that has the userid in the searchpath default should be 0 but
				// just in case
				int index = 0;

				// check if there are policies related to this account else add one
				if (acc.getPolicies().getValue().length > 0) {

					// print policies security object value for debugging
					log.debug("=> Found these policy records for " + acc.getSearchPath().getValue());
					for (int i = 0; i < acc.getPolicies().getValue().length; i++) {

						Policy p = acc.getPolicies().getValue()[i];
						String searchPath = p.getSecurityObject().getSearchPath().getValue();
						if (searchPath.toLowerCase().contains(acc.getUserName().getValue().toLowerCase()))
							index = i;
						log.debug("=>> " + searchPath);

					}

					// update the security object for the first value
					log.debug("=> Will update policies for " + acc.getSearchPath().getValue());
					if (force || !acc.getPolicies().getValue()[index].getSecurityObject().getSearchPath().getValue()
							.equals(acc.getSearchPath().getValue())) {
						log.debug("=>> Updating "
								+ acc.getPolicies().getValue()[index].getSecurityObject().getSearchPath().getValue()
								+ " policy to " + acc.getSearchPath().getValue() + ".");
						acc.getPolicies().getValue()[index].getSecurityObject().setSearchPath(acc.getSearchPath());
					} else {
						log.debug("=>> Policies for " + acc.getSearchPath().getValue()
								+ " are correct, please use --force in case this is incorrect.");
						log.debug("");
						log.debug("");
						log.debug("");
						continue;
					}

				} else {

					// print that no policies where found for this account
					log.debug("=> No policies found for " + acc.getSearchPath().getValue());
					log.debug("");
					log.debug("");
					log.debug("");
					continue;

				}

				// create a new account object since we can't update existing accounts
				Account updatedAccount = new Account();
				// set the search path that will be updated
				updatedAccount.setSearchPath(acc.getSearchPath());
				// set the new policies
				updatedAccount.setPolicies(acc.getPolicies());

				// add it to the update list so all the accounts are updated at once and to
				// reduce the save timings.
				updatedAccounts.add(updatedAccount);

				log.debug("");
				log.debug("");
				log.debug("");
			}

			try {
				if (updatedAccounts.size() > 0) {
					BaseClass[] updatedItems = cms.update(updatedAccounts.toArray(new BaseClass[] {}),
							new UpdateOptions());

					if (updatedItems.length > 0) {
						log.info(updatedAccounts.size() + " accounts policies has been updated.");
					}
				} else {
					log.info("0 Accounts need update...");
				}
			} catch (Exception E) {
				log.debug("Unable to update the object...");
				log.debug(E.getMessage());
			}

		}

	}

	/**
	 * Loops over all the searchPaths inside reportVersions and deletes unwanted
	 * report versions.
	 * 
	 * @param reportVersions
	 */
	public void reportVersions(List<Properties> reportVersions) {

		// save reportVersions that we find in the future to remove
		ArrayList<BaseClass> reportVersionsToRemove = new ArrayList<>();

		for (Properties reportVersion : reportVersions) {

			// get the search path for reportVersions to look for
			String searchPath = reportVersion.getProperty("searchPath");
			// get the how many reportVersion we want to remove for specific report
			int keepLatest = Integer.parseInt(reportVersion.getProperty("keepLatest"));
			// get dryRun property if true do not add them to reportVersionsToRemove
			Boolean dryRun = true;
			if (reportVersion.getProperty("dryRun").equalsIgnoreCase("true")
					|| reportVersion.getProperty("dryRun").equalsIgnoreCase("false"))
				dryRun = Boolean.parseBoolean(reportVersion.getProperty("dryRun"));

			if (dryRun)
				log.info("Dry run is set to true for " + searchPath
						+ ", will not remove any report versions for this search path.");

			// find all the BaseClasses using the searchpath
			BaseClass[] reports = this.fetchObjectsWithQueryOptions(searchPath, this.setPropEnum(), new Sort[] {},
					this.setQORefProps());

			// check if we have found any reports with specified searchPath
			log.info(reports.length + " reports found using search path: " + searchPath);
			if (reports.length > 0) {
				// loop over all reports
				for (BaseClass report : reports) {
					log.info("Working on report: " + report.getDefaultName().getValue().toString());
					// get all report versions for current report
					BaseClass[] versions = this.fetchObjectsWithQueryOptions(
							report.getSearchPath().getValue().toString() + "/reportVersion", this.setPropEnum(),
							new Sort[] {}, this.setQORefProps());
					log.info(versions.length + " report versions found for report: "
							+ report.getDefaultName().getValue().toString());
					// check if there are equal or less amout of versions for current report else
					// remove older ones
					if (versions.length <= keepLatest) {
						log.info("There are already equal or less amount of report versions as keepLatest ("
								+ keepLatest + ") value");
					} else {
						// loop over all the versions and keep the latest specified
						for (int i = 0; i < versions.length - keepLatest; i++) {
							// remove these ones...
							log.info("Adding report version: " + versions[i].getDefaultName().getValue().toString()
									+ " to deleting list.");
							if (dryRun)
								reportVersionsToRemove.add(versions[i]);
						}
					}
				}
			} else { // log about the result
				log.info("Nothing to do here");
			}

		}

		// delete all the reportVersion that we have previously saved
		if (reportVersionsToRemove.size() > 0) {
			log.info("Removing total of " + reportVersionsToRemove.size() + " report versions");
			BaseClass[] versionsToRemove = new BaseClass[] {};
			versionsToRemove = reportVersionsToRemove.toArray(versionsToRemove);
			try {
				DeleteOptions deleteOptions = new DeleteOptions();
				deleteOptions.setRecursive(true);
				int objectsRemoved = c8Access.getCmService().delete(versionsToRemove, deleteOptions);
				log.info("Total objects removed related to " + versionsToRemove.length
						+ " report versions removed objects: " + objectsRemoved);
			} catch (Exception err) {
				log.error(err);
			}
		}

		log.info("This function was especially developed on request from Martin Otto :)");
	}

	private void logBaseObjectData(BaseClass object) {

		// log out basic information about the object
		String objectOutput = String.format("%s: %s", object.getSearchPath().getValue(),
				object.getDefaultName().getValue());
		log.debug(objectOutput);

		// get memebers
		BaseClass[] members = null;
		if (object.getClass().getName().contains("Role")) {
			members = ((Role) object).getMembers().getValue();
		} else if (object.getClass().getName().contains("Group")) {
			members = ((Group) object).getMembers().getValue();
		}

		if (members != null) {
			log.debug("== MEMBERS ==");
			// loop over members and the member type (Account, Role, Group, etc) and basic
			// information
			for (BaseClass member : members) {
				String[] className = member.getClass().getName().split("\\.");
				objectOutput = String.format("(%s) %s: %s", className[className.length - 1],
						member.getSearchPath().getValue(), member.getDefaultName().getValue());
				log.debug(objectOutput);
			}
			log.debug("== /MEMBERS ==");
		}

		// get user capability policies
		UserCapabilityPolicy[] userCapabilityPolicies = null;
		if (object.getClass().getName().contains("Content")) {
			userCapabilityPolicies = ((Content) object).getUserCapabilityPolicies().getValue();
		} else if (object.getClass().getName().contains("_package")) {
			userCapabilityPolicies = ((_package) object).getUserCapabilityPolicies().getValue();
		} else if (object.getClass().getName().contains("Folder")) {
			userCapabilityPolicies = ((Folder) object).getUserCapabilityPolicies().getValue();
		}

		if (userCapabilityPolicies != null) {
			log.debug("== CAPABILITY POLICIES ==");
			// loop over all capability policies and print out information
			for (UserCapabilityPolicy userCapabilityPolicy : userCapabilityPolicies) {
				// get securiyObject
				BaseClass securityObject = userCapabilityPolicy.getSecurityObject();
				// get permissions
				UserCapabilityPermission[] permissions = userCapabilityPolicy.getPermissions();
				String[] className = securityObject.getClass().getName().split("\\.");
				objectOutput = String.format("(%s) %s", className[className.length - 1],
						securityObject.getSearchPath().getValue());
				log.debug(objectOutput);
				log.debug("=== PERMISSIONS ===");
				// loop over all permissions and print out
				for (UserCapabilityPermission permission : permissions) {
					objectOutput = String.format("%s -> %s", permission.getUserCapability().getValue(),
							permission.getAccess().getValue());
					log.debug(objectOutput);
				}
				log.debug("=== /PERMISSIONS ===");
			}
			log.debug("== /CAPABILITY POLICIES ==");
		}

	}

	public void dumpSecuriyAndAccounts() {

		// get all accounts and dump the data
		ContentManagerService_PortType cms = c8Access.getCmService();

		String[] queries = { "//account", "//role", "//group", "//content", "//package", "//folder" };

		// initial values
		SearchPathMultipleObject spmo = null;
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name, PropEnum.searchPath, PropEnum.defaultName,
				PropEnum.userCapabilityPolicies };
		Sort[] sort = new Sort[] {};

		// loop over each query and print out information
		for (String query : queries) {
			log.debug("==============================");
			log.debug("Dumping information in " + query);
			log.debug("==============================");

			spmo = new SearchPathMultipleObject(query);

			try {
				BaseClass[] results = cms.query(spmo, props, sort, setQORefPropsForAccounts());

				if (results.length < 1) {
					log.error("Query for " + query + " did not return any results");
				} else {
					for (BaseClass bc : results) {
						logBaseObjectData(bc);
					}
				}

			} catch (RemoteException e) {
				log.error("Error querying " + query + " data. " + e.getMessage());
			}
		}
	}

}
