/**
 * $Id: CoCoMaConfigException.java 89 2010-02-18 15:56:26Z rroeber $
 */
package com.dai.mif.cocoma.exception;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 89 $ ($Date:: 2010-02-18 16:56:26 +0100#$)
 */
public class ConfigException extends Exception {

	/**
     *
     */
	private static final long serialVersionUID = 8759841046354797830L;

	/**
	 * @param string
	 */
	public ConfigException(String string) {
		super(string);
	}

	/**
	 * @param string
	 * @param e
	 */
	public ConfigException(String string, Exception e) {
		super(string, e);
	}
}
