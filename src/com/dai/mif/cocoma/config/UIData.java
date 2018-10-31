/**
 * $Id: UIData.java 153 2010-06-22 08:36:36Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class encapsulates data to be set on the user interface in Cognos 8. It
 * contains settings for the default user profile as well as definitions for
 * portal skins to be created.
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 16, 2010
 * @version $Revision: 153 $ ($Date:: 2010-06-22 10:36:36 +0200#$)
 */
public class UIData {

    public static final String RECORD_KEY_PREFIX = "ui";

    private List<UISkinData> skinData;

    private int linesPerPage;

    private String skin;

    private String listSeparator;
    
    private Boolean UIdataFoundIXmlConfig = false;

    /**
     * Constructor for the {@link UIData} class. It takes a reference to the
     * current {@link XMLConfiguration} object and reads the relevant data for
     * the UI from the config.
     *
     * @param conf
     *            Instance of {@link XMLConfiguration} to be used for reading
     *            the configuration details.
     *
     * @throws ConfigException
     *             This exception is thrown whenever an invalid configuration
     *             value is detected.
     */
    public UIData(XMLConfiguration conf) throws ConfigException {

    	//
        // Prepare Logger
        //
        Logging logging = Logging.getInstance();
        logging.setConsoleLogging(true);
        Logger log = logging.getLog(DispatcherData.class);

        // read the sub-data

        this.skinData = readSkinData(conf);
        if ( this.skinData.size() != 0 ) {
        	UIdataFoundIXmlConfig = true;
        	log.debug("Skin Config Options found. Reading Skin Data");
        	// read the data for the default user profile
	        this.listSeparator = conf.getString(RECORD_KEY_PREFIX
	                + ".defaultProfile.listSeparator", "");
	        this.linesPerPage = conf.getInt(RECORD_KEY_PREFIX
	                + ".defaultProfile.linesPerPage", 0);
	        this.skin = conf.getString(RECORD_KEY_PREFIX + ".defaultProfile.skin",
	                "");
	
	        // check if the data read is valid
	
	        if (!this.listSeparator.equals("background")
	                && !this.listSeparator.equals("line")
	                && !this.listSeparator.equals("none")) {
	        	UIdataFoundIXmlConfig = false;
	        	throw new ConfigException(
	                    "Invalid value for listViewSeparator. May only be one of: background, line or none");
	        	}
	
	        if (this.linesPerPage < 1) {
	        	UIdataFoundIXmlConfig = false;
	            throw new ConfigException(
	                    "Invalid value for linesPerPage. Must be a value >= 1");
	        }
	
	        if (this.skin.length() < 1) {
	        	UIdataFoundIXmlConfig = false;
	            throw new ConfigException(
	                    "Invalid value for skin. A skin name must be specified.");
	        }
        }
        else {
        	log.debug("No skin Config Options found. Skipping skin data.");
        }
    }

    public Boolean getUItagFoundInXmlConfig() {
    	return UIdataFoundIXmlConfig;
    }
    
    
    /**
     * Method to read configuration details for the portal skins. The respective
     * {@link UISkinData} objects are collected in a list in this class.
     *
     * @return List of {@link UISkinData} instances encapsulating the single
     *         skins to be created.
     */
    private List<UISkinData> readSkinData(XMLConfiguration conf)
            throws ConfigException {

        List<UISkinData> skinList = new ArrayList<UISkinData>();

        // read and prepare skin data
        String[] skinNames = conf.getStringArray(UISkinData.RECORD_KEY_PREFIX
                + ".name");
        for (int i = 0; i < skinNames.length; i++) {
            String skinName = skinNames[i];
            String configKey = UISkinData.RECORD_KEY_PREFIX + "(" + i + ")";
            UISkinData uiSkinData = new UISkinData(skinName);
            uiSkinData.readConfig(conf, configKey);
            skinList.add(uiSkinData);
        }

        return skinList;
    }

    /**
     * Getter for the listSeparator field.
     *
     * @return The currently set listSeoarator.
     */
    public String getListSeparator() {
        return this.listSeparator;
    }

    /**
     * Getter for the linePerPage field.
     *
     * @return The currently set linesPerPage
     */
    public int getLinesPerPage() {
        return this.linesPerPage;
    }

    /**
     * Getter for the skin field. It defines the skin to be set as default skin.
     *
     * @return The currently set default skin.
     */
    public String getSkin() {
        return this.skin;
    }

    /**
     * Getter for the list of {@link UISkinData} instances.
     *
     * @return The current list of {@link UISkinData} instances.
     */
    public List<UISkinData> getUISkinData() {
        return this.skinData;
    }

}
