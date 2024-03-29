/**
 * $Id: CoCoMaConfiguration.java 163 2010-10-12 09:16:13Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.console.PwdConsole;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 * Convenience class to provide access to the configuration file. It offers
 * methods to read and access the respective configuration blocks, which may
 * consist of various different configuration values.
 *
 * @author riedchr
 * @author Last change by $Author: Ralf Roeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 163 $ ($Date:: 2010-10-12 11:16:13 +0200#$)
 */
public class CoCoMaConfiguration {

	private Logger log;

	private ServerData serverData;

	private MailserverData mailserverData;

	private BackupData backupData;

	private List<DataSourceData> dataSources;

	private List<DispatcherData> dispatchers;

	public XMLConfiguration conf;

	private SecurityData securityData;

	private List<CapabilityData> capabilities;

	private String version;

	private List<DeploymentData> deployments;

	private List<Properties> reportVersions;

	private UIData uiData;

	private RestrictedContentData restrictedContentData;

	// DEFAULT Log4J Loglevel if not set in CONFIG.XML
	private String log4jloglevel;

	/**
	 * Read the given configuration file and prepare the data objects encapsulating
	 * the configuration data.
	 *
	 * @param configFile The configuration file to be read.
	 */
	@SuppressWarnings("unchecked")
	public void read(File configFile) throws ConfigurationException, ConfigException {

		this.log = Logging.getInstance().getLog(this.getClass());

		this.conf = new XMLConfiguration(configFile);

		this.serverData = new ServerData(conf);
		this.mailserverData = new MailserverData(conf);
		this.backupData = new BackupData(conf);

		this.dataSources = new ArrayList<>();
		this.dispatchers = new ArrayList<>();
		this.capabilities = new ArrayList<>();

		this.deployments = new ArrayList<>();

		this.reportVersions = new ArrayList<>();

		this.version = conf.getString("version");

		// look for log4j loglevel configuration first !
		this.setLog4jloglevel(conf.getString("log4jloglevel"));
		this.log.debug("log4j-level:" + getLog4jloglevel());
		if (getLog4jloglevel() != null && getLog4jloglevel().length() > 0) {
			this.log.debug("Setting loglevel from XML config-file");
//			Logger.getRootLogger().setLevel(Level.toLevel(getLog4jloglevel()));
			Logging.getInstance().setLogLevel(Level.toLevel(getLog4jloglevel()));
		}

		// read the data sources defined in the configuration
		log.debug("Reading Datasource Config");
		List<Object> dataSourceDataList = conf.getList(DataSourceData.RECORD_KEY_PREFIX + ".name");
		for (int i = 0; i < dataSourceDataList.size(); i++) {
			DataSourceData dataSourceData = new DataSourceData(conf, i);
			this.dataSources.add(dataSourceData);
		}

		// read the dispatcher parameters defined in the configuration
		log.debug("Reading Dispatcher Config");
		List<Object> dispatcherDataList = conf.getList(DispatcherData.RECORD_KEY_PREFIX + ".name");
		boolean globalConfigured = false;
		for (int i = 0; i < dispatcherDataList.size(); i++) {
			DispatcherData dispatcherData = new DispatcherData(conf, i);
			if (dispatcherData.isGlobaleDispatcher()) {
				globalConfigured = true;
			}
			this.dispatchers.add(dispatcherData);
		}

		if (globalConfigured && (this.dispatchers.size() > 1)) {
			throw new ConfigException("At least one of the configured dispatchers is set as "
					+ "global dispatcher but more than one dispatcher "
					+ "is configured. When using the global flag for a "
					+ "dispatcher there must be exactly one dispatcher " + "configured.");
		}

		// read and prepare config data about Cognos roles, groups and users
		this.securityData = new SecurityData(conf);

		// read the capabilities defined defined in the configuration
		log.debug("Reading Capabilites Config");
		String[] capabilityNames = conf.getStringArray(CapabilityData.RECORD_KEY_PREFIX + ".name");
		for (int i = 0; i < capabilityNames.length; i++) {
			String capabilityName = capabilityNames[i];
			String configKey = CapabilityData.RECORD_KEY_PREFIX + "(" + i + ")";
			CapabilityData capabilityData = new CapabilityData(capabilityName);
			capabilityData.readConfig(conf, configKey);
			this.capabilities.add(capabilityData);
		}

		// read the data for content visibility definitions defined in the
		// configuration
		log.debug("Reading Restricted Contentdata");
		this.restrictedContentData = new RestrictedContentData();
		restrictedContentData.readConfig(conf);

		// read and prepare deployments to be performed by CoCoMa
		log.debug("Reading Deployments Config");
		String[] deploymentNames = conf.getStringArray(DeploymentData.RECORD_KEY_PREFIX + ".name");
		for (int i = 0; i < deploymentNames.length; i++) {
			String deploymentName = deploymentNames[i];
			String configKey = DeploymentData.RECORD_KEY_PREFIX + "(" + i + ")";
			DeploymentData deploymentData = new DeploymentData(deploymentName);
			log.debug("Found Deployment Config with name: " + deploymentName);
			deploymentData.readConfig(conf, configKey);
			this.deployments.add(deploymentData);
		}

		// read and prepare report versions data to remove from cognos
		if (conf.getStringArray("reportVersions.searchPath").length > 0) {
			log.debug("Reading Report Versions Config");
			// getting reportVersions element/node
			Node reportVersions = conf.configurationAt("reportVersions").getRoot();

			// get dry run property
			boolean dryRun = true;
			try {
				dryRun = conf.getBoolean("reportVersions[@dryRun]", true);
			} catch (ConversionException err) {
				log.error("Got value that is not boolean, will set dryRun to default value true");
			}
			// loop over nodes found inside reportVersions element
			for (Node element : (List<Node>) reportVersions.getChildren()) {
				// save all searchPath properties
				Properties reportVersion = new Properties();

				// add dryRun property
				reportVersion.setProperty("dryRun", String.valueOf(dryRun));

				reportVersion.setProperty(element.getName(), (String) element.getValue());
				// loop over all attributes and add them to properties as well
				for (Node attribute : (List<Node>) element.getAttributes()) {
					reportVersion.setProperty(attribute.getName(), (String) attribute.getValue());
				}
				// add reportVersion to reportVersions
				this.reportVersions.add(reportVersion);
			}

			// set REPORT_VERSIONS for CoCoMa for it to execute
			CoCoMa.setREPORT_VERSIONS(true);
		}

		// read and prepare ui configuartion
		this.uiData = new UIData(conf);

	}

