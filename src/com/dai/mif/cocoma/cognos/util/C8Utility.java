/**
 * $Id: C8Utility.java 169 2011-09-01 07:29:00Z rroeber $
 */
package com.dai.mif.cocoma.cognos.util;

import java.rmi.RemoteException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.Dispatcher_Type;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.Permission;
import com.cognos.developer.schemas.bibus._3.Policy;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
// import com.cognos.developer.schemas.bibus._3.RefProp;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since Mar 3, 2010
 * @version $Revision: 169 $ ($Date:: 2011-09-01 09:29:00 +0200#$)
 */
public class C8Utility {

	private static C8Access c8Access;
	private static Logger log;

	private ContentManagerService_PortType cmService = null;
	
	public C8Utility(C8Access c8Access) {
		this.c8Access = c8Access;
		this.log = Logging.getInstance().getLog(this.getClass());
	}

	/**
	 * Convenience method to find Cognos objects in {@link searchpath} with the
	 * given name within the Cognos namespace.
	 *
	 * @param searchPath
	 *            The name of the {@link searchPath} that is to be found.
	 *
	 * @return BaseClass[] results with the given name, or NULL if the group
	 *         could not be found.
	 */
	public BaseClass[] findObjectsInSearchPath(String searchPath) {

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(searchPath);
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath, PropEnum.members };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results = null;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			log.debug("The query for the searched group returned "
					+ results.length + " results.");
		} catch (RemoteException e) {
			String msg = "Error querying group " + searchPath + ": "
					+ e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return results;
	}

	/**
	 * Convenience method to find a Cognos {@link Group} with the given name
	 * within the Cognos namespace.
	 *
	 * @param groupName
	 *            The name of the {@link Group} that is to be found.
	 *
	 * @return {@link Group} with the given name, or NULL if the group could not
	 *         be found.
	 */
	public Group findGroup(String groupName) {
		Group theGroup = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"//group[@defaultName=\"" + groupName + "\"]");
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath, PropEnum.members };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			if (results.length == 1) {
				theGroup = (Group) results[0];
			} else {
				log.debug("The query for the searched group returned "
						+ results.length + " results.");
			}
		} catch (RemoteException e) {
			String msg = "Error querying group " + groupName + ": "
					+ e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return theGroup;
	}

	/**
	 * Convenience method to find a Cognos {@link Account} with the given name
	 * within the security namespace that is defined in the config.
	 *
	 * @param userName
	 *            The name of the {@link Account} that is to be found.
	 *
	 * @return {@link Account} with the given name, or NULL if the account could
	 *         not be found.
	 */
	public Account findAccount(String userName) {
		Account theAccount = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		// FIXME As a workaround the account has to be searched manually. For
		// some reason a query of the form //account[@userName="foo"] does not
		// return the account as it normally should do.

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"/directory/namespace");

		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name,
				PropEnum.defaultName, PropEnum.searchPath, PropEnum.options };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] namespaces;
		BaseClass[] results;

		try {

			namespaces = cms.query(spmo, props, sort, queryOptions);

			for (BaseClass namespace : namespaces) {

				String nsSearchPath = namespace.getSearchPath().getValue();
				spmo.set_value(nsSearchPath + "//account[@userName=\""
						+ userName + "\"]");
				// spmo.setValue(nsSearchPath + "//account");

				results = cms.query(spmo, props, sort, queryOptions);

				int i = 0;

				for (BaseClass bc : results) {

					Account acc = (Account) bc;
					if (acc != null) {

						String accName = acc.getUserName().getValue();

						if (accName != null) {
							if (accName.equals(userName)) {
								theAccount = acc;
								break;
							}
						}
					} else {
						log.info("Account at position " + i + " is NULL!");
					}
					i++;
				}
			}

		} catch (RemoteException e) {
			String msg = "Error querying account " + userName + ": "
					+ e.getMessage();
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
	 * Convenience method to find a Cognos {@link Role} with the given name
	 * within the Cognos namespace.
	 *
	 * @param roleName
	 *            The name of the {@link Role} that is to be found.
	 *
	 * @return {@link Role} with the given name, or NULL if the role could not
	 *         be found.
	 */
	public Role findRole(String roleName) {

		Role theRole = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"//role[@defaultName=\"" + roleName + "\"]");
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath, PropEnum.members };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		BaseClass[] results;

		try {
			results = cms.query(spmo, props, sort, queryOptions);
			if (results.length == 1) {
				theRole = (Role) results[0];
			} else {
				log.debug("The query for the searched role " + roleName
						+ " returned " + results.length + " results.");
			}
		} catch (RemoteException e) {
			String msg = "Error querying role " + roleName + ": "
					+ e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		return theRole;
	}

	/**
	 * Convenience method to create a {@link Policy} object on the given target
	 * holding the permissions provided by the given array of {@link Permission}
	 * .
	 *
	 * @param target
	 *            The target as {@link BaseClass} that the permission shall be
	 *            applied on.
	 * @param c8Permissions
	 *            Array of {@link Permission} objects defining the permissions
	 *            for read, write, execute, setPolicy and traverse.
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
	 * Convenience method for testing purposes. While working on CoCoMa the
	 * problem has come up, that various accounts within the D4 namespace do nor
	 * have a proper userName set no is the defaultName or any localized name
	 * filled with the proper userName. This method queries all accounts from
	 * the security namespace and dumps the relevant account data to the logging
	 * debug channel.
	 */
	public void dumpAccounts() {

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"//account");
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name,
				PropEnum.searchPath, PropEnum.defaultName };
		Sort[] sort = new Sort[] {};
		QueryOptions options = new QueryOptions();

		try {
			BaseClass[] results = cms.query(spmo, props, sort, options);

			if (results.length < 1) {
				log.error("Query for accounts did not return any results");
			} else {
				for (BaseClass bc : results) {
					Account acc = (Account) bc;

					String accData = "Account: "
							+ acc.getSearchPath().getValue() + "\n\tuserName: "
							+ acc.getUserName().getValue()
							+ "\n\tdefaultName: "
							+ acc.getDefaultName().getValue();

					MultilingualToken[] mlts = acc.getName().getValue();
					for (MultilingualToken mlt : mlts) {
						accData += "\n\tname (" + mlt.getLocale() + "): "
								+ mlt.getValue();
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
			delObject = cmService.query(searchPobj, new PropEnum[]{}, new Sort[]{}, new QueryOptions());
			cmService.delete(delObject, del);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			log.error("!!! Severe Error !!!");
			log.error("Could not remove:"+deleteItem);
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

			PropEnum[] confprop = { PropEnum.brsAffineConnections,
					PropEnum.brsMaximumProcesses,
					PropEnum.brsNonAffineConnections, PropEnum.capacity,
					PropEnum.dispatcherPath, PropEnum.msNonPeakDemandBeginHour,
					PropEnum.msNonPeakDemandMaximumTasks,
					PropEnum.msPeakDemandBeginHour,
					PropEnum.msPeakDemandMaximumTasks,
					PropEnum.dispatcherAuditLevel, PropEnum.jsAuditLevel,
					PropEnum.brsAuditLevel, PropEnum.brsAuditNativeQuery,
					PropEnum.rsAuditLevel, PropEnum.rsAuditNativeQuery,
					PropEnum.rsAffineConnections, PropEnum.rsMaximumProcesses,
					PropEnum.rsNonAffineConnections, PropEnum.rsQueueLimit,
					PropEnum.runningState, PropEnum.serverGroup, PropEnum.state };

			cmService = this.c8Access.getCmService();
			String activeCM = cmService.getActiveContentManager();

			log.info("--------------------------------------------------------------");
			log.info("activeCM: "+activeCM);
			log.info(" ... this is the Dispatcher with the active ContentManager. Use it for deployments.");
			
			BaseClass[] dispatchers = cmService.query(
					new SearchPathMultipleObject(searchPath), confprop,
					new Sort[] {}, new QueryOptions());

			// Print each object search path and class type
			for (int disp_index = 0; disp_index < dispatchers.length; disp_index++) {
				// For each dispatcher, identify the dispatcher with a number
				// and then output all information that defines that dispatcher
				// in terms of characteristics.

				Dispatcher_Type dispatch = (Dispatcher_Type) dispatchers[disp_index];
				int dispnum = disp_index + 1; // Convert from zero based.

				OutputDispatcherInformation(dispatch, dispnum);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// OutputDispatcherInformation: This is a procedure used to output all
	// information
	// that was specified by the PropEnum.

	public static void OutputDispatcherInformation(
			Dispatcher_Type objDispatcher, int dispnum) {
		// Basic Dispatcher Information
		log.info("--------------------------------------------------------------");
		log.info("Dispatcher Number: " + dispnum);
		log.info("Dispatcher Path: "
				+ objDispatcher.getDispatcherPath().getValue());

		log.info("ServerGroup: "
				+ objDispatcher.getServerGroup().getValue());
		log.info("runningState: "
				+ objDispatcher.getRunningState().getValue());
		log.info("capacity: "
				+ objDispatcher.getCapacity().getValue());

		// Batch Report Service configurations.
		log.info("BrsAffinConnections: "
				+ objDispatcher.getBrsAffineConnections().getValue());
		log.info("BrsMaximumProcesses: "
				+ objDispatcher.getBrsMaximumProcesses().getValue());
		log.info("BrsNonAffineConnections: "
				+ objDispatcher.getBrsNonAffineConnections().getValue());

		// Report Service Configuration
		log.info("rsMaximumProcesses: "
				+ objDispatcher.getRsAffineConnections().getValue());
		log.info("rsQueueLimit: "
				+ objDispatcher.getRsQueueLimit().getValue());
		log.info("rsAffineConnections: "
				+ objDispatcher.getRsAffineConnections().getValue());
		log.info("rsNonAffineConnections: "
				+ objDispatcher.getRsNonAffineConnections().getValue());

		// Job Services Configuration
		log.info("msPeakDemandBeginHour: "
				+ objDispatcher.getMsPeakDemandBeginHour().getValue());
		log.info("msPeakDemandMaximumTasks: "
				+ objDispatcher.getMsPeakDemandMaximumTasks().getValue());
		log.info("msNonPeakDemandBeginHour: "
				+ objDispatcher.getMsNonPeakDemandBeginHour().getValue());
		log.info("msNonPeakDemandMaximumTasks: "
				+ objDispatcher.getMsNonPeakDemandMaximumTasks().getValue());

		// Logging Configuration
		log.info("dispatcherAuditLevel: "
				+ objDispatcher.getDispatcherAuditLevel().getValue());
		log.info("jsAuditLevel: "
				+ objDispatcher.getJsAuditLevel().getValue());
		log.info("brsAuditLevel: "
				+ objDispatcher.getBrsAuditLevel().getValue());
		log.info("brsAuditNativeQuery: "
				+ objDispatcher.getBrsAuditNativeQuery().isValue());
		log.info("rsAuditLevel: "
				+ objDispatcher.getRsAuditLevel().getValue());
		log.info("rsAuditNativeQuery: "
				+ objDispatcher.getRsAuditNativeQuery().isValue());
	
	}

}
