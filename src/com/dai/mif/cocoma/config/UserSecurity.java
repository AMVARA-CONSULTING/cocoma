/**
 * $Id: UserSecurity.java 93 2010-03-04 08:42:55Z rroeber $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.XMLConfiguration;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 93 $ ($Date:: 2010-03-04 09:42:55 +0100#$)
 */
public class UserSecurity extends AbstractSecurityObject {

    /**
     * @param name
     */
    public UserSecurity(String name) {
        super(name);
    }

    /**
     * @param name
     * @param enabled
     */
    public UserSecurity(String name, boolean enabled) {
        super(name, enabled);
    }

    /**
     * @see com.dai.mif.cocoma.config.AbstractSecurityObject#setEnabled(boolean)
     */
    @Override
    protected void setEnabled(boolean enabled) {
        Account acc = (Account) getC8Object();
        acc.getDisabled().setValue(!enabled);
    }

    /**
     * @see com.dai.mif.cocoma.config.AbstractSecurityObject#readConfig(org.apache.commons.configuration.XMLConfiguration,
     *      java.lang.String)
     */
    @Override
    public void readConfig(XMLConfiguration conf, String configKey) {
        // TODO Auto-generated method stub

    }

    /**
     * @see com.dai.mif.cocoma.config.AbstractSecurityObject#createC8Object()
     */
    @Override
    public BaseClass createC8Object() {
        // for now users are not created in the Cognos namespace
        return null;
    }

}
