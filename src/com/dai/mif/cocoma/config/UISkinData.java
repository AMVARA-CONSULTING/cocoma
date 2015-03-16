/**
 * $Id: UISkinData.java 115 2010-03-17 10:59:54Z rroeber $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.XMLConfiguration;

import com.dai.mif.cocoma.exception.CoCoMaConfigException;

/**
 * This class encapsulates data about portal skins that shall be created or
 * updated in the Cognos 8 system.
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 16, 2010
 * @version $Revision: 115 $ ($Date:: 2010-03-17 11:59:54 +0100#$)
 */
public class UISkinData {

    public static final String RECORD_KEY_PREFIX = "ui.skins.skin";
    private String name;
    private String preview;
    private String resourceLocation;

    /**
     * Constructor for the {@link UISkinData} class. It takes the name for the
     * skin as argument.
     *
     * @param skinName
     *            The name that is to be associated with this skin.
     *
     * @throws CoCoMaConfigException
     *             This exception is thrown when there is an invalid value for
     *             the skin name.
     */
    public UISkinData(String skinName) throws CoCoMaConfigException {
        this.name = skinName;
        if (name.length() < 1) {
            throw new CoCoMaConfigException(
                    "Invalid value for name. String must not be empty");
        }
    }

    /**
     * Method to actually read the details for the skin encapsulated by this
     * class.
     *
     * @param conf
     *            Reference to the {@link XMLConfiguration} instance to be used
     *            for reading the data.
     * @param configKey
     *            The configuration key to be used to access the correct data in
     *            the config file.
     *
     * @throws CoCoMaConfigException
     *             This exception is thrown when an invalid value has been
     *             specified in the configuration.
     */
    public void readConfig(XMLConfiguration conf, String configKey)
            throws CoCoMaConfigException {

        this.preview = conf.getString(configKey + ".preview");
        this.resourceLocation = conf.getString(configKey + ".resourceLocation");

        if (preview.length() < 1) {
            throw new CoCoMaConfigException(
                    "Invalid value for preview. String must not be empty");
        }
        if (resourceLocation.length() < 1) {
            throw new CoCoMaConfigException(
                    "Invalid value for resourceLocation. String must not be empty");
        }
    }

    /**
     * Getter for the name field in this class.
     *
     * @return The currently set name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Getter for the preview field in this class.
     *
     * @return the currently set value for preview.
     */
    public String getPreview() {
        return this.preview;
    }

    /**
     * Getter for the resourceLocation file in this class.
     *
     * @return The currently set value for the resourceLocation.
     */
    public String getResourceLocation() {
        return this.resourceLocation;
    }

}
