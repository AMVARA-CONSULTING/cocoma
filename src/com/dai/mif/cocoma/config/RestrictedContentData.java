/**
 * $Id: RestrictedContentData.java 163 2010-10-12 09:16:13Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class encapsulates the data necessary to limit the visibility of entries
 * in Cognos Connection to a certain element.
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Oct 11, 2010
 * @version $Revision: 163 $ ($Date:: 2010-10-12 11:16:13 +0200#$)
 */
public class RestrictedContentData {

    public static final String RECORD_KEY_PREFIX = "restrictedContent";

    private List<String> visibleElements;

    private List<String> unrestrictedRoles;
    private List<String> unrestrictedGroups;
    private List<String> unrestrictedUsers;

    private boolean restrictionDefined;

    /**
     */
    public RestrictedContentData() {
        this.restrictionDefined = false;

        this.visibleElements = new ArrayList<String>();

        this.unrestrictedRoles = new ArrayList<String>();
        this.unrestrictedGroups = new ArrayList<String>();
        this.unrestrictedUsers = new ArrayList<String>();

    }

    /**
     * @param conf
     * @throws ConfigException
     */
    public void readConfig(XMLConfiguration conf) throws ConfigException {

    	//
        // Prepare Logger
        //
        Logging logging = Logging.getInstance();
        logging.setConsoleLogging(true);
        Logger log = logging.getLog(DispatcherData.class);
        
        this.restrictionDefined = true;

        @SuppressWarnings("unchecked")
		Iterator<String> keys = conf.getKeys(RECORD_KEY_PREFIX); 
        
    	if ( !keys.hasNext()) {
        	log.debug("No restrictions config option found. Skipping restrictions reading.");
        	this.restrictionDefined = false;
    	} else {
	        log.debug("Restrictions found! Will read.");		
	        if (restrictionDefined) {
	
	            String[] visibleEntries = conf.getStringArray(RECORD_KEY_PREFIX
	                    + ".visible.searchPath");
	            if (visibleEntries != null) {
	                for (String visibleEntry : visibleEntries) {
	                    if (visibleEntry.length() > 0) {
	                        this.visibleElements.add(visibleEntry);
	                    }
	                }
	            }
	
	            // read the unrestricted roles, groups and users
	
	            String[] roles = conf.getStringArray(RECORD_KEY_PREFIX
	                    + ".unrestricted.role");
	            if (roles != null) {
	                for (String role : roles) {
	                    if (role.length() > 0) {
	                        this.unrestrictedRoles.add(role);
	                    }
	                }
	            }
	
	            String[] groups = conf.getStringArray(RECORD_KEY_PREFIX
	                    + ".unrestricted.group");
	            if (groups != null) {
	                for (String group : groups) {
	                    if (group.length() > 0) {
	                        this.unrestrictedGroups.add(group);
	                    }
	                }
	            }
	
	            String[] users = conf.getStringArray(RECORD_KEY_PREFIX
	                    + ".unrestricted.user");
	            if (users != null) {
	                for (String user : users) {
	                    if (user.length() > 0) {
	                        this.unrestrictedUsers.add(user);
	                    }
	                }
	            }
	
	            if (this.unrestrictedGroups.isEmpty()
	                    && this.unrestrictedRoles.isEmpty()
	                    && this.unrestrictedUsers.isEmpty()) {
	                throw new ConfigException(
	                        "There must be at least one group, one role or one user defined to be able to see even hidden elements.");
	            }

	        }
        }

    }

    public Boolean getRestrictionsDefined() {
		return restrictionDefined;
    }
    
    /**
     *
     * @return
     */
    public List<String> getVisibleElements() {
        return this.visibleElements;
    }

    /**
     *
     * @return
     */
    public List<String> getUnrestrictedRoles() {
        return this.unrestrictedRoles;
    }

    /**
     *
     * @return
     */
    public List<String> getUnrestrictedGroups() {
        return this.unrestrictedGroups;
    }

    /**
     *
     * @return
     */
    public List<String> getUnrestrictedUsers() {
        return this.unrestrictedUsers;
    }

}