	/**
	 * Getter for the serverData field encapsulating the information about the
	 * server to connect to, user name etc.
	 *
	 * @return Current instance of {@link ServerData}
	 */
	public ServerData getServerData() {
		return serverData;
	}

	/**
	 * Getter for the mailserverData field
	 *
	 * @return the currently set value for mailserverData
	 */
	public MailserverData getMailserverData() {
		return mailserverData;
	}

	/**
	 * Getter for the list of {@link DataSourceData} instances defined in the
	 * configuration.
	 *
	 * @return The currently set list of {@link DataSourceData} instances. May be an
	 *         empty list.
	 */
	public List<DataSourceData> getDataSources() {
		return this.dataSources;
	}

	/**
	 * Getter for the list of {@link DispatcherData} instances defined in the
	 * configuration.
	 *
	 * @return The currently set list of {@link DispatcherData} instances. May be an
	 *         empty list.
	 */
	public List<DispatcherData> getDispatchers() {
		return this.dispatchers;
	}

	/**
	 * Method to prompt for the password needed to access the server. This method
	 * starts an interactive prompt via STDIN.
	 */
	public void setServerPassword() throws ConfigException {
		boolean passwordSet = (this.serverData.getPassword().length() > 0);
		if (passwordSet) {
			String passAlreadySetMsg = "A password for server " + this.serverData.getDispatcherURL()
					+ " using namespace " + this.serverData.getNameSpace() + " with user "
					+ this.serverData.getUserName() + " " + "has already been set. To reset the password, "
					+ "edit the configuration file and enter an empty "
					+ "string (<password></password>) as password value.";

			System.out.println(passAlreadySetMsg);
			log.info(passAlreadySetMsg);

		} else {
			String prompt = "Setting password for server " + this.serverData.getDispatcherURL() + " using namespace "
					+ this.serverData.getNameSpace() + " with user " + this.serverData.getUserName() + ".";
			String password = promptForPassword(prompt);
			if (password != null) {
				this.serverData.setPassword(password);
			} else {
				throw new ConfigException("The passwords do not match.");
			}
		}
	}

