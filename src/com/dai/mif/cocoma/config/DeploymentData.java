/**
 * $Id: DeploymentData.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 * 
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: Stefan Brauner $
 * 
 * @since Mar 16, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class DeploymentData {

	public static final String RECORD_KEY_PREFIX = "deployments.deployment";

	// Deploymentname coming from Config file, e.g. MIF_Arbeitslisten
	private String name;

	// Deploymentname used in Filesystem of Cognos deployment folder
	private String deploymentTargetArchiveName;

	// Deploymentname used in Filesystem coming from deployment platform before
	// copying to Cognos deployment folder
	private String deploymentSrcArchiveName;

	// Deploymentname used Cognos Portal in list of deployments
	private String deploymentTargetCognosPortalArchiveName;

	// Deploymentname used in ZIP-Archive ... this is the original export
	// archive name
	private String deploymentNameOfArchiveInZipDeploymentFile;

	// Deploymentname used in Filesystem coming from deployment platform before
	// copying to Cognos deployment folder
	private String deploymentTargetFolder;

	private String password;

	private String recordingLevel;

	private String mailSender;

	private String mailRecipient;

	private String mailSubject;

	private String mailText;

	private String runCureJar;

	private String runCureJar_searchPath;

	private String runCureJar_PackagePath;

	private DeploymentOption[] deploymentOptions;

	private DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();

	private Boolean displayHistoryAfterDeployment;

	/**
	 * Physical path to the archive
	 */
	private String archive;

	public static final int DEPLOYMENT_STATUS_PENDING = 0;
	public static final int DEPLOYMENT_STATUS_SUCCESS = 1;
	public static final int DEPLOYMENT_STATUS_ERROR = 2;

	/**
	 * Deployment-Status 1 = Success 0 = Pending 2 = Error
	 */
	private int status;

	/**
	 * Timestamp which is used for renaming the archive after the deployment.
	 * 
	 * XML-Node: prefix_archive_after_deployent
	 */
	private String timestampPrefix;

	private String deploymentFolder;

	private XMLConfiguration conf;

	private String configKey;

	private Logger log;

	private ArrayList<String> deleteItems;

	private String name_set_datetime_suffix;

	/*
	 * Searchpath of item to be deployed
	 */
	private String deploymentSearchpath;

	/**
	 * @param deploymentName
	 */
	public DeploymentData(String deploymentName) {
		this.name = deploymentName;
		this.status = DEPLOYMENT_STATUS_PENDING;
	}

	/**
	 * @param conf
	 * @param configKey
	 */
	public void readConfig(XMLConfiguration conf, String configKey)
			throws ConfigException {

		this.conf = conf;
		this.configKey = configKey;
		this.log = Logging.getInstance().getLog(this.getClass());

		log.debug("Reading Config for Deployment Data");

		// name_set_datetime_suffice
		this.name_set_datetime_suffix = conf.getString(configKey
				+ ".name_set_datetime_suffix");

		// deploymentfolder
		this.deploymentFolder = conf.getString("server.deploymentFolder");

		Cryptography crypt = Cryptography.getInstance();
		String cryptedPass = "";

		// password
		try {
			cryptedPass = conf.getString(configKey + ".password", null);
			this.password = crypt.decrypt(cryptedPass);
		} catch (Exception e) {
			this.status = DEPLOYMENT_STATUS_ERROR;
			if (!CoCoMa.isInteractiveMode()) {
				throw new ConfigException(
						"Error decrypting the deployment password: "
								+ e.getMessage(), e);
			} else {
				this.password = null;
			}
		}
		if (cryptedPass.length() > 0 && this.password == null) {
			log.error("Error decrypting the deployment password");
			this.status = DEPLOYMENT_STATUS_ERROR;
		}

		this.recordingLevel = conf.getString(configKey + ".recordingLevel");
		this.timestampPrefix = conf.getString(configKey
				+ ".prefix_archive_after_deployent", "");
		this.archive = conf.getString(configKey + ".archive", "");

		// read Curejar items
		this.runCureJar = conf.getString(configKey + ".runCureJar", "");
		this.runCureJar_searchPath = conf.getString(configKey
				+ ".runCureJar_searchPath", "");
		this.runCureJar_PackagePath = conf.getString(configKey
				+ ".runCureJar_PackagePath", "");

		this.mailSender = conf.getString(configKey + ".mail_sender", "");
		this.mailRecipient = conf.getString(configKey + ".mail_recipient", "");
		this.mailSubject = conf.getString(configKey + ".mail_subject", "");
		this.mailText = conf.getString(configKey + ".mail_text", "");

		// Read Delete items
		log.debug("Looking for config entry 'delete items': " + configKey
				+ ".delete_items.item");

		List<?> deleteItemsDataList = conf.getList(configKey
				+ ".delete_items.item");
		this.deleteItems = new ArrayList<String>();
		for (int i = 0; i < deleteItemsDataList.size(); i++) {
			log.debug("Item Value  :" + deleteItemsDataList.get(i));
			this.deleteItems.add(deleteItemsDataList.get(i).toString());
		}
		log.debug("Found number of deleteItems config entries: "
				+ this.deleteItems.size());

		// Read DisplayHistoryAfterDeployment
		String temp = conf.getString(configKey
				+ ".show_job_history_after_deployment");
		if (temp == null) {
			this.displayHistoryAfterDeployment = true;
			log.debug("No XML-Tag found for: show_job_history_after_deployment ... using default value");
		} else {
			if (temp.equalsIgnoreCase("false")) {
				log.debug("setting option to false");
				setDisplayHistoryAfterDeployment(false);
			} else {
				log.debug("setting option to true (" + temp + ")");
				setDisplayHistoryAfterDeployment(true);
			}
		}

		log.debug("show_job_history_after_deployment Option: "
				+ this.getDisplayHistoryAfterDeployment());

	}

	/**
	 * get_nameSetDateTimeSuffix()
	 * 
	 * @return String from Configfile
	 */
	public String get_nameSetDateTimeSuffix() {
		return this.name_set_datetime_suffix;
	}

	/**
	 * getDeleteItems()
	 * 
	 * @return ArrayList<String> of Items to be deleted
	 */
	public ArrayList<String> getDeleteItems() {
		return this.deleteItems;
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setter for the name field
	 * 
	 * @param name
	 *            the new value to set for the name field
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return
	 */
	public String getPassword() {
		return this.password;
	}

	public String getRunCureJar() {
		return this.runCureJar;
	}

	public String getRunCureJar_packagePath() {
		return this.runCureJar_PackagePath;
	}

	public String getRunCureJar_searchPath() {
		return this.runCureJar_searchPath;
	}

	public String getDeploymentFolder() {
		return this.deploymentFolder;
	}

	/**
	 * @return
	 */
	public String getRecordingLevel() {
		return this.recordingLevel;
	}

	public String getArchive() {
		return this.archive;
	}

	/**
	 * Getter for the mailSender field
	 * 
	 * @return the currently set value for mailSender
	 */
	public String getMailSender() {
		return mailSender;
	}

	/**
	 * Getter for the mailRecipient field
	 * 
	 * @return the currently set value for mailRecipient
	 */
	public String getMailRecipient() {
		return mailRecipient;
	}

	/**
	 * Getter for the mailSubject field
	 * 
	 * @return the currently set value for mailSubject
	 */
	public String getMailSubject() {
		return mailSubject;
	}

	/**
	 * Getter for the mailText field
	 * 
	 * @return the currently set value for mailText
	 */
	public String getMailText() {
		return mailText;
	}

	/**
	 * Setter for the archive field
	 * 
	 * @param archive
	 *            the new value to set for the archive field
	 */
	public void setArchive(String archive) {
		this.archive = archive;
	}

	/**
	 * @return
	 */
	public String getTimestampPrefix() {
		return this.timestampPrefix;
	}

	/**
	 * Setting password for deployment archive
	 * 
	 * @param password
	 * @throws ConfigException
	 */
	public void setPassword(String password) throws ConfigException {

		Cryptography crypt = Cryptography.getInstance();

		try {
			conf.setProperty(configKey + ".password", crypt.encrypt(password));
			conf.save();
			conf.reload();
		} catch (ConfigurationException e) {
			throw new ConfigException("Error saving the password: "
					+ e.getMessage());
		}

	}

	/**
	 * Getter for the status field
	 * 
	 * @return the currently set value for status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Setter for the status field
	 * 
	 * @param status
	 *            the new value to set for the status field
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @param deploymentNameOfArchiveInZipDeploymentFile
	 *            the deploymentNameOfArchiveInZipDeploymentFile to set
	 */
	public void setDeploymentNameOfArchiveInZipDeploymentFile(
			String deploymentNameOfArchiveInZipDeploymentFile) {
		this.deploymentNameOfArchiveInZipDeploymentFile = deploymentNameOfArchiveInZipDeploymentFile;
	}

	/**
	 * @return the deploymentNameOfArchiveInZipDeploymentFile
	 */
	public String getDeploymentNameOfArchiveInZipDeploymentFile() {
		return deploymentNameOfArchiveInZipDeploymentFile;
	}

	/**
	 * @param deploymentTargetCognosPortalArchiveName
	 *            the deploymentTargetCognosPortalArchiveName to set
	 */
	public void setDeploymentTargetCognosPortalArchiveName(
			String deploymentTargetCognosPortalArchiveName) {
		this.deploymentTargetCognosPortalArchiveName = deploymentTargetCognosPortalArchiveName;
	}

	/**
	 * @return the deploymentTargetCognosPortalArchiveName
	 */
	public String getDeploymentTargetCognosPortalArchiveName() {
		return deploymentTargetCognosPortalArchiveName;
	}

	/**
	 * @param deploymentTargetArchiveName
	 *            the deploymentTargetArchiveName to set
	 */
	public void setDeploymentTargetArchiveName(
			String deploymentTargetArchiveName) {
		this.deploymentTargetArchiveName = deploymentTargetArchiveName;
	}

	/**
	 * @return the deploymentTargetArchiveName
	 */
	public String getDeploymentTargetArchiveName() {
		return deploymentTargetArchiveName;
	}

	/**
	 * @param deploymentSrcArchiveName
	 *            the deploymentSrcArchiveName to set
	 */
	public void setDeploymentSrcArchiveName(String deploymentSrcArchiveName) {
		this.deploymentSrcArchiveName = deploymentSrcArchiveName;
	}

	/**
	 * @return the deploymentSrcArchiveName
	 */
	public String getDeploymentSrcArchiveName() {
		return deploymentSrcArchiveName;
	}

	/**
	 * @param deploymentTargetFolder
	 *            the deploymentTargetFolder to set
	 */
	public void setDeploymentTargetFolder(String deploymentTargetFolder) {
		this.deploymentTargetFolder = deploymentTargetFolder;
	}

	/**
	 * @return the deploymentTargetFolder
	 */
	public String getDeploymentTargetFolder() {
		return deploymentTargetFolder;
	}

	/**
	 * @param deploymentOptions
	 *            the deploymentOptions to set
	 */
	public void setDeploymentOptions(DeploymentOption[] deploymentOptions) {
		this.deploymentOptions = deploymentOptions;
	}

	/**
	 * @return the deploymentOptions
	 */
	public DeploymentOption[] getDeploymentOptions() {
		return deploymentOptions;
	}

	/**
	 * @param archiveEncryptPassword
	 *            the archiveEncryptPassword to set
	 */
	public void setArchiveEncryptPassword(
			DeploymentOptionString archiveEncryptPassword) {
		this.archiveEncryptPassword = archiveEncryptPassword;
	}

	/**
	 * @return the archiveEncryptPassword
	 */
	public DeploymentOptionString getArchiveEncryptPassword() {
		return archiveEncryptPassword;
	}

	/**
	 * @param displayHistoryAfterDeployment
	 *            the displayHistoryAfterDeployment to set
	 */
	public void setDisplayHistoryAfterDeployment(
			Boolean displayHistoryAfterDeployment) {
		this.displayHistoryAfterDeployment = displayHistoryAfterDeployment;
	}

	/**
	 * @return the displayHistoryAfterDeployment
	 */
	public Boolean getDisplayHistoryAfterDeployment() {
		if (this.displayHistoryAfterDeployment == null)
			this.displayHistoryAfterDeployment = true;
		return displayHistoryAfterDeployment;
	}

	/**
	 * @param deploySearchpath the deploySearchpath to set
	 */
	public void setDeploymentSearchpath(String deploySearchpath) {
		this.deploymentSearchpath = deploySearchpath;
	}

	/**
	 * @return the deploySearchpath
	 */
	public String getDeploymentSearchpath() {
		return deploymentSearchpath;
	}

}
