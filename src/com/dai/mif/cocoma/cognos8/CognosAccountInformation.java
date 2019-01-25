package com.dai.mif.cocoma.cognos8;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.LanguageProp;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;


public class CognosAccountInformation {

    private Logger logger;

    private C8Access c8Access;

    public CognosAccountInformation(C8Access c8Access2) {
    	c8Access = c8Access2;
		logger = Logger.getLogger(this.getClass());

	}

	public void setLanguageToAccount(String account, String language) {
    	
    	String myAccountSearchpath = "CAMID('LDAP')//*[@userName='apiuser'][@objectClass='account']";
    	String myLanguageString = "en";
    	
    	// Update myAccountSearchpath if incoming account string has value
    	if (account != null && account.length()>0)
			myAccountSearchpath = account;
    	
    	// Update languageString if incmoming language string has value
    	if (language != null && language.length()>0)
    		myLanguageString = language;
    	
    	
    	// Get ConnectivityHelper
		C8Utility ch = c8Access.getC8Utility();

		// Query Objects
		BaseClass[] bcResults = ch.fetchObjectsWithQueryOptions(
				 myAccountSearchpath,
				 ch.setPropEnum(), new Sort[] {},
				 ch.setQORefPropsForAccounts());
		
		// log the Account Informationen retrieved 
		Account myAccount = ((Account)bcResults[0]);
		String myAccountDefaultName = myAccount.getDefaultName().getValue();
		String myAccountContentLocale = myAccount.getContentLocale().getValue();
		logger.debug("ContentLocale of "+myAccountDefaultName+": "+myAccountContentLocale);
		logger.debug("ProductLocale: "+myAccount.getProductLocale().getValue());
		
		// Return if language is set to myLanguageString
		if (myAccount.getContentLocale().getValue().equalsIgnoreCase(myLanguageString) 
				&& myAccount.getProductLocale().getValue().equalsIgnoreCase(myLanguageString)) 
			return;

		logger.debug("Language settings of the account must be updated to '"+myLanguageString+"'");

		// Set the locales in temporary variables
		LanguageProp myPropsPL = myAccount.getProductLocale();
		LanguageProp myPropsCL = myAccount.getContentLocale();
		
		myPropsPL.setValue(myLanguageString);
		myPropsCL.setValue(myLanguageString);

		// Create new AccountObject containing only the updated information
		Account updatedAccount = new Account();
		updatedAccount.setSearchPath( myAccount.getSearchPath() );
		updatedAccount.setContentLocale(myPropsCL);
		updatedAccount.setProductLocale(myPropsPL);
		
		// Try sending update request to cmService
		try
		{
			BaseClass[] updatedItems =
				c8Access.getCmService().update(
					new BaseClass[] { updatedAccount },
					new UpdateOptions());
			if (updatedItems.length > 0)
			{
				logger.info("Successfully updated "+updatedAccount.getSearchPath().getValue());
				return;
			}
		}
		catch (java.rmi.RemoteException remoteEx)
		{
			remoteEx.printStackTrace();
			System.out.println("Exception Caught:\n" + remoteEx.getMessage() );
			return ; 
		}
    	
    }

}