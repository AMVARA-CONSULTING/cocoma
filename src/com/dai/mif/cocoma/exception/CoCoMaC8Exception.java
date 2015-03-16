/**
 * $Id: CoCoMaC8Exception.java 89 2010-02-18 15:56:26Z rroeber $
 */
package com.dai.mif.cocoma.exception;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 4, 2010
 * @version $Revision: 89 $ ($Date:: 2010-02-18 16:56:26 +0100#$)
 */
public class CoCoMaC8Exception extends Exception {

	/**
     *
     */
	private static final long serialVersionUID = 1934300934904396286L;

	/**
	 *
	 * @param message
	 */
	public CoCoMaC8Exception(String message) {
		super(message);
	}

	/**
	 *
	 * @param message
	 * @param e
	 */
	public CoCoMaC8Exception(String message, Exception e) {
		super(message, e);
	}
}
