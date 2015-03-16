/**
 * $Id: DataSourceData.java 153 2010-06-22 08:36:36Z rroeber $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.CoCoMaConfigException;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 153 $ ($Date:: 2010-06-22 10:36:36 +0200#$)
 */
public class DataSourceData {

    protected static final String RECORD_KEY_PREFIX = "dataSources.dataSource";

    private String recordKey;

    private String name;
    private String dbAlias;
    private String userName;
    private String password;
    private String isolationLevel;
    private String openConnectionCommands;
    private String closeConnectionCommands;
    private String openSessionCommands;
    private String closeSessionCommands;

    private XMLConfiguration conf;

    /**
     * @param conf
     * @param i
     */
    public DataSourceData(XMLConfiguration conf, int i)
            throws CoCoMaConfigException {

        this.conf = conf;

        this.recordKey = RECORD_KEY_PREFIX + "(" + i + ")";

        Cryptography crypt = Cryptography.getInstance();

        this.name = conf.getString(recordKey + ".name");
        this.dbAlias = conf.getString(recordKey + ".dbalias");
        this.userName = conf.getString(recordKey + ".username");

        String cryptedPass = conf.getString(recordKey + ".password");
        try {
            this.password = crypt.decrypt(cryptedPass);
        } catch (Exception e) {
            if (CoCoMa.isInteractiveMode()) {
                this.password = "";
            }
        }

        this.isolationLevel = conf.getString(recordKey + ".isolationLevel");

        this.openConnectionCommands = conf.getString(recordKey
                + ".connectionCommands.openConnection", "");
        this.closeConnectionCommands = conf.getString(recordKey
                + ".connectionCommands.closeConnection", "");
        this.openSessionCommands = conf.getString(recordKey
                + ".connectionCommands.openSession", "");
        this.closeSessionCommands = conf.getString(recordKey
                + ".connectionCommands.closeSession", "");

        if (this.name.length() == 0) {
            throw new CoCoMaConfigException("DataSource name is empty.");
        }

        if (this.dbAlias.length() == 0) {
            throw new CoCoMaConfigException("DataSource (" + this.name
                    + ") DB alias is empty.");
        }

        if (this.userName.length() == 0) {
            throw new CoCoMaConfigException("DataSource (" + this.name
                    + ") username is empty.");
        }

        if ((this.password.length() == 0) && (!CoCoMa.isInteractiveMode())) {
            throw new CoCoMaConfigException("DataSource (" + this.name
                    + ") password is empty.");
        }

        if (this.isolationLevel.length() == 0) {
            throw new CoCoMaConfigException("DataSource (" + this.name
                    + ") isolationLevel is empty.");
        } else if (!this.isolationLevel.equals("cursorStability")
                && !this.isolationLevel.equals("phantomProtection")
                && !this.isolationLevel.equals("readCommitted")
                && !this.isolationLevel.equals("readUncommitted")
                && !this.isolationLevel.equals("reproducibleRead")
                && !this.isolationLevel.equals("serializable")) {
            throw new CoCoMaConfigException(
                    "DataSource isolationLevel may only be one of the following values: cursorStability , phantomProtection , readCommitted , readUncommitted , reproducibleRead, serializable");

        }

    }

    /**
     * @param password
     * @throws CoCoMaConfigException
     */
    public void setPassword(String password) throws CoCoMaConfigException {

        Cryptography crypt = Cryptography.getInstance();

        try {
            conf.setProperty(this.recordKey + ".password",
                    crypt.encrypt(password));
            conf.save();
            conf.reload();
        } catch (ConfigurationException e) {
            throw new CoCoMaConfigException("Error saving the password: "
                    + e.getMessage());
        }

    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the isolationLevel
     */
    public String getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * @return the catalog
     */
    public String getDBAlias() {
        return dbAlias;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the openConnectionCommands
     */
    public String getOpenConnectionCommands() {
        return openConnectionCommands;
    }

    /**
     * @return the closeConnectionCommands
     */
    public String getCloseConnectionCommands() {
        return closeConnectionCommands;
    }

    /**
     * @return the openSessionCommands
     */
    public String getOpenSessionCommands() {
        return openSessionCommands;
    }

    /**
     * @return the closeSessionCommands
     */
    public String getCloseSessionCommands() {
        return closeSessionCommands;
    }

}
