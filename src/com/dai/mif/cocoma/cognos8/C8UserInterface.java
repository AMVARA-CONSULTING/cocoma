/**
 * $Id: C8UserInterface.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.Account;
import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.PortalListSeparatorEnum;
import com.cognos.developer.schemas.bibus._3.PortalOption;
import com.cognos.developer.schemas.bibus._3.PortalOptionInt;
import com.cognos.developer.schemas.bibus._3.PortalOptionListSeparator;
import com.cognos.developer.schemas.bibus._3.PortalOptionSearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.PortalSkin;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.config.UIData;
import com.dai.mif.cocoma.config.UISkinData;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class encapsulates routines to modify the user interface provided by
 * Cognos 8. It allows changing the default user profile as well as creating new
 * portal page skins and assigning them to the default user profile.
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Mar 16, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class C8UserInterface {

	private C8Access c8Access;
	private Logger log;

	/**
	 * Constructor for the {@link C8UserInterface} class. It prepares logging
	 * and sets attributes for easily accessing the C8 system.
	 *
	 * @param c8Access
	 *            Instance of {@link C8Access} to be used for accessing the
	 *            Cognos 8 system.
	 */
	public C8UserInterface(C8Access c8Access) {
		this.c8Access = c8Access;
		this.log = Logging.getInstance().getLog(this.getClass());
	}

	/**
	 * Apply the settings for the user interface according to the definitions
	 * provided by the given instance of {@link UIData}.
	 *
	 * @param Instance
	 *            of {@link UIData} encapsulating information about the default
	 *            profile settings, and, in child-objects, information about
	 *            skins to be created in C8.
	 */
	public void apply(UIData uiData) throws CoCoMaC8Exception {

		// apply the skin definitions as defined in the configuration

		log.debug("Creating UI Skins.");
		List<UISkinData> uiSkins = uiData.getUISkinData();
		for (UISkinData uiSkin : uiSkins) {
			applyUiSkin(uiSkin);
		}

		// apply the settings for the default user profile
		log.debug("Updating preferences for default user profile.");
		applyDefaultSettings(uiData);

	}

	/**
	 * Apply the skin data represented by the given {@link UISkinData} instance
	 * to the Cognos 8 system. Therefore a skin entry with the relevant data is
	 * created within the styles folder for the portal skins.
	 *
	 * @param uiSkin
	 *            Instance of {@link UISkinData} encapsulating the relevant
	 *            information to create a new portal page skin in C8.
	 */
	private void applyUiSkin(UISkinData uiSkin) {

		log.debug("Creating portal skin '" + uiSkin.getName() + "'");

		PortalSkin skin = new PortalSkin();

		TokenProp nameProp = new TokenProp();
		nameProp.setValue(uiSkin.getName());

		StringProp previewProp = new StringProp();
		previewProp.setValue(uiSkin.getPreview());

		StringProp resourceLocationProp = new StringProp();
		resourceLocationProp.setValue(uiSkin.getResourceLocation());

		skin.setDefaultName(nameProp);
		skin.setPreviewImageLocation(previewProp);
		skin.setResourceLocation(resourceLocationProp);

		ContentManagerService_PortType cms = this.c8Access.getCmService();

		String parentSearchPath = "/portal/portalSkinFolder[@name='Styles']";

		SearchPathSingleObject parent = new SearchPathSingleObject();
		parent.set_value(parentSearchPath);

		AddOptions addOptions = new AddOptions();
		addOptions.setUpdateAction(UpdateActionEnum.replace);

		try {
			log.debug("Adding skin under " + parentSearchPath);
			cms.add(parent, new BaseClass[] { skin }, addOptions);
		} catch (RemoteException e) {
			String msg = "Error creating skin: " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);

			log.error(msg);
		}

	}

	/**
	 * Apply the settings defined in the given {@link UIData} object on the
	 * default profile in Cognos 8. Currently, the number of lines per page,
	 * list separation mode and the default skin are set.
	 *
	 * @throws CoCoMaC8Exception
	 *             Exception is thrown if an unexpected error with the C8 system
	 *             has been caught.
	 *
	 * @param uiData
	 *            Instance of {@link UIData} encapsulating the preferences to be
	 *            applied on the default user profile.
	 *
	 */
	private void applyDefaultSettings(UIData uiData) throws CoCoMaC8Exception {

		log.debug("Setting default user profile's properties.");

		ContentManagerService_PortType cms = c8Access.getCmService();
		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"/configuration//account");
		PropEnum[] props = new PropEnum[] { PropEnum.userName, PropEnum.name,
				PropEnum.options, PropEnum.searchPath };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();
		try {
			BaseClass[] results = cms.query(spmo, props, sort, queryOptions);

			if (results.length <= 0) {
				throw new CoCoMaC8Exception(
						"Query for default account returned 0 elements.");
			} else if (results.length > 1) {
				throw new CoCoMaC8Exception(
						"Query result for default account is ambiguous: "
								+ results.length + " results.");
			} else {
				Account acc = (Account) results[0];

				OptionArrayProp optionsProp = acc.getOptions();
				Option[] options = optionsProp.getValue();

				for (Option option : options) {
					if (option instanceof PortalOption) {
						PortalOption po = (PortalOption) option;

						String optionName = po.getName().getValue();

						if (optionName.equals("listViewSeparator")) {
							log.debug(optionName + "='"
									+ uiData.getListSeparator() + "'");
							PortalOptionListSeparator poLineSeparator = (PortalOptionListSeparator) po;
							PortalListSeparatorEnum sep = PortalListSeparatorEnum
									.fromValue(uiData.getListSeparator());
							poLineSeparator.setValue(sep);
						} else if (optionName.equals("linesPerPage")) {
							log.debug(optionName + "='"
									+ uiData.getLinesPerPage() + "'");
							PortalOptionInt poLinesPerPage = (PortalOptionInt) po;
							poLinesPerPage.setValue(uiData.getLinesPerPage());
						} else if (optionName.equals("skin")) {
							log.debug(optionName + "='" + uiData.getSkin()
									+ "'");
							PortalOptionSearchPathSingleObject poSkinPath = (PortalOptionSearchPathSingleObject) po;
							SearchPathSingleObject skinSearchPath = null;
							try {
								skinSearchPath = searchSkin(uiData.getSkin());
							} catch (Exception e) {
								String msg = "Skin '" + uiData.getSkin()
										+ "' does not exist: " + e.getMessage();
								log.error(msg);
								CoCoMa.setErrorCode(
										CoCoMa.COCOMA_ERROR_MINOR_ERROR, msg);

							}
							if (skinSearchPath != null) {
								poSkinPath.setValue(skinSearchPath);
							}
						}
					}
				}

				log.debug("Updating default profile's preferences.");

				cms.update(new BaseClass[] { acc }, new UpdateOptions());

			}

		} catch (RemoteException e) {
			String msg = "Error setting up user interface: " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);

			log.error(msg);
		}

	}

	/**
	 * Search the portal skin styles folder for a skin named by the given
	 * parameter and return the Skins searchPath as instance of
	 * {@link SearchPathSingleObject}. If the skin has not been found NULL is
	 * returned.
	 *
	 * @param skin
	 *            Name of the skin to be searched.
	 *
	 * @return Instance of {@link SearchPathSingleObject} representing the
	 *         skin's searchPath or NULL if the skin has not been found.
	 */
	private SearchPathSingleObject searchSkin(String skin)
			throws CoCoMaC8Exception {

		SearchPathSingleObject spso = null;

		ContentManagerService_PortType cms = c8Access.getCmService();
		SearchPathMultipleObject spmo = new SearchPathMultipleObject(
				"/portal/portalSkinFolder[@name='Styles']/portalSkin[@name='"
						+ skin + "']");
		PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();
		try {
			BaseClass[] results = cms.query(spmo, props, sort, queryOptions);

			if (results.length <= 0) {
				throw new CoCoMaC8Exception("Query for Skin '" + skin
						+ "' returned 0 elements.");
			} else if (results.length > 1) {
				throw new CoCoMaC8Exception("Query result for Skin '" + skin
						+ "' is ambiguous: " + results.length + " results.");
			} else {
				spso = new SearchPathSingleObject();
				spso.set_value(results[0].getSearchPath().getValue());
			}

		} catch (RemoteException e) {
			String msg = "Error setting up user interface: " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);

			log.error(msg);

		}
		return spso;
	}
}