	/**
	 * Method to prompt for the password needed to access the mailserver. This
	 * method starts an interactive prompt via STDIN.
	 */
	public void setMailserverPassword() throws ConfigException {

		try {
			if (!this.mailserverData.getHost().isEmpty()) {
				log.debug("found Mailserver Entry:" + this.mailserverData.getHost());
			}
		} catch (Exception e1) {
			log.debug("No MailServer Host entry found ... so no pw to set!");
			return;
		}

		boolean passwordSet = false;

		try {
			passwordSet = (this.mailserverData.getPassword().length() > 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			passwordSet = false;
		}

		if (passwordSet) {
			String passAlreadySetMsg = "A password for mailserver " + this.mailserverData.getHost() + " with user "
					+ this.mailserverData.getUsername() + " " + "has already been set. To reset the password, "
					+ "edit the configuration file and enter an empty "
					+ "string (<password></password>) as password value.";

			System.out.println(passAlreadySetMsg);
			log.info(passAlreadySetMsg);

		} else {
			String prompt = "Setting password for mailserver " + this.mailserverData.getHost() + " with user "
					+ this.mailserverData.getUsername() + ".";
			String password = promptForPassword(prompt);
			if (password != null) {
				this.mailserverData.setPassword(password);
			} else {
				throw new ConfigException("The passwords do not match.");
			}
		}
	}

	/**
	 * Method to prompt for the password needed to access the content store backups.
	 * This method starts an interactive prompt via STDIN.
	 */
	public void setBackupPassword() throws ConfigException {

		// Check for Config-Item
		if (this.backupData.isBackup_configured()) {
			log.debug("Backup is configured:" + this.backupData.getName());
		} else {
			log.debug("No Backup configured ... setting a pw would be useless. Skipping");
			return;
		}

		boolean passwordSet;
		try {
			passwordSet = (this.backupData.getPassword().length() > 0);
		} catch (Exception e) {
			passwordSet = false;
		}
		if (passwordSet) {
			String passAlreadySetMsg = "A password for content store backups "
					+ "has already been set. To reset the password, "
					+ "edit the configuration file and enter an empty "
					+ "string (<password></password>) as password value.";

			System.out.println(passAlreadySetMsg);
			log.info(passAlreadySetMsg);

		} else {
			String prompt = "Setting password for content store backups.";
			String password = promptForPassword(prompt);
			if (password != null) {
				this.backupData.setPassword(password);
			} else {
				throw new ConfigException("The passwords do not match.");
			}
		}
	}

	/**
	 * Method to prompt for the password needed to access the content store backups.
	 * This method starts an interactive prompt via STDIN.
	 */
	public void setDeploymentPasswords() throws ConfigException {
		for (DeploymentData deployment : this.deployments) {
			boolean passwordSet = (deployment.getPassword().length() > 0);
			if (passwordSet) {
				String passAlreadySetMsg = "A password for the deployment " + deployment.getName()
						+ "has already been set. To reset the password, "
						+ "edit the configuration file and enter an empty "
						+ "string (<password></password>) as password value.";

				System.out.println(passAlreadySetMsg);
				log.info(passAlreadySetMsg);

			} else {
				String prompt = "Setting password for the deployment " + deployment.getName() + ".";
				String password = promptForPassword(prompt);
				if (password != null) {
					deployment.setPassword(password);
				} else {
					throw new ConfigException("The passwords do not match.");
				}
			}
		}

	}

	/**
	 * Method to prompt for the passwords needed to create the signons for the
	 * currently set data sources. This method starts an interactive prompt via
	 * STDIN for each data source defined in the configuration.
	 */
	public void setDataSourcesPasswords() throws ConfigException {

		for (DataSourceData ds : this.dataSources) {
			boolean passwordSet = (ds.getPassword().length() > 0);

			if (passwordSet) {
				String passAlreadySetMsg = "A password for DataSource " + ds.getName() + " using DB alias "
						+ ds.getDBAlias() + " with user " + ds.getUserName() + " "
						+ "has already been set. To reset the password, "
						+ "edit the configuration file and enter an empty "
						+ "string (<password></password>) as password value.";

				System.out.println(passAlreadySetMsg);
				log.info(passAlreadySetMsg);

			} else {
				String prompt = "Setting password for DataSource " + ds.getName() + " using DB alias " + ds.getDBAlias()
						+ " with user " + ds.getUserName() + ".";
				String password = promptForPassword(prompt);

				if (password != null) {
					ds.setPassword(password);
				} else {
					throw new ConfigException("The passwords do not match.");
				}
			}
		}
	}

	/**
	 * Convenience method to actually issue a password prompt. It takes an
	 * introductory string used as prompt and the reads the password.
	 *
	 * @param intro String to be used as prompt for this password prompt.
	 *
	 * @return The newly entered password
	 *
	 * @throws ConfigException This exception is thrown whenever there is a problem
	 *                         with the password prompt or when the passwords do not
	 *                         match.
	 */
	private static String promptForPassword(String intro) throws ConfigException {

		String pwd = PwdConsole.unmaskedPasswordPrompt(intro, "Password:", "Retype password:");

		if (pwd == null) {
			throw new ConfigException("The passwords do not match");
		}

		return pwd;
	}

	/**
	 * Getter for the {@link SecurityData} field.
	 *
	 * @return The currently set {@link SecurityData} instance.
	 */
	public SecurityData getSecurityData() {
		return this.securityData;
	}

	/**
	 * Getter for the list of {@link CapabilityData} instances encapsulating the
	 * capabilities that shall be modified by CoCoMa
	 *
	 * @return The currently set list of {@link CapabilityData} instances. May be an
	 *         empty list.
	 */
	public List<CapabilityData> getCapabilities() {
		return this.capabilities;
	}

	/**
	 * Getter for the version string contained in the configuration file.
	 *
	 * @return The currently set version string.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Getter for the instance of {@link UIData} encapsulating the settings for the
	 * user interface to be applied by CoCoMa.
	 *
	 * @return The currently set instance of {@link UIData}
	 */
	public UIData getUIData() {
		return this.uiData;
	}

	/**
	 * Getter for the list of deployments described by {@link DeploymentData}
	 * objects that are configured to be executed by CoCoMa.
	 *
	 * @return The currently set list of {@link DeploymentData} instances..
	 */
	public List<DeploymentData> getDeployments() {
		return this.deployments;
	}

	public List<Properties> getReportVersions() {
		return reportVersions;
	}

	/**
	 * @return
	 */
	public RestrictedContentData getRestrictedContentData() {
		return this.restrictedContentData;
	}

	/**
	 * Getter for the backupData field
	 *
	 * @return the currently set value for backupData
	 */
	public BackupData getBackupData() {
		return backupData;
	}

	/**
	 * @param log4jloglevel the log4jloglevel to set
	 */
	public void setLog4jloglevel(String log4jloglevel) {
		this.log4jloglevel = log4jloglevel;
	}

	/**
	 * @return the log4jloglevel
	 */
	public String getLog4jloglevel() {
		if (log4jloglevel == null) {
			log4jloglevel = "";
		}
		return log4jloglevel;
	}

}
