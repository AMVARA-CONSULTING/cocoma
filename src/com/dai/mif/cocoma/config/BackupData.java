/**
 * $Id: $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author Stefan Brauner
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since Oct 14, 2014
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class BackupData {

    private String name;
    private String password;
    private boolean enabled;
    private boolean use_datetimesuffix;
    private boolean backup_configured;
    
    private XMLConfiguration conf;
    private Logger log;

    /**
     * @param conf
     */
    public BackupData(XMLConfiguration conf) throws ConfigException {
        this.conf = conf;
        Cryptography crypt = Cryptography.getInstance();
        this.log = Logging.getInstance().getLog(this.getClass());

        log.debug("Parsing backup configuration");

        // Check for backupSection
        this.setBackup_configured(conf.getKeys("backup").hasNext()) ;
        
        // Read rest of Configuration
        this.enabled = conf.getBoolean("backup.enabled", false);
        this.name = conf.getString("backup.name", "FullBackup");
        this.password = conf.getString("backup.password", null);
        this.use_datetimesuffix = conf.getBoolean("backup.use_datetimesuffix", false);

        try {
            String cryptedPass = conf.getString("backup.password", null);
            this.password = crypt.decrypt(cryptedPass);
        } catch (Exception e) {
            if (!CoCoMa.isInteractiveMode()) {
                throw new ConfigException(
                        "Error decrypting the backup password: " + e.getMessage(), e);
            } else {
                this.password = null;
            }
        }
    }

    /**
     * @param password
     * @throws ConfigException
     */
    public void setPassword(String password) throws ConfigException {

        Cryptography crypt = Cryptography.getInstance();

        try {
            conf.setProperty("backup.password", crypt.encrypt(password));
            conf.save();
            conf.reload();
        } catch (ConfigurationException e) {
            throw new ConfigException("Error saving the password: "
                    + e.getMessage());
        }

    }

    /**
     * Getter for the name field
     *
     * @return the currently set value for name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the name field
     *
     * @param name the new value to set for the name field
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the enabled field
     *
     * @return the currently set value for enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Setter for the enabled field
     *
     * @param enabled the new value to set for the enabled field
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Getter for the password field
     *
     * @return the currently set value for password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Getter for the UseDateTimeSuffix field
     *
     * @return the currently set value for the parameters, can be true or false
     */
    public boolean getUseDateTimeSuffix() {
        return use_datetimesuffix;
    }

	public boolean isBackup_configured() {
		return backup_configured;
	}

	public void setBackup_configured(boolean backup_configured) {
		this.backup_configured = backup_configured;
	}


}
