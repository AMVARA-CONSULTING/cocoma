/**
 * $Id: C8ContentRestriction.java 169 2011-09-01 07:29:00Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BooleanProp;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.PortalOption;
import com.cognos.developer.schemas.bibus._3.PortalOptionBoolean;
import com.cognos.developer.schemas.bibus._3.PortalOptionEnum;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.Role;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UiClass;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.config.RestrictedContentData;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Oct 11, 2010
 * @version $Revision: 169 $ ($Date:: 2011-09-01 09:29:00 +0200#$)
 */
public class C8ContentRestriction {

	private C8Access c8Access;
	private RestrictedContentData restrictedContentData;
	private Logger log;
	private C8Utility c8util;

	/**
	 * @param limitedContentData
	 * @param c8Access
	 */
	public C8ContentRestriction(RestrictedContentData restrictionData,
			C8Access c8Access) {
		this.log = Logging.getInstance().getLog(this.getClass());
		this.c8Access = c8Access;
		this.c8util = new C8Utility(this.c8Access);
		this.restrictedContentData = restrictionData;
	}

	/**
     *
     */
	public void applyContentRestriction() {

		// hide all but the defined folders and packages
		hideContent(this.restrictedContentData.getVisibleElements());

		// allow the defined members to see even restricted content
		// disallow all other users to see hidden entries and prevent them
		// from setting to show hidden elements
		applyAccountRestriction(this.restrictedContentData);
	}

	/**
	 * @param restrictedContentData
	 */
	private void applyAccountRestriction(
			RestrictedContentData restrictedContentData) {

		// first collect all accounts that shall be unrestricted
		List<Account> unrestricted = gatherUnrestrictedAccounts();

		// now manipulate any account available and set the 'show hidden' flag
		// depending on whether the current account is in the list of
		// unrestricted accounts
		updateAccounts(unrestricted);

	}

	/**
	 * @param unrestrictedAccounts
	 */
	private void updateAccounts(List<Account> unrestrictedAccounts) {
		log.debug("Updating unrestricted accounts");
		ContentManagerService_PortType cms = c8Access.getCmService();

		String accountSearchPath = "/directory/namespace[@name=\""
				+ this.c8Access.getNameSpaceName() + "\"]//account";

		log.debug("Working on searchpath: "+accountSearchPath);
		
		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				accountSearchPath);

		PropEnum[] props = new PropEnum[] { PropEnum.searchPath,
				PropEnum.options };

		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		ArrayList<Account> updatedAccounts = new ArrayList<Account>();

		try {
			BaseClass[] results = cms.query(spmo, props, sort, queryOptions);

			for (BaseClass bc : results) {

				// can we access the account at all?
				if (bc != null) {
					Account acc = (Account) bc;
					boolean isRestrictedAccount = isRestrictedAccount(acc,
							unrestrictedAccounts);

					Option[] options = acc.getOptions().getValue();

					// see if we can access the options, otherwise the account
					// has not been initialized yet. In this case no further
					// steps are required. The user is skipped.
					if (options != null) {
						Account updatedAccount = updateAccountOptions(acc,
								isRestrictedAccount);
						if (updatedAccount != null) {
							updatedAccounts.add(updatedAccount);
						}
					}
				}
			}

		} catch (RemoteException e) {
			String msg = "Could not access accounts " + accountSearchPath;
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			this.log.error(msg);
		}

