/**
 * $Id: DispatcherData.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AuditLevelEnum;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 16, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class DispatcherData {

	protected static final String RECORD_KEY_PREFIX = "dispatchers.dispatcher";

	private boolean globalDispatcher;

	private XMLConfiguration conf;

	private String recordKey;

	private String dispatcherName;
	private String loadBalancingMode;

	private String governorLimit;

	private String peakStartHour;
	private String peakEndHour;

	private String connectionsHighAffinity;
	private String connectionsLowAffinity;

	private String maxProcessesInteractiveReporting;
	private String maxProcessesInteractiveBatch;

	private String queueTimeoutReporting;

	private String[][] advancedDispatcherSettings;

	private String cmsAuditLevel;

	private String rdsAuditLevel;

	private String emsAuditLevel;

	private String jsAuditLevel;

	private String msAuditLevel;

	private String psAuditLevel;

	private String rsAuditLevel;

	private String ssAuditLevel;

	private String dispatcherAuditLevel;

	private boolean rsNativeQueryAudit;

	private Logger log;

	/**
	 * @param conf
	 * @param i
	 */
	public DispatcherData(XMLConfiguration config, int i) throws ConfigException {

		//
		// Prepare Logger
		//
		Logging logging = Logging.getInstance();
		logging.setConsoleLogging(true);
		log = logging.getLog(DispatcherData.class);

		//
		// Read Config
		//
		this.conf = config;

		this.recordKey = RECORD_KEY_PREFIX + "(" + i + ")";
		log.debug("recordKey:" + this.recordKey);

		this.globalDispatcher = conf.getBoolean(RECORD_KEY_PREFIX + "[@global]", false);

		this.dispatcherName = conf.getString(recordKey + ".name");
		this.loadBalancingMode = conf.getString(recordKey + ".loadBalancingMode");
		this.governorLimit = conf.getString(recordKey + ".governorLimit");
		this.peakStartHour = conf.getString(recordKey + ".peakStartHour");
		this.peakEndHour = conf.getString(recordKey + ".peakEndHour");
		this.connectionsHighAffinity = conf.getString(recordKey + ".connectionsHighAffinity");
		this.connectionsLowAffinity = conf.getString(recordKey + ".connectionsLowAffinity");
		this.maxProcessesInteractiveReporting = conf.getString(recordKey + ".maxProcessesInteractiveReporting");
		this.maxProcessesInteractiveBatch = conf.getString(recordKey + ".maxProcessesInteractiveBatch");
		this.queueTimeoutReporting = conf.getString(recordKey + ".queueTimeoutReporting");

		//
		// Read Advanced Parameters from XML_Config <advancedSettings />
		//
		String tempkey = recordKey + ".advancedSettings";
		@SuppressWarnings("unchecked")
		List<Object> advancedParameters_list = conf.getList(tempkey + ".name");
		@SuppressWarnings("unchecked")
		List<Object> advancedParameters_listvalues = conf.getList(tempkey + ".value");

		String[][] advancedParameters_array = new String[advancedParameters_list.size()][2];
		this.advancedDispatcherSettings = new String[advancedParameters_list.size()][2];

		log.debug("Looking for " + tempkey);
		log.debug("Size:" + advancedParameters_list.size());
		for (int i_advSettingslist = 0; i_advSettingslist < advancedParameters_list.size(); i_advSettingslist++) {

			log.debug("KEY  :" + advancedParameters_list.get(i_advSettingslist));
			log.debug("VALUE:" + advancedParameters_listvalues.get(i_advSettingslist));

			// build key
			String tempkey_int = tempkey + "(" + i_advSettingslist + ")";
			log.debug("key: " + tempkey_int);
			// parameter name
			String adv_name = (String) advancedParameters_list.get(i_advSettingslist); // conf.getString(tempkey_int+".name");
			log.debug("name: " + adv_name);
			advancedParameters_array[i_advSettingslist][0] = adv_name;
			this.advancedDispatcherSettings[i_advSettingslist][0] = adv_name;

			// value
			String adv_value = (String) advancedParameters_listvalues.get(i_advSettingslist); // conf.getString(tempkey_int+".value");
			log.debug("value: " + adv_value);
			advancedParameters_array[i_advSettingslist][1] = adv_value;
			this.advancedDispatcherSettings[i_advSettingslist][1] = adv_value;
		}
		log.debug(advancedDispatcherSettings.length + " advancedParameters reading finished.");

		// audit levels
		this.cmsAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.contentManagerService"));
		this.rdsAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.reportDataService"));
		this.emsAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.eventManagementService"));
		this.jsAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.jobService"));
		this.msAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.monitorService"));
		this.psAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.presentationService"));
		this.rsAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.reportService"));
		this.rsNativeQueryAudit = conf.getBoolean(recordKey + ".auditLevel.reportServiceNativeQuery", true);
		this.ssAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.systemService"));
		this.dispatcherAuditLevel = parseAuditLevel(conf.getString(recordKey + ".auditLevel.dispatcher"));

		if (this.dispatcherName.length() == 0) {
			throw new ConfigException("Dispatcher (" + i + ") name is empty.");
		}

		if (this.loadBalancingMode.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") loadBalancingMode is empty.");
		} else if (!this.loadBalancingMode.equals("weightedRoundRobin")
				&& !this.loadBalancingMode.equals("clusterCompatible")) {
			throw new ConfigException(
					"Dispatcher loadBalancingMode may only be one of the following values: 'weightedRoundRobin' or 'clusterCompatible'");
		}

		if (this.governorLimit.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") governerLimit is empty.");
		}

		if (this.peakStartHour.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") peakStartHour is empty.");
		}

		if (this.peakEndHour.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") peakEndHour is empty.");
		}

		if (this.connectionsHighAffinity.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") connectionsHighAffinity is empty.");
		}

		if (this.connectionsLowAffinity.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") connectionsLowAffinity is empty.");
		}

		if (this.maxProcessesInteractiveReporting.length() == 0) {
			throw new ConfigException(
					"Dispatcher (" + this.dispatcherName + ") maxProcessesInteractiveReporting is empty.");
		}

		if (this.maxProcessesInteractiveBatch == null) {
			log.debug("maxProcessesInteractiveBatch is not set... will not update it.");
		} else if (this.maxProcessesInteractiveBatch.length() == 0) {
			throw new ConfigException(
					"Dispatcher (" + this.dispatcherName + ") maxProcessesInteractiveBatch is empty.");
		}

		if (this.queueTimeoutReporting.length() == 0) {
			throw new ConfigException("Dispatcher (" + this.dispatcherName + ") queueTimeoutReporting is empty.");
		}
	}

	/**
	 * @return
	 */
	public String getDispatcherName() {
		return this.dispatcherName;
	}

	/**
	 * @return
	 */
	public String getLoadBalancingMode() {
		return this.loadBalancingMode;
	}

	/**
	 * @return
	 */
	public String getPeakStartHour() {
		return this.peakStartHour;
	}

	/**
	 * @return
	 */
	public String getPeakEndHour() {
		return this.peakEndHour;
	}

	/**
	 * @return
	 */
	public String getConnectionsHighAffinity() {

		return this.connectionsHighAffinity;
	}

	/**
	 * @return
	 */
	public String getConnectionsLowAffinity() {
		return this.connectionsLowAffinity;
	}

	/**
	 * @return
	 */
	public String getMaxProcessesInteractiveReporting() {
		return this.maxProcessesInteractiveReporting;
	}

	/**
	 * @return
	 */
	public String getMaxProcessesInteractiveBatch() {
		return this.maxProcessesInteractiveBatch;
	}

	/**
	 * @return
	 */
	public String getQueueTimeoutReporting() {
		return this.queueTimeoutReporting;
	}

	/**
	 * @return
	 */
	public String getGovernorLimit() {
		return this.governorLimit;
	}

	/**
	 * @return
	 */
	public String getCmsAuditLevel() {
		return this.cmsAuditLevel;
	}

	/**
	 * @param auditLevel
	 * @return
	 */
	private String parseAuditLevel(String auditLevel) throws ConfigException {

		String level = "";

		if ((auditLevel == null) || (auditLevel.length() == 0)) {
			throw new ConfigException("Audit level could not be parsed, is null or empty");
		} else if (auditLevel.equals("MINIMAL")) {
			level = AuditLevelEnum._minimal;
		} else if (auditLevel.equals("BASIC")) {
			level = AuditLevelEnum._basic;
		} else if (auditLevel.equals("REQUEST")) {
			level = AuditLevelEnum._request;
		} else if (auditLevel.equals("FULL")) {
			level = AuditLevelEnum._full;
		} else if (auditLevel.equals("TRACE")) {
			level = AuditLevelEnum._trace;
		} else {
			throw new ConfigException("Invalid value for audit level: " + auditLevel
					+ ". Can only be one of BASIC, FULL, MINIMAL, REQUEST, TRACE.");
		}

		return level;
	}

	/**
	 * @return
	 */
	public String getEmsAuditLevel() {
		return this.emsAuditLevel;
	}

	/**
	 * @return
	 */
	public String getRdsAuditLevel() {
		return this.rdsAuditLevel;
	}

	/**
	 * @return
	 */
	public String getDispatcherAuditLevel() {
		return this.dispatcherAuditLevel;
	}

	/**
	 * @return
	 */
	public String getSsAuditLevel() {
		return this.ssAuditLevel;
	}

	/**
	 * @return
	 */
	public String getRsAuditLevel() {
		return this.rsAuditLevel;
	}

	/**
	 * @return
	 */
	public String getPsAuditLevel() {
		return this.psAuditLevel;
	}

	/**
	 * @return
	 */
	public String getJsAuditLevel() {
		return this.jsAuditLevel;
	}

	/**
	 * @return
	 */
	public String getMsAuditLevel() {
		return this.msAuditLevel;
	}

	/**
	 * @return
	 */
	public boolean isRsNativeQueryAudit() {
		return this.rsNativeQueryAudit;
	}

	/**
	 * @return
	 */
	public boolean isGlobaleDispatcher() {
		return this.globalDispatcher;
	}

	/**
	 * @param advancedParameters the advancedParameters to set
	 */
	public void setAdvancedParameters(String[][] advancedParameters) {
		this.advancedDispatcherSettings = advancedParameters;
	}

	/**
	 * @return the advancedParameters
	 */
	public String[][] getAdvancedParameters() {
		return this.advancedDispatcherSettings;
	}

}
