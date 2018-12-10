package com.dai.mif.cocoma;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;
// import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.cognos8.C8Capabilities;
import com.dai.mif.cocoma.cognos8.C8ContentRestriction;
import com.dai.mif.cocoma.cognos8.C8DataSource;
import com.dai.mif.cocoma.cognos8.C8Deployment;
import com.dai.mif.cocoma.cognos8.C8Dispatcher;
import com.dai.mif.cocoma.cognos8.C8ExportDeployment;
import com.dai.mif.cocoma.cognos8.CognosSecurity;
import com.dai.mif.cocoma.cognos8.C8UserInterface;
import com.dai.mif.cocoma.cognos8.CognosUpdateReport;
import com.dai.mif.cocoma.config.CapabilityData;
import com.dai.mif.cocoma.config.CoCoMaConfiguration;
import com.dai.mif.cocoma.config.DataSourceData;
import com.dai.mif.cocoma.config.DeploymentData;
import com.dai.mif.cocoma.config.DispatcherData;
import com.dai.mif.cocoma.config.MailserverData;
import com.dai.mif.cocoma.config.RestrictedContentData;
import com.dai.mif.cocoma.config.SecurityData;
import com.dai.mif.cocoma.config.ServerData;
import com.dai.mif.cocoma.config.UIData;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 * Main Class of the CoCoMa Project. CoCoMa (Cognos Configuration Manager) is an
 * implementation to automatically create certain configuration settings based
 * on an XML-based config file. This file defines which server to connect to,
 * which dataSources and roles to create as well as permissions and capabilities
 * to be set.
 * 
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: Stefan Brauner, Ralf Roeber $
 * 
 * @since 02.02.2010
 * @version $Revision: 169 $ ($Date:: 2011-09-01 09:29:00 +0200#$)
 */
public class CoCoMa {

	private static String productName = "CoCoMa - Cognos Configuration Manager";
	private static String productVersion = "v3.1";
	private static String productRevision = "Build: @@Cognos 10.2.1/2018-12-10_1545/394@@ ";
	
	/** Mail Address displayed in CoCoMa Help text **/
	/* maybe overwritten by commandline argument --mailto */
	public static String COCOMA_MAILTO_HELPTEXT = "mbox-096-mif-betrieb@daimler.com; mbox_mif_dcxam@daimler.com";

	/** UID used as key for password encryption */
	private static final String COCOMA_UID = "07369e30-1175-11df-8a39-0800200c9a66";

	/**
	 * Error code denoting that an error occurred but the application could
	 * continue its work.
	 */
	public static final int COCOMA_ERROR_MINOR_ERROR = 1;

	/**
	 * Error code denoting that a critical error occurred which renders the
	 * applications's result as unusable.
	 */
	public static final int COCOMA_ERROR_CRTICAL_ERROR = 2;

	/**
	 * The default file name for the configuration - can be overridden via
	 * command line.
	 */
	private static String configFileName = "COCOMA_CONFIG_LOCAL.xml";

	private static boolean interactiveMode = false;

	private static boolean infoMode = false;

	private static boolean checkConfigMode = false;

	private static boolean checkDispatcherInformation = false;

	private static boolean consoleLogging = false;

	/*
	 * If using --crypt on commandline, this variable stores the cleartext password as next
	 * argument from commandline
	 */
	private static String crypt = "";

	private static Logger log;

	/** Holds the current configuration data encapsulated in proper Objects. */
	private CoCoMaConfiguration configuration;

	private static int errorCode = 0;
	private static List<String> errorLog;
	private static boolean dumpAccounts = false;

	/**
	 * Convenience method to extract the acutal command from a command line
	 * argument. This program currently support thre ways of defining a command
	 * line argument: prefixed by --, - or /.
	 * 
	 * @param arg
	 *            The argument from which the actual command is to be extracted
	 * @return The extracted command, or the original argument value, if the
	 *         command could not be extracted.
	 */
	private static String extractCommandLineSwitch(String arg) {

		String command = arg;

		if (command.startsWith("--")) {
			command = arg.substring(2);
		} else if (command.startsWith("-")) {
			command = arg.substring(1);
		} else if (command.startsWith("/")) {
			command = arg.substring(1);
		}
		
		return command.toLowerCase();
	}

	/**
	 * Construct a string showing help information for this program. It shows
	 * all recognized command line arguments and gives a short description of
	 * the features available via command line.
	 * 
	 * @return String to be displayed as program help.
	 */
	private static String getHelpString() {
		String lf = System.getProperty("line.separator");

		String helpString = "Recognized options:" + lf + lf;
		helpString += "--version" + lf + "shows the product version" + lf + lf;
		helpString += "--help" + lf + "shows this help text" + lf + lf;
		helpString += "--config <fileName>"
				+ lf
				+ "defines <fileName> to be used as configuration file. If this option is ommited, "
				+ "CoCoMa.xml is assumed as default value." + lf + lf;
		helpString += "--setpass"
				+ lf
				+ "starts an interactive mode where all passwords are queried via prompt and afterwards saved "
				+ "as encrypted password in the config." + lf + lf;
		helpString += "--check"
				+ lf
				+ "checks the configuration file for syntax errors and prints out the result"
				+ lf + lf;
		helpString += "--dispatcherinfo" + lf
				+ "reads config file and checks dispatcher for information"
				+ lf + lf;
		helpString += "--console"
				+ lf
				+ "prints the log messages to console as well as to the config file"
				+ lf + lf;
		helpString += "--phasebasic"
				+ lf
				+ "enters the basic configuration phase to set up general parameters for dispatchers, "
				+ "Cognos 8 security etc." + lf + lf;
		helpString += "--phasedeployment" + lf
				+ "performs deployments for the configured system" + lf + lf;
		helpString += "--phasecontent"
				+ lf
				+ "enters content configuration phase where security settings are applied to the "
				+ "various content objects" + lf + lf;
		helpString += "--crypt <password in cleartext>"
			+ lf
			+ "read cleartext password from commandline and returns crypted password." 
			+ "Be careful using this option. Password may be stored in commandline history. " 
			+ "Better use --setpass as option. This will read password from console and " 
			+ "store the crypted password to the right place in your config.xml."
			+ lf + lf;
		helpString += "--donotaskforbackup"
			+ lf
			+ "Does not ask to confirm 'YES, I HAVE A BACKUP'. " 
			+ "Better set create-fullbackup option in config file to true. " 
			+ "" + lf + lf;
		helpString += "--mailto emailaddress@domain"
				+ lf
				+ "eMail being displayed in helptext information. " 
				+ "" + lf + lf;
		helpString += "Almost all options may be combined, e.g. java -jar CoCoMa.jar --dispatcherinfo --config COCOMA_CONFIG_LOCAL.xml --donotaskforbackup"
				+ lf
				+ "See CoCoMa documentation for details on howto configure config.xml files." + lf
				+ "---------- end of help information  --------------------------------" 
				+ "" 
				+ "" + lf + lf;
		return helpString;
	}

	/**
	 * Main method as entry point for this program.
	 * 
	 * @param args
	 *            Command line arguments passed when program was started.
	 */
	public static void main(String[] args) {

		// initialize logging so that it can be used from now on
		Logging logging = Logging.getInstance();

		// set the logging mode according to the current state of the static
		// console logging attribute
		logging.setConsoleLogging(consoleLogging);
		log = logging.getLog(CoCoMa.class);
		
		// Show programminformation
		CoCoMa.show_programinformation();


		// at first check any command line arguments
		readCommandlineArguments(args);

		if (CoCoMa.getCrypt().length()>0)  {
			log.info("==> Better use commandline switch --setpass! <==");
			log.info("Password to crypt: "+CoCoMa.getCrypt());
			// prepare crypt module
			Cryptography cryptClass = Cryptography.initialize(COCOMA_UID);
			String encryptedPw = cryptClass.encrypt(CoCoMa.getCrypt());
			log.info("Encrypted Password: "+encryptedPw);
			log.info("Cut and paste this password into the config.xml file of your choice.");
			System.exit(0);
		}
		
		
		
		
		if (infoMode) {

			showProductInfo();

		} else {

			CoCoMa.errorLog = new ArrayList<String>();

			// create an instance of the main program
			CoCoMa cocoma = new CoCoMa();
			C8Access c8Access = cocoma.prepare();

			if (CoCoMa.isDumpAccounts() == true) {
				C8Utility c8util = new C8Utility(c8Access);
				c8util.dumpAccounts();
				log.info("done.");
				System.exit(0);
			}

			// see if run in interactive mode to set the passwords or in
			// standard mode to actually run

			if (checkDispatcherInformation) {
				log.info("Check DispatcherInformation mode: true");
				C8Utility c8check = new C8Utility(c8Access);
				if (c8Access != null) {
					if (c8Access.isConnected()) {
						c8check.queryDispatcher();
						log.debug("Need more information? Send request to ralf.roeber@amvara.de");
						log.debug("------------------------------------------------------------");
					}
				} else {
					log.error("C8 access is null");
				}
			}

			if (!checkConfigMode) {

				if (interactiveMode) {

					try {

						cocoma.configuration.setServerPassword();
						cocoma.configuration.setMailserverPassword();
						cocoma.configuration.setBackupPassword();
						cocoma.configuration.setDeploymentPasswords();
						cocoma.configuration.setDataSourcesPasswords();

					} catch (ConfigException ce) {
						String msg = ce.getMessage();
						CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR,
								msg);
						log.error(msg);
						log.error(msg, ce);

						// only in this case, when in interactive mode, print
						// the error message to STDERR.
						System.err.println(ce.getMessage());
					}

				} else {

					// if preparation was successful, start the actual work
					if (c8Access != null) {
						if (c8Access.isConnected()) {

							// do the work
							cocoma.process(c8Access);

							// and disconnect afterwards
							c8Access.disconnect();
						} else {
							String msg = "Preparation successfull, but not connected to the server";
							CoCoMa.setErrorCode(
									CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
							log.error(msg);
						}
					} else {
						String msg = "Preparation failed.";
						CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR,
								msg);
						log.error(msg);
					}
				}
			}

			log.info("Ending program with exit code " + CoCoMa.errorCode);

			if (log.isDebugEnabled() && (CoCoMa.errorCode > 0)) {
				log.debug("The following errors occured during run:");
				for (String message : CoCoMa.errorLog) {
					log.debug(message);
				}
			}

			log.debug("Thank you for using CognosConfigurationManager.");
			log.debug("Send questions or suggestions to "+COCOMA_MAILTO_HELPTEXT+" .");
			log.info("end");

		}

		// exit with the error code stored in the static field CoCoMa.errorCode
		System.exit(errorCode);

	}

	/**
	 * Construct a string containing information about the product (name,
	 * version, SVN revision etc).
	 * 
	 * @return A String containing the product version information.
	 */
	private static String getVersionString() {
		return productName + " " + productVersion;
	}

	private static String getRevisionString() {
		return productRevision;
	}

	private static boolean phaseBasicConfiguration = false;
	private static boolean phaseAdvancedDispatcherSetting = false;
	private static boolean phaseDeployment = false;
	private static boolean phaseContentConfiguration = false;
	private static boolean phaseBackupDonotAsk = false;

	/**
	 * Start the actual work by applying the configured data to the C8 system to
	 * which the connection is currently established.
	 * 
	 * @param c8Access
	 *            C8Access instance that provides access to the currently
	 *            connected C8 system
	 */
	private void process(C8Access c8Access) {

		// create full content store backup ... always usefull before any changes destroy your environment.
		if (this.configuration.getBackupData().isEnabled()) {
			log.info("----------------------------");
			log.info("Creating Contentstore Backup");
			C8ExportDeployment export = new C8ExportDeployment(c8Access,
					this.configuration.getBackupData());
			export.createExport();
			log.info("done");
			log.info("----------------------------");
		}

		// Adv. Dispatcher Settings
		if (phaseAdvancedDispatcherSetting) {
			log.info("Perfoming content phaseAdvancedDispatcherSetting");
			processAdvancedDispatcherSetting(c8Access);
			List<DispatcherData> dispatchers = this.configuration.getDispatchers();
			if (dispatchers.size() != 0) {
				log.info("Trying to configuring dispatchers");
				log.info("There are " + dispatchers.size()
						+ " dispatchers to be configured.");
				// If there is more than 0 dispatchers in Config file ... go ahead
				for (DispatcherData dispatcherData : dispatchers) {
					C8Dispatcher dispatcher = new C8Dispatcher(dispatcherData,
							c8Access);
					String dispatcherName = dispatcherData.getDispatcherName();
					if (dispatcherData.getAdvancedParameters().length > 0) {
						log.info("Applying "
								+ dispatcherData.getAdvancedParameters().length
								+ " Adv.Settings on Dispatcher:" + dispatcherName
								+ " ");
						try {
							dispatcher.setAdvancedDispatcherSettings();
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						log.info("No advanced Dispatcher Settings found in configfile. ");
						log.info("if you want to use adv. dispatcher settings, add them to your config-file");
						log.info("Add in <dispatcher>-Section ... "
								+ "<advancedSettings><name>CM.OUTPUT</name><value>c:\\temp\\</value></advancedSettings> "
								+ "to configfile, to configure the global settings above any dispatcher.");
					}
				}
			} else {
				log.info("No Dispatcher configuration settings in Configfile. Nothing to set.");
			}
		} else {
			log.info("Skipping content phaseAdvancedDispatcherSetting.");
		}

		try {

			log.info("Starting processing of content store ...");

			// basic configuration to set up the dataSources, dispatchers, and
			// security objects

			if (phaseBasicConfiguration) {
				log.info("Performing basic configuration.");
				processBasicConfiguration(c8Access);
			} else {
				log.info("Skipping basic configuration phase.");
			}


			// deployments to be performed by CoCoMa

			if (phaseDeployment) {
				log.info("Processing deployments");
				processDeployments(c8Access);
			} else {
				log.info("Skipping deployment phase.");
			}

			// content configuration to apply security options to the current
			// content

			if (phaseContentConfiguration) {
				log.info("Perfoming content configuration");
				processContentConfiguration(c8Access);
			} else {
				log.info("Skipping content configuration phase.");
			}

			// --- exit with the proper exit codes
			if (CoCoMa.errorCode == 0) {
				log.info("Successfully finished processing of content store.");
			} else {
				log.info("Finished processing of content store, but errors occurred during operations.");
			}

			// Mail notification
			if (phaseDeployment && !interactiveMode && !checkConfigMode) {

				log.debug("Check, if mail notification has been configured.");
				
				MailserverData mailServer = this.configuration
						.getMailserverData();

				for (DeploymentData deployment : this.configuration
						.getDeployments()) {
					if (deployment.getMailRecipient().length() > 0) {
						log.debug("Mail recipient: "
								+ deployment.getMailRecipient());

						String subject = mailServer.fillPlaceholders(
								deployment.getMailSubject(), deployment,
								productVersion);
						log.debug("Mail subject: " + subject);

						String mailtext = mailServer.fillPlaceholders(
								deployment.getMailText(), deployment,
								productVersion);

						log.debug("Sending mail(s) ... ");
						mailServer.sendMail(subject, mailtext,
								deployment.getMailSender(),
								deployment.getMailRecipient());
					} else {
						log.debug("No recipient for email in config file. Not sending any mail(s).");
					}
				}
			}
				
		} catch (CoCoMaC8Exception c8e) {
			String msg = c8e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
			log.error(msg, c8e);
		}
	}

	/**
	 * @param processAdvancedDispatcherSetting
	 *            (C8Access c8Access)
	 */
	private void processAdvancedDispatcherSetting(C8Access c8Access) {
		log.info("started processAdvancedDispatcherSetting");
	}

	/**
	 * processDeployments(C8Access c8Access)
	 * 
	 * @param c8Access
	 */
	private void processDeployments(C8Access c8Access) throws CoCoMaC8Exception {
		List<DeploymentData> deployments = this.configuration.getDeployments();
		if (!deployments.isEmpty()) {
			if (deployments.size() == 1) {
				log.info("There is one deployment to be executed.");
			} else {
				log.info("There are " + deployments.size()
						+ " deployment to be executed.");
			}

			String lf = System.getProperty("line.separator");
			boolean doDeployment = true;

			// Ask for backup if autobackup is not enabled
			if (this.configuration.getBackupData().isEnabled() == false && phaseBackupDonotAsk == false) {
				log.debug("Autobackup was disabled in configfile. Ask for backup!");
				String answer = "YES, I HAVE A BACKUP";
				doDeployment = showDeploymentConfirmationPrompt(
						lf
								+ "Are you sure, you want to execute the deployments?"
								+ lf
								+ "You should only do so, if you have created a backup of the content store before."
								+ lf
								+ "Disable this question: use --donotaskforbackup on commandline "
								+ lf
								+ "Or configure backup section in config.xml, setting backup to true."
								+ lf
								+ "If you really want to execute the deployments, please answer: "
								+ answer + lf + lf + "Your answer: ", answer);

			} else {
				log.debug("Autobackup was enabled in configfile. Not asking for backup.");
			}

			if (doDeployment) {
				int depl_number = 0;

				for (DeploymentData depData : deployments) {
					depl_number++;

					log.info("------------------------------------");
					log.info("Processing deployment '" + depData.getName()
							+ "'" + "( " + depl_number + " of "
							+ deployments.size() + ")");
					log.info("------------------------------------");
					// Check status of last deployment
					if (depData.getStatus() == DeploymentData.DEPLOYMENT_STATUS_ERROR) {
						log.info("Skipping deployment \""
								+ depData.getName()
								+ "\", because of an error that occured previously.");
						continue;
					}

					// Items to delete before deployment
					ArrayList<String> deleteItems = depData.getDeleteItems();
					if (deleteItems.size() > 0) {
						log.info("Found "
								+ deleteItems.size()
								+ " configured items to delete. Looping over items to delete before deployment.");
						// Loop over deleteItems form Config
						for (int i = 0; i < deleteItems.size(); i++) {
							String deleteItem = deleteItems.get(i);
							log.info("Delete item: " + deleteItem);
							C8Utility c8utility = new C8Utility(c8Access);
							// remove item
							if (c8utility.removeItems(deleteItem)) {
								log.info("was removed");
							} else {
								log.error("Error occured removing item.");
								depData.setStatus(DeploymentData.DEPLOYMENT_STATUS_ERROR);
							}
						}
					} else {
						log.debug("No items to delete in config .. nothing to delete");
					}

					// Execute Deployment
					log.debug("Executing deployment '" + depData.getName()
							+ "'" + "( " + depl_number + " of "
							+ deployments.size() + ")");
					C8Deployment deployment = new C8Deployment(depData,
							c8Access);
					log.info("Deployment      : " + depData.getName());
					log.info("Deploymentfolder: "
							+ depData.getDeploymentFolder());
					log.info("Archive         : " + depData.getArchive());
					
					deployment.execute();
					log.info("Done executing deployment '" + depData.getName()
							+ "'" + "( " + depl_number + " of "
							+ deployments.size() + ")");

					// Run Cure.Jar after Deployment
					if (depData.getRunCureJar().equalsIgnoreCase("true")) {
						log.info("Run cure.Jar = true");
						CognosUpdateReport cure = new CognosUpdateReport();
						try {
							cure.updateQueryReport(c8Access,
									depData.getRunCureJar_searchPath(),
									depData.getRunCureJar_packagePath());
						} catch (Exception e) {

							log.error("Severe error!");
							log.error(e.getStackTrace());
							e.printStackTrace();
							depData.setStatus(DeploymentData.DEPLOYMENT_STATUS_ERROR);
						}
					} else {
						log.info("Run cure.Jar = false");
						log.info("Set options in config.xml to execute cure.jar and set all reports to use 'modellLast()' version.");
					}
				}
			} else {
				log.info("BackupQuestion has not been confirmed. No deployments will be executed.");

			}
		} else {
			log.debug("No deployment-tag found in XML config-file. Nothing to dpeloy.");
		}
	}

	private static void show_programinformation() {
		// show information about the program itself
		log.info("---------------------------------------------------o_o-");
		log.info("Welcome to CoCoMa for IBM Cognos 10.x ");
		log.info("(C) 2011-2016 Ralf Roeber, AMVARA Consulting, Barcelona");
		log.info(getVersionString());
		log.info(getRevisionString());
		log.info("Use --help to see options.");
		log.info("---------------------------------------------------^o^-");

	}

	/**
	 * Prepare the actual work by reading the configuration from the config file
	 * and establishing the connection to the c8 server.
	 * 
	 * @return A reference to the C8Access instance to be used for the further
	 *         access to the c8 server
	 * @see com.dai.mif.cocoma.cognos.util.C8Access
	 */
	private C8Access prepare() {

		// prepare a reference to be returned by this method
		C8Access access = null;

		// prepare crypt module
		@SuppressWarnings("unused")
		Cryptography crypt = Cryptography.initialize(COCOMA_UID);

		// check the config file
		File configFile = new File(configFileName);

		if (configFile.exists()) {
			if (configFile.canRead()) {

				// try to actually load the config
				try {
					log.info("Reading configuration from file "
							+ configFileName);
					this.configuration = new CoCoMaConfiguration();

					this.configuration.read(configFile);
					log.info("Config file version: "
							+ this.configuration.getVersion());

					if (checkConfigMode) {
						String configCheckResult = "Configuration in "
								+ configFileName + " is ok.";
						log.info(configCheckResult);
					}

					ServerData serverData = this.configuration.getServerData();

					if ((!interactiveMode) && (!checkConfigMode)) {
						log.info("Running in standard mode");
						// prepare access to the c8 system
						access = new C8Access(serverData.getDispatcherURL(),
								serverData.getNameSpace(),
								serverData.getUserName(),
								serverData.getPassword());

						log.info("Connecting to server "
								+ serverData.getDispatcherURL());
						log.info("Cognos Version: "
								+ serverData.getServerVersion());
						log.info("With user " + serverData.getUserName());

						boolean connected = false;
						if (serverData.getServerVersion().equals("10")) {
							try {
								connected = access
										.ConnectToCognosServer(serverData
												.getDispatcherURL());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							log.info("No methods to connect to server other than 10. Set <version>10</version> in <server>-section");
							System.exit(2);
						}

						if (connected) {
							log.info("Successfully connected to the server");
							log.debug("Connected to "
									+ serverData.getDispatcherURL()
									+ " with user " + serverData.getUserName());
						} else {
							log.warn("Not connected to the server");
						}
					} else {
						log.info("Running in interactive mode");
					}

				} catch (ConfigurationException e) {
					access = null;
					String msg = "Error reading the configuration: "
							+ e.getMessage();
					CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
					log.error(msg);
					log.error(e.getMessage(), e);

					if (checkConfigMode) {
						String configCheckResult = "Configuration in "
								+ configFileName + " is invalid: "
								+ e.getMessage();
						System.out.println(configCheckResult);
					}
				} catch (ConfigException e) {
					access = null;
					String msg = "Invalid configuration data: "
							+ e.getMessage();
					CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
					log.error(msg);
					log.error(e.getMessage(), e);

					if (checkConfigMode) {
						String configCheckResult = "Configuration in "
								+ configFileName + " is invalid: "
								+ e.getMessage();
						System.out.println(configCheckResult);
					}
				}

			} else {
				access = null;
				String msg = "The config file exists but cannot be read. ("
						+ configFile.getAbsolutePath() + ")";
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
				log.error(msg);
			}
		} else {
			access = null;
			String msg = "The config file does not exist. ("
					+ configFile.getAbsolutePath() + ")";
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}

		// return the access reference in its current state
		return access;
	}

	/**
	 * @param c10Access
	 * @throws CoCoMaC8Exception
	 */
	private void processContentConfiguration(C8Access c8Access)
			throws CoCoMaC8Exception {
		// --- set the security on the content of the Cognos 8 system
		SecurityData secData = this.configuration.getSecurityData();
		if (secData.getSecurityTagFoundInXMLConfig()) {
			log.info("Setting up content security");
	
			CognosSecurity security = new CognosSecurity(secData, c8Access);
			security.applyContentSecurity();
	
			log.info("Finished setting up content security");
		}
		
		// --- set the visibility of content

		RestrictedContentData limitedContentData = this.configuration
				.getRestrictedContentData();
		if (limitedContentData.getRestrictionsDefined()) {
			log.info("Setting up content restrictions");
			C8ContentRestriction visibility = new C8ContentRestriction(
					limitedContentData, c8Access);
			visibility.applyContentRestriction();
			log.info("Finished setting up content restrictions");
		}
	}

	/**
	 * @param c8Access
	 * @throws CoCoMaC8Exception
	 */
	private void processBasicConfiguration(C8Access c8Access)
			throws CoCoMaC8Exception {
		// --- create the data sources defined in the configuration

		log.info("Processing data sources");

		List<DataSourceData> dataSources = this.configuration.getDataSources();

		log.debug("There are " + dataSources.size()
				+ " data sources to be processed.");

		for (DataSourceData dsData : dataSources) {
			C8DataSource dataSource = new C8DataSource(dsData, c8Access);
			// for now set all data sources to asynchronous mode
			dataSource.create(true);
		}

		log.info("Finished processing data sources");

		// --- set up process and affinity parameters

		log.info("Configuring dispatchers");

		List<DispatcherData> dispatchers = this.configuration.getDispatchers();

		log.debug("There are " + dispatchers.size()
				+ " dispatchers to be configured.");

		for (DispatcherData dispatcherData : dispatchers) {
			C8Dispatcher dispatcher = new C8Dispatcher(dispatcherData, c8Access);
			dispatcher.updateDispatcherParameters();
		}

		log.info("Finished configuring dispatchers");

		// --- set up the security
		SecurityData secData = this.configuration.getSecurityData();
		if (secData.getSecurityTagFoundInXMLConfig()) {

			// --- setup roles defined in the configuration

			log.debug("XML Configfile contains security tag ... will set Cognos role security now ...");
			CognosSecurity security = new CognosSecurity(secData, c8Access);
			security.applyCognosSecurity();
			log.info("Finished setting up Cognos security");

			// --- set up the capabilities

			log.info("Setting up Cognos capabilities");

			List<CapabilityData> capabilities = this.configuration
					.getCapabilities();
			C8Capabilities c8Capabilites = new C8Capabilities(c8Access);
			c8Capabilites.applySecuredFunctionPermissions(capabilities);

			log.info("Finished setting up Cognos capabilities");
		} else {
			log.debug("No security tag found on XML config-file. Nothing to set.");
		}

		// --- set up UI preferences

		UIData uiData = this.configuration.getUIData();

		if (uiData.getUItagFoundInXmlConfig()) {
			log.info("Setting up user interface");
			C8UserInterface c8UI = new C8UserInterface(c8Access);
			c8UI.apply(uiData);
			log.info("Finished setting up user interface");
		} else {
			log.debug("No UI-tag found in XML config file. Nothing to set on UI.");
		}

	}

	/**
	 * Parse command line arguments to set certain operating flags, such as
	 * interactiveMode
	 * 
	 * @param args
	 *            The command line arguments to be parsed
	 */
	private static void readCommandlineArguments(String[] args) {

		String complete_command_line_args = "";
		
		// iterate over the given arguments
		for (int i = 0; i < args.length; i++) {

			// convert the argument to lower case, i.e. command line switches
			// are not case sensitive for this application
			String arg = args[i].toLowerCase();
			complete_command_line_args += arg+" ";
			
			// command line switches may begin with --, - or /
			if (arg.startsWith("--") || arg.startsWith("-")
					|| arg.startsWith("/")) {

				String command = extractCommandLineSwitch(arg);

				if (command.equalsIgnoreCase("config") && (i < args.length - 1)) {
					configFileName = args[i + 1];
				}

				if (command.equalsIgnoreCase("setpass")) {
					interactiveMode = true;
				}

				if (command.equalsIgnoreCase("dispatcherinfo")) {
					log.debug("Found commandline option disaptcherinfo");
					checkDispatcherInformation = true;
				}

				if (command.equalsIgnoreCase("version") || command.equals("help")) {
					infoMode = true;
				}

				if (command.equalsIgnoreCase("check")) {
					checkConfigMode = true;
				}

				if (command.equalsIgnoreCase("console")) {
					consoleLogging = true;
				}

				if (command.equalsIgnoreCase("phasebasic")) {
					phaseBasicConfiguration = true;
				}

				if (command.equalsIgnoreCase("advancedDispatcherSetting")) {
					phaseAdvancedDispatcherSetting = true;
				}

				if (command.equalsIgnoreCase("phasedeployment")
						|| command.equals("phasedeploy")) {
					phaseDeployment = true;
				}

				if (command.equalsIgnoreCase("phasecontent")) {
					phaseContentConfiguration = true;
				}
				
				if (command.equalsIgnoreCase("donotaskforbackup")) {
					phaseBackupDonotAsk = true;
				}
				
				if (command.equalsIgnoreCase("crypt")) {
					setCrypt(args[i+1]);
				}
				
				if (command.equalsIgnoreCase("dumpaccounts")) {
					setDumpAccounts(true);
				}
				
				if (command.equalsIgnoreCase("mailto")) {
					COCOMA_MAILTO_HELPTEXT = args[i+1];
				}
			}
		}
		log.info("Command line parameters: "+complete_command_line_args);

		// if none of the phase switches is set, do a complete run
		if (!phaseBasicConfiguration 
				&& !phaseDeployment
				&& !phaseContentConfiguration 
				&& crypt.length()==0 
				&& !infoMode) {
			phaseBasicConfiguration = true;
			phaseDeployment = true;
			phaseContentConfiguration = true;

			log.debug("No phase information found on commandline.");
			log.debug("Will perform defaultphase options: Basic+Deployment+Contentconfiguration");
			log.debug("If this is not a desired behaviour, just set the phase as switch on the commandline.");

		}

	}

	/**
	 * Show a prompt on STDOUT and read the user's input from STDIN. The Input
	 * is compared with the given confirmation string, if the strings match,
	 * TRUE is returned, false if the strings dont match.
	 * 
	 * @param prompt
	 *            The Prompt to be shown.
	 * @param confirmation
	 *            The answer that is expected as confirmation.
	 * 
	 * @return TRUE if the prompt was confirmed, FALSE if not
	 */
	private boolean showDeploymentConfirmationPrompt(String prompt,
			String confirmation) {

		boolean confirmed = false;

		try {
			System.out.print(prompt);
			BufferedReader stdin = new BufferedReader(new InputStreamReader(
					System.in));
			String answer = stdin.readLine();
			confirmed = confirmation.trim().equals(answer.trim());
		} catch (IOException e) {
			log.error("Error reading answer from STDIN.");
			confirmed = false;
		}

		return confirmed;
	}

	/**
	 * This method prints the product information on STDOUT. This information
	 * contains the product name and version as well as a short overview over
	 * the recognized command line arguments.
	 */
	private static void showProductInfo() {

		String lf = System.getProperty("line.separator");
		String infoString = getVersionString() + lf + lf + getHelpString() + lf;

		System.out.println(infoString);

	}

	/**
	 * Set the static error code. If the given code is > 0 and the currently
	 * saved static error code still is 0, the given code is applied. Otherwise
	 * the previously set code remains. The static error code is used to define
	 * the exit code the application shall return when ending.
	 * 
	 * @param code
	 *            The error code to be used.
	 */
	public static void setErrorCode(int code, String message) {
		if ((CoCoMa.errorCode == 0) && (code > 0)) {
			CoCoMa.errorCode = code;
		}
		errorLog.add("Error Code " + code + ": " + message);
	}

	/**
	 * Getter to check if CoCoMa is running in interactive mode to set the
	 * passwords.
	 * 
	 * @return TRUE if CoCoMa is in interactive mode, FALSE if in standard oder
	 *         other mode
	 */
	public static boolean isInteractiveMode() {
		return CoCoMa.interactiveMode;
	}

	/**
	 * @param crypt the crypt to set
	 */
	public static void setCrypt(String crypt) {
		CoCoMa.crypt = crypt;
	}

	/**
	 * @return the crypt
	 */
	public static String getCrypt() {
		return crypt;
	}

	/**
	 * @param dumpAccounts the dumpAccounts to set
	 */
	public static void setDumpAccounts(boolean dumpAccounts) {
		CoCoMa.dumpAccounts = dumpAccounts;
	}

	/**
	 * @return the dumpAccounts
	 */
	public static boolean isDumpAccounts() {
		return dumpAccounts;
	}
}