		// issue an update for the accounts which have manipulated
		// options if there are any.
		if (!updatedAccounts.isEmpty()) {
			UpdateOptions updateOptions = new UpdateOptions();
			try {
				cms.update(updatedAccounts.toArray(new BaseClass[] {}),
						updateOptions);
			} catch (RemoteException re) {
				String msg = "Could not update options of accounts. "
						+ updatedAccounts.size()
						+ " accounts need to be updated.";
				if (re instanceof AxisFault) {
					msg += " Error message: "
							+ c8util.parseAxisFault((AxisFault) re);
				}
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
				this.log.error(msg);
			}
		}
	}

	/**
	 * @param acc
	 * @param isRestrictedAccount
	 * @param options
	 * @return Account reference to the updated Account
	 */
	private Account updateAccountOptions(Account acc,
			boolean isRestrictedAccount) {

		Account result = null;
		Option[] options = acc.getOptions().getValue();

		boolean showHidden = !isRestrictedAccount;

		if (options != null) {
			String poShowHiddenName = PortalOptionEnum.showHiddenObjects
					.getValue();
			for (Option option : options) {
				if (option instanceof PortalOption) {
					PortalOption po = (PortalOption) option;

					String optionName = po.getName().getValue();
					if (optionName.equals(poShowHiddenName)) {

						PortalOptionBoolean pob = (PortalOptionBoolean) po;
						pob.setValue(showHidden);

						log.debug("Setting portal option " + poShowHiddenName
								+ " to " + showHidden + " for account "
								+ acc.getSearchPath().getValue());

						break;
					}
				}
			}

			result = acc;
		}

		return result;
	}

	/**
	 * @param acc
	 * @param unrestrictedAccounts
	 * @return
	 */
	private boolean isRestrictedAccount(Account acc,
			List<Account> unrestrictedAccounts) {

		boolean isRestricted = true;

		for (Account checkAccount : unrestrictedAccounts) {
			String checkSearchPath = checkAccount.getSearchPath().getValue();
			String accountSearchPath = acc.getSearchPath().getValue();
			if (checkSearchPath.equals(accountSearchPath)) {
				isRestricted = false;
				break;
			}
		}

		return isRestricted;
	}

	/**
	 * @return
	 */
	private List<Account> gatherUnrestrictedAccounts() {

		List<Account> unrestricted = new ArrayList<Account>();

		try {
			log.debug("Trying to add unrestricted roles");
			unrestricted.addAll(gatherUnrestrictedRoles());
			log.debug("Trying to add unrestricted groups");
			unrestricted.addAll(gatherUnrestrictedGroups());
			log.debug("Trying to add unrestricted users");
			unrestricted.addAll(gatherUnrestrictedUsers());
		} catch(Exception e) {
			log.info("Got an null value on gatherUnrestrictedRoles || gatherUnrestrictedGroups || gatherUnrestrictedUsers");
		}

		return unrestricted;
	}

	/**
	 * @return
	 */
	private List<Account> gatherUnrestrictedUsers() {
		List<Account> accounts = new ArrayList<Account>();

		for (String accountName : this.restrictedContentData
				.getUnrestrictedUsers()) {
			Account acc = c8util.findAccount(accountName);
			if (acc != null) {
				acc = loadAccount(acc.getSearchPath().getValue());
				accounts.add(acc);
			}
		}

		return accounts;
	}

	/**
	 * @return
	 */
	private List<Account> gatherUnrestrictedGroups() {
		List<Account> accounts = new ArrayList<Account>();

		for (String groupName : this.restrictedContentData
				.getUnrestrictedGroups()) {
			Group group = c8util.findGroup(groupName);
			List<Account> groupAccounts = gatherAccountsForGroup(group);
			accounts.addAll(groupAccounts);
		}

		return accounts;
	}

	/**
	 * @return
	 */
	private List<Account> gatherUnrestrictedRoles() {

		List<Account> accounts = new ArrayList<Account>();

		for (String roleName : this.restrictedContentData
				.getUnrestrictedRoles()) {
			Role role = c8util.findRole(roleName);
			if (role != null) {
				log.info("Role return not null - will gather members");
				List<Account> roleAccounts = gatherAccountsForRole(role);
				if(roleAccounts == null) {
					log.info("Role members returned null - you might want to check this.");
					return null;
				}
				log.debug("Adding accounts");
				accounts.addAll(roleAccounts);
			} else {
				log.info("Role return null - you might want to check this.");
			}
		}

		return accounts;
	}

	/**
	 * @param role
	 * @return
	 */
	private List<Account> gatherAccountsForRole(Role role) {

		List<Account> accounts = new ArrayList<Account>();
		String roleName = getRoleName(role);
		log.info("Gather Accounts for Role: "+roleName);
		
		BaseClass[] members = null;
		try {
			members = role.getMembers().getValue();
		} catch (Exception e) {
			log.info("Working on Role: " + roleName + ". No members found.");
			return null;
//			log.fatal(" !! will exit now !! ");
//			e.printStackTrace();
//			System.exit(-1);
		}

		log.info("Looping over members");
		for (BaseClass member : members) {
			if (member instanceof Group) {
				List<Account> accs = gatherAccountsForGroup((Group) member);
				accounts.addAll(accs);
			}
			if (member instanceof Role) {
				List<Account> accs = gatherAccountsForRole((Role) member);
				if(accs == null) {
					return null;
				}
				accounts.addAll(accs);
			}
			if (member instanceof Account) {
				Account acc = loadAccount(((Account) member).getSearchPath()
						.getValue());
				log.debug("Found account: "+acc.getDefaultName().getValue());
				if (acc != null) {
					accounts.add(acc);
				}
			}

		}
		return accounts;
	}

	/**
	 *
	 * @param accountSearchPath
	 * @return
	 */
	private Account loadAccount(String accountSearchPath) {
		Account acc = null;

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				accountSearchPath);
		PropEnum[] props = new PropEnum[] { PropEnum.searchPath, PropEnum.name,
				PropEnum.defaultName, PropEnum.userName, PropEnum.options };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();
		try {
			BaseClass[] results = cms.query(spmo, props, sort, queryOptions);
			if (results.length > 0) {
				if (results[0] instanceof Account) {
					acc = (Account) results[0];
				}
			}

		} catch (RemoteException e) {
			String msg = "Could not load account " + accountSearchPath;
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			this.log.error(msg);
		}

		return acc;
	}

	/**
	 * @param member
	 * @return
	 */
	private List<Account> gatherAccountsForGroup(Group group) {

		List<Account> accounts = new ArrayList<Account>();

		log.debug("Groupname: " + getGroupName(group));

		BaseClass[] members;
		try {
			members = group.getMembers().getValue();
			for (BaseClass member : members) {
				if (member instanceof Group) {
					List<Account> accs = gatherAccountsForGroup((Group) member);
					accounts.addAll(accs);
				}
				if (member instanceof Account) {
					Account acc = loadAccount(((Account) member)
							.getSearchPath().getValue());
					if (acc != null) {
						accounts.add(acc);
					}
				}

			}
		} catch (Exception e) {
			this.log.debug("??? Error ???");
			this.log.debug("Check why groupmembers for " + getGroupName(group)
					+ " returns null !!");
		}
		return accounts;
	}

	/**
	 * Get name from object group 
	 * if that fails it gets it default name and 
	 * if that also fails it gets it search path
	 * 
	 * @author ASOHAIL
	 * @since 2018-11-13
	 * @return string with group name
	 */
	private String getGroupName(Group group) {
		String myName = "undefined";
		try {
			myName = group.getName().getValue().toString();
		} catch (Exception e) {
			try {
				myName = group.getDefaultName().getValue().toString();
			} catch (Exception e1) {
				try {
					myName = group.getSearchPath().getValue().toString();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
		
		return myName;
	}
	/**
	 * Get name from object role 
	 * if that fails it gets it default name and 
	 * if that also fails it gets it search path
	 * 
	 * @author ASOHAIL
	 * @since 2018-11-13
	 * @return string with role name
	 */
	private String getRoleName(Role group) {
		String myName = "undefined";
		try {
			myName = group.getName().getValue().toString();
		} catch (Exception e) {
			try {
				myName = group.getDefaultName().getValue().toString();
			} catch (Exception e1) {
				try {
					myName = group.getSearchPath().getValue().toString();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
		
		return myName;
	}

	/**
	 * @param visibleElements
	 */
	private void hideContent(List<String> visibleElements) {

		ContentManagerService_PortType cms = c8Access.getCmService();

		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"/content/*");
		PropEnum[] props = new PropEnum[] { PropEnum.searchPath,
				PropEnum.defaultName, PropEnum.hidden, PropEnum.capabilities };
		Sort[] sort = new Sort[] {};
		QueryOptions options = new QueryOptions();

		try {
			BaseClass[] results = cms.query(spmo, props, sort, options);

			ArrayList<BaseClass> updates = new ArrayList<BaseClass>();

			for (BaseClass bc : results) {
				String searchPath = bc.getSearchPath().getValue();

				if (bc instanceof UiClass) {

					UiClass uic = (UiClass) bc;

					boolean hideElement = !this.restrictedContentData
							.getVisibleElements().contains(searchPath);

					BooleanProp hiddenProp = new BooleanProp();
					hiddenProp.setValue(hideElement);

					if (hideElement) {
						this.log.debug("Setting visibility status of element "
								+ searchPath + " to HIDDEN.");
					} else {
						this.log.debug("Setting visibility status of element "
								+ searchPath + " to VISIBLE.");
					}

					uic.setHidden(hiddenProp);

					updates.add(uic);

				} else {
					this.log.debug("The content element "
							+ searchPath
							+ " is no instance of UiClass and cannot be hidden.");
				}
			}

			if (updates.isEmpty()) {
				this.log.debug("There are no content elements to be updated.");
			} else {

				UpdateOptions updateOptions = new UpdateOptions();
				cms.update(updates.toArray(new BaseClass[] {}), updateOptions);
			}
		} catch (RemoteException e) {

			String msg = "Error setting visibility status: " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			this.log.error(msg);

		}

	}
}
