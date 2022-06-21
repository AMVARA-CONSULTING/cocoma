/**
 * $Id: ServerData.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.ConfigException;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class ServerData {

    private String dispatcherURL;
    private String nameSpace;
    private String userName;
    private String password;
    private String version;

    private XMLConfiguration conf;

    /**
     * @param conf
     */
    public ServerData(XMLConfiguration conf) throws ConfigException {

        this.conf = conf;

        Cryptography crypt = Cryptography.getInstance();

        this.dispatcherURL = conf.getString("server.dispatcherURL", "");
        this.nameSpace = conf.getString("server.namespace", "");
        this.userName = conf.getString("server.username", "");
        this.version = conf.getString("server.version", "");

        try {
            String cryptedPass = conf.getString("server.password", "");
            this.password = crypt.decrypt(cryptedPass);
        } catch (Exception e) {
            if (!CoCoMa.isInteractiveMode()) {
                throw new ConfigException(
                        "Error decrypting the password: " + e.getMessage(), e);
            } else {
                this.password = "";
            }
        }

        if (this.dispatcherURL.length() == 0) {
            throw new ConfigException("DispatcherURL is empty.");
        }
        if (this.nameSpace.length() == 0) {
            throw new ConfigException("Namspace is empty.");
        }
        if (this.userName.length() == 0) {
            throw new ConfigException("Username is empty.");
        }
        if (this.password == null) {
            throw new ConfigException("Password could not be decrypted");
        } else if ((this.password.length() == 0)
                && (!CoCoMa.isInteractiveMode())) {
            throw new ConfigException("Password is empty.");
        }

    }

    /**
     * @param password
     * @throws ConfigException
     */
    public void setPassword(String password) throws ConfigException {

        Cryptography crypt = Cryptography.getInstance();

        try {
            conf.setProperty("server.password", crypt.encrypt(password));
            conf.save();
            conf.reload();
        } catch (ConfigurationException e) {
            throw new ConfigException("Error saving the password: "
                    + e.getMessage());
        }

    }

    /**
     * @return the dispatcherURL
     */
    public String getDispatcherURL() {
        return dispatcherURL;
    }

    /**
     * @return the dispatcherURL
     */
    public String getServerVersion() {
        return version;
    }

    /**
     * @return the nameSpace
     */
    public String getNameSpace() {
        return nameSpace;
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

}
