/**
 * $Id: AbstractSecurityObject.java 92 2010-03-03 15:41:41Z rroeber $
 */
package com.dai.mif.cocoma.config;

import org.apache.commons.configuration.XMLConfiguration;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.Role;

/**
 * Abstract class to encapsulate a security object. It defines a name and can
 * hold a reference to the corresponding Cognos8 security object (
 * {@link Account}, {@link Group}, {@link Role} ).
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 18, 2010
 * @version $Revision: 92 $ ($Date:: 2010-03-03 16:41:41 +0100#$)
 */
public abstract class AbstractSecurityObject {

    private String name;
    private boolean enabled;

    private BaseClass c8Object;

    /**
     *
     * @param name
     */
    public AbstractSecurityObject(String name) {
        this(name, true);
    }

    /**
     *
     * @param name
     * @param enabled
     */
    public AbstractSecurityObject(String name, boolean enabled) {

        this.name = name;
        this.enabled = enabled;

        this.c8Object = null;

    }

    /**
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     *
     * @return
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @param bc
     */
    public void setC8Object(BaseClass bc) {
        this.c8Object = bc;
    }

    /**
     *
     * @return
     */
    public BaseClass getC8Object() {
        return this.c8Object;
    }

    /**
     *
     */
    public void disable() {
        setEnabled(false);
    };

    /**
     *
     */
    public void enable() {
        setEnabled(true);
    };

    /**
     *
     * @param enabled
     */
    protected abstract void setEnabled(boolean enabled);

    /**
     *
     * @param conf
     * @param configKey
     */
    public abstract void readConfig(XMLConfiguration conf, String configKey);

    /**
     * @return
     */
    public abstract BaseClass createC8Object();

}
