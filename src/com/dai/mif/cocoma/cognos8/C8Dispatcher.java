/**
 * $Id: C8Dispatcher.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.math.BigInteger;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.AuditLevelEnum;
import com.cognos.developer.schemas.bibus._3.AuditLevelEnumProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BooleanProp;
import com.cognos.developer.schemas.bibus._3.Configuration;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_Type;
import com.cognos.developer.schemas.bibus._3.Dispatcher_Type;
import com.cognos.developer.schemas.bibus._3.IntProp;
import com.cognos.developer.schemas.bibus._3.LoadBalancingModeEnum;
import com.cognos.developer.schemas.bibus._3.LoadBalancingModeEnumProp;
import com.cognos.developer.schemas.bibus._3.PositiveIntegerProp;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.config.DispatcherData;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 16, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class C8Dispatcher {

	private Logger log;

	private DispatcherData dispatcherData;
	private C8Access c8Access;

	/**
	 * @param dispatcherData
	 * @param c8Access
	 */
	public C8Dispatcher(DispatcherData dispatcherData, C8Access c8Access) {
		this.log = Logging.getInstance().getLog(this.getClass());

		this.dispatcherData = dispatcherData;
		this.c8Access = c8Access;
	}

	/**
	 * setAdvancedDispatcherSettings() throws RemoteException
	 */
	public void setAdvancedDispatcherSettings() throws RemoteException {

		ContentManagerService_PortType cmService = this.c8Access.getCmService();
		String searchPath = "/configuration";

		PropEnum props[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.advancedSettings };

		SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
		spMulti.set_value(searchPath);

		// build Adv.Dispatcher Configuration with values from Config-File
		String setting = "<settings>";
		String[][] advancedDispatcherSettings = this.dispatcherData.getAdvancedParameters();
		log.debug("AdvancedDispatcherSettings:" + advancedDispatcherSettings);
		for (int i = 0; i < advancedDispatcherSettings.length; i++) {
			log.info("Name : " + advancedDispatcherSettings[i][0]); // do
																	// something
			log.info("Value: " + advancedDispatcherSettings[i][1]); // do
																	// something
			setting += "<setting name=\"" + advancedDispatcherSettings[i][0] + "\">" + advancedDispatcherSettings[i][1]
					+ "</setting>";
		}
		setting += "</settings>";
		log.debug("Setting: " + setting);
		log.debug("Getting baseClass from: " + searchPath);

		BaseClass bc[] = cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());

		Configuration cm = (Configuration) bc[0];

		AnyTypeProp atp = new AnyTypeProp();
		atp.setValue(setting);
		cm.setAdvancedSettings(atp);
		cmService.update(new BaseClass[] { cm }, new UpdateOptions());
		log.info("AdvancedSettings have been set to: " + setting);
	}

	/**
	 *
	 */
	public void updateDispatcherParameters() throws CoCoMaC8Exception {

		log.info("Configuring dispatcher " + dispatcherData.getDispatcherName());

		try {

			String queryPath;

			if (this.dispatcherData.isGlobaleDispatcher()) {
				queryPath = "/configuration";
			} else {
				queryPath = "/configuration//dispatcher[@defaultName=\"" + this.dispatcherData.getDispatcherName()
						+ "\"]";
			}

			ContentManagerService_PortType cmService = this.c8Access.getCmService();

			PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath };
			Sort[] sort = new Sort[] {};
			QueryOptions queryOptions = new QueryOptions();

			BaseClass[] dispatchers = cmService.query(new SearchPathMultipleObject(queryPath), props, sort,
					queryOptions);

			if (dispatchers.length == 0) {
				throw new CoCoMaC8Exception(
						"No dispatcher found with the name " + this.dispatcherData.getDispatcherName());

			} else if (dispatchers.length > 1) {
				throw new CoCoMaC8Exception("Dispatcher name " + this.dispatcherData.getDispatcherName()
						+ " is ambiguous. Returned " + dispatchers.length + " results.");
			} else {

				if (this.dispatcherData.isGlobaleDispatcher()) {
					Configuration disp = (Configuration) dispatchers[0];

					log.debug("Found global dispatcher at " + disp.getSearchPath().getValue());

					log.debug("Applying global dispatcher parameters");

					// set the balancing mode
					setLoadBalacingMode(disp, this.dispatcherData);

					// set the governor limit
					setGovernorLimit(disp, this.dispatcherData);

					// set the peak start and end time
					setPeakStartTime(disp, this.dispatcherData);
					setPeakEndTime(disp, this.dispatcherData);

					// set high and low affinity connections
					setHighAffintyConnections(disp, this.dispatcherData);
					setLowAffintyConnections(disp, this.dispatcherData);

					// set process limit for interactive reporting
					setInteractiveReportingProcessLimit(disp, this.dispatcherData);

					// set process limit for interactive batch reports
					setInteractiveBatchReportProcessLimit(disp, this.dispatcherData);

					// set reporting queue timeout
					setReportingQueueTimeout(disp, this.dispatcherData);

					// set custom config parameters for the
					// contentManagerService
					// XXX Todo setReportServiceCustomParameters(disp,
					// this.dispatcherData);

					// set custom config parameters for the
					// contentManagerService
					setCMServiceCustomParameters(disp, this.dispatcherData);

					// set audit levels
					setAuditLevels(disp, this.dispatcherData);

					log.debug("Updating dispatcher " + disp.getDefaultName().getValue() + " at "
							+ disp.getSearchPath().getValue());

					UpdateOptions updateOptions = new UpdateOptions();

					cmService.update(new BaseClass[] { disp }, updateOptions);

					log.debug("Configuration saved for dispatcher " + disp.getDefaultName().getValue());
				}

				else {

					Dispatcher_Type disp = (Dispatcher_Type) dispatchers[0];

					log.debug("Found dispatcher at " + disp.getSearchPath().getValue());

					log.debug("Applying dispatcher parameters");

					// set the balancing mode
					setLoadBalacingMode(disp, this.dispatcherData);

					// set the governor limit
					setGovernorLimit(disp, this.dispatcherData);

					// set the peak start and end time
					setPeakStartTime(disp, this.dispatcherData);
					setPeakEndTime(disp, this.dispatcherData);

					// set high and low affinity connections
					setHighAffintyConnections(disp, this.dispatcherData);
					setLowAffintyConnections(disp, this.dispatcherData);

					// set process limit for interactive reporting
					setInteractiveReportingProcessLimit(disp, this.dispatcherData);

					// set process limit for interactive batch reports
					setInteractiveBatchReportProcessLimit(disp, this.dispatcherData);

					// set reporting queue timeout
					setReportingQueueTimeout(disp, this.dispatcherData);

					// set custom config parameters for the
					// contentManagerService
					setCMServiceCustomParameters(disp, this.dispatcherData);

					// set audit levels
					setAuditLevels(disp, this.dispatcherData);

					log.debug("Updating dispatcher " + disp.getDefaultName().getValue() + " at "
							+ disp.getSearchPath().getValue());
					UpdateOptions updateOptions = new UpdateOptions();

					cmService.update(new BaseClass[] { disp }, updateOptions);

					log.debug("Configuration saved for dispatcher " + disp.getDefaultName().getValue());
				}
			}

		} catch (RemoteException e) {
			throw new CoCoMaC8Exception("Error accessing configuration for dispatcher "
					+ this.dispatcherData.getDispatcherName() + ". C8 Server returned: " + e.getMessage());
		}

	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setAuditLevels(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		log.debug("Setting audit levels");

		// --- content manager service

		AuditLevelEnum cmsAuditLevel = AuditLevelEnum.fromString(dispData.getCmsAuditLevel());
		AuditLevelEnumProp cmsAuditLevelProp = new AuditLevelEnumProp();
		cmsAuditLevelProp.setValue(cmsAuditLevel);

		log.debug("Audit level content manager service: " + dispData.getCmsAuditLevel());

		// --- report data service

		AuditLevelEnum rdsAuditLevel = AuditLevelEnum.fromString(dispData.getRdsAuditLevel());
		AuditLevelEnumProp rdsAuditLevelProp = new AuditLevelEnumProp();
		rdsAuditLevelProp.setValue(rdsAuditLevel);

		log.debug("Audit level report data service: " + dispData.getRdsAuditLevel());

		// --- event management service

		AuditLevelEnum emsAuditLevel = AuditLevelEnum.fromString(dispData.getEmsAuditLevel());
		AuditLevelEnumProp emsAuditLevelProp = new AuditLevelEnumProp();
		emsAuditLevelProp.setValue(emsAuditLevel);

		log.debug("Audit level event management service: " + dispData.getEmsAuditLevel());

		// --- job service

		AuditLevelEnum jsAuditLevel = AuditLevelEnum.fromString(dispData.getJsAuditLevel());
		AuditLevelEnumProp jsAuditLevelProp = new AuditLevelEnumProp();
		jsAuditLevelProp.setValue(jsAuditLevel);

		log.debug("Audit level job service: " + dispData.getJsAuditLevel());

		// --- monitor service

		AuditLevelEnum msAuditLevel = AuditLevelEnum.fromString(dispData.getMsAuditLevel());
		AuditLevelEnumProp msAuditLevelProp = new AuditLevelEnumProp();
		msAuditLevelProp.setValue(msAuditLevel);

		log.debug("Audit level monitor service: " + dispData.getMsAuditLevel());

		// --- presentation service

		AuditLevelEnum psAuditLevel = AuditLevelEnum.fromString(dispData.getPsAuditLevel());
		AuditLevelEnumProp psAuditLevelProp = new AuditLevelEnumProp();
		psAuditLevelProp.setValue(psAuditLevel);

		log.debug("Audit level presentation service: " + dispData.getPsAuditLevel());

		// --- report service

		AuditLevelEnum rsAuditLevel = AuditLevelEnum.fromString(dispData.getRsAuditLevel());
		AuditLevelEnumProp rsAuditLevelProp = new AuditLevelEnumProp();
		rsAuditLevelProp.setValue(rsAuditLevel);

		log.debug("Audit level report service: " + dispData.getRsAuditLevel());

		// --- System service

		AuditLevelEnum ssAuditLevel = AuditLevelEnum.fromString(dispData.getSsAuditLevel());
		AuditLevelEnumProp ssAuditLevelProp = new AuditLevelEnumProp();
		ssAuditLevelProp.setValue(ssAuditLevel);

		log.debug("Audit level system service: " + dispData.getSsAuditLevel());

		// --- dispatcher

		AuditLevelEnum dispatcherAuditLevel = AuditLevelEnum.fromString(dispData.getDispatcherAuditLevel());
		AuditLevelEnumProp dispatcherAuditLevelProp = new AuditLevelEnumProp();
		dispatcherAuditLevelProp.setValue(dispatcherAuditLevel);

		log.debug("Audit level dispatcher: " + dispData.getDispatcherAuditLevel());

		// --- native query auditing for report service
		BooleanProp rsNativeQueryAuditProp = new BooleanProp();
		rsNativeQueryAuditProp.setValue(dispData.isRsNativeQueryAudit());

		log.debug("Allow native query auditing for report service: " + dispData.isRsNativeQueryAudit());

		conf.setCmsAuditLevel(cmsAuditLevelProp);
		conf.setRdsAuditLevel(rdsAuditLevelProp);
		conf.setEmsAuditLevel(emsAuditLevelProp);
		conf.setJsAuditLevel(jsAuditLevelProp);
		conf.setMsAuditLevel(msAuditLevelProp);
		conf.setPsAuditLevel(psAuditLevelProp);
		conf.setRsAuditLevel(rsAuditLevelProp);
		conf.setSsAuditLevel(ssAuditLevelProp);
		conf.setDispatcherAuditLevel(dispatcherAuditLevelProp);
		conf.setRsAuditNativeQuery(rsNativeQueryAuditProp);
	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setAuditLevels(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		log.debug("Setting audit levels");

		// --- content manager service

		AuditLevelEnum cmsAuditLevel = AuditLevelEnum.fromString(dispData.getCmsAuditLevel());
		AuditLevelEnumProp cmsAuditLevelProp = new AuditLevelEnumProp();
		cmsAuditLevelProp.setValue(cmsAuditLevel);

		log.debug("Audit level content manager service: " + dispData.getCmsAuditLevel());

		// --- report data service

		AuditLevelEnum rdsAuditLevel = AuditLevelEnum.fromString(dispData.getRdsAuditLevel());
		AuditLevelEnumProp rdsAuditLevelProp = new AuditLevelEnumProp();
		rdsAuditLevelProp.setValue(rdsAuditLevel);

		log.debug("Audit level report data service: " + dispData.getRdsAuditLevel());

		// --- event management service

		AuditLevelEnum emsAuditLevel = AuditLevelEnum.fromString(dispData.getEmsAuditLevel());
		AuditLevelEnumProp emsAuditLevelProp = new AuditLevelEnumProp();
		emsAuditLevelProp.setValue(emsAuditLevel);

		log.debug("Audit level event management service: " + dispData.getEmsAuditLevel());

		// --- job service

		AuditLevelEnum jsAuditLevel = AuditLevelEnum.fromString(dispData.getJsAuditLevel());
		AuditLevelEnumProp jsAuditLevelProp = new AuditLevelEnumProp();
		jsAuditLevelProp.setValue(jsAuditLevel);

		log.debug("Audit level job service: " + dispData.getJsAuditLevel());

		// --- monitor service

		AuditLevelEnum msAuditLevel = AuditLevelEnum.fromString(dispData.getMsAuditLevel());
		AuditLevelEnumProp msAuditLevelProp = new AuditLevelEnumProp();
		msAuditLevelProp.setValue(msAuditLevel);

		log.debug("Audit level monitor service: " + dispData.getMsAuditLevel());

		// --- presentation service

		AuditLevelEnum psAuditLevel = AuditLevelEnum.fromString(dispData.getPsAuditLevel());
		AuditLevelEnumProp psAuditLevelProp = new AuditLevelEnumProp();
		psAuditLevelProp.setValue(psAuditLevel);

		log.debug("Audit level presentation service: " + dispData.getPsAuditLevel());

		// --- report service

		AuditLevelEnum rsAuditLevel = AuditLevelEnum.fromString(dispData.getRsAuditLevel());
		AuditLevelEnumProp rsAuditLevelProp = new AuditLevelEnumProp();
		rsAuditLevelProp.setValue(rsAuditLevel);

		log.debug("Audit level report service: " + dispData.getRsAuditLevel());

		// --- System service

		AuditLevelEnum ssAuditLevel = AuditLevelEnum.fromString(dispData.getSsAuditLevel());
		AuditLevelEnumProp ssAuditLevelProp = new AuditLevelEnumProp();
		ssAuditLevelProp.setValue(ssAuditLevel);

		log.debug("Audit level system service: " + dispData.getSsAuditLevel());

		// --- dispatcher

		AuditLevelEnum dispatcherAuditLevel = AuditLevelEnum.fromString(dispData.getDispatcherAuditLevel());
		AuditLevelEnumProp dispatcherAuditLevelProp = new AuditLevelEnumProp();
		dispatcherAuditLevelProp.setValue(dispatcherAuditLevel);

		log.debug("Audit level dispatcher: " + dispData.getDispatcherAuditLevel());

		// --- native query auditing for report service
		BooleanProp rsNativeQueryAuditProp = new BooleanProp();
		rsNativeQueryAuditProp.setValue(dispData.isRsNativeQueryAudit());

		log.debug("Allow native query auditing for report service: " + dispData.isRsNativeQueryAudit());

		disp.setCmsAuditLevel(cmsAuditLevelProp);
		disp.setRdsAuditLevel(rdsAuditLevelProp);
		disp.setEmsAuditLevel(emsAuditLevelProp);
		disp.setJsAuditLevel(jsAuditLevelProp);
		disp.setMsAuditLevel(msAuditLevelProp);
		disp.setPsAuditLevel(psAuditLevelProp);
		disp.setRsAuditLevel(rsAuditLevelProp);
		disp.setSsAuditLevel(ssAuditLevelProp);
		disp.setDispatcherAuditLevel(dispatcherAuditLevelProp);
		disp.setRsAuditNativeQuery(rsNativeQueryAuditProp);
	}

	/**
	 * @param conf
	 * @param dispData
	 */
	private void setCMServiceCustomParameters(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		log.debug("Setting advanced contentManagerService parameters");

		String cmServiceQuery = conf.getSearchPath().getValue() + "//contentManagerService";

		ContentManagerService_PortType cmService = this.c8Access.getCmService();

		PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		try {
			BaseClass[] cmServices = cmService.query(new SearchPathMultipleObject(cmServiceQuery), props, sort,
					queryOptions);

			if (cmServices.length == 0) {
				throw new CoCoMaC8Exception("No ContentManagerService found for the dispatcher named "
						+ this.dispatcherData.getDispatcherName());

			} else {

				for (BaseClass bcCMService : cmServices) {

					log.debug("ContentManagerService at " + bcCMService.getSearchPath().getValue());

					ContentManagerService_Type cms = (ContentManagerService_Type) bcCMService;

					AnyTypeProp customProp = new AnyTypeProp();

					// ATTENTION! This value is hard coded and not influenced by
					// the
					// config file

					String customPropertyXML = buildAdvancedPropertyXML("CM.DeploymentIncludeConfiguration", "true");

					log.debug("Advanced content manager service parameters: " + customPropertyXML);

					customProp.setValue(customPropertyXML);

					cms.setAdvancedSettings(customProp);

					cmService.update(new BaseClass[] { cms }, new UpdateOptions());

					log.debug("ContentManagerService updated");
				}

			}
		} catch (RemoteException e) {
			throw new CoCoMaC8Exception("Error accessing contentManagerService for dispatcher "
					+ this.dispatcherData.getDispatcherName() + ". C8 Server returned: " + e.getMessage());
		}

	}

	/**
	 * @param disp
	 * @param dispData
	 */
	private void setCMServiceCustomParameters(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		log.debug("Setting advanced contentManagerService parameters");

		String cmServiceQuery = disp.getSearchPath().getValue() + "/contentManagerService";

		ContentManagerService_PortType cmService = this.c8Access.getCmService();

		PropEnum[] props = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		try {
			BaseClass[] cmServices = cmService.query(new SearchPathMultipleObject(cmServiceQuery), props, sort,
					queryOptions);

			if (cmServices.length == 0) {
				throw new CoCoMaC8Exception("No ContentManagerService found for the dispatcher named "
						+ this.dispatcherData.getDispatcherName());

			} else if (cmServices.length > 1) {
				throw new CoCoMaC8Exception("Dispatcher named " + this.dispatcherData.getDispatcherName()
						+ " has more than one contentManagerService assigned. Returned " + cmServices.length
						+ " results.");
			} else {

				ContentManagerService_Type cms = (ContentManagerService_Type) cmServices[0];

				AnyTypeProp customProp = new AnyTypeProp();

				// ATTENTION! This value is hard coded and not influenced by the
				// config file

				String customPropertyXML = buildAdvancedPropertyXML("CM.DeploymentIncludeConfiguration", "true");

				log.debug("Advanced content manager service parameters: " + customPropertyXML);

				customProp.setValue(customPropertyXML);

				cms.setAdvancedSettings(customProp);

				cmService.update(new BaseClass[] { cms }, new UpdateOptions());

				log.debug("ContentManagerService updated");

			}
		} catch (RemoteException e) {
			throw new CoCoMaC8Exception("Error accessing contentManagerService for dispatcher "
					+ this.dispatcherData.getDispatcherName() + ". C8 Server returned: " + e.getMessage());
		}

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setReportingQueueTimeout(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		String queueTimeoutReportingString = dispData.getQueueTimeoutReporting();

		log.debug("Setting queue limit for reporting service to " + queueTimeoutReportingString);

		BigInteger queueTimeoutReporting;

		try {
			queueTimeoutReporting = new BigInteger(queueTimeoutReportingString);

		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Queue limit " + queueTimeoutReportingString
					+ " for reporting service cannot be converted to integer.");
		}

		PositiveIntegerProp queueTimeoutReportingProp = new PositiveIntegerProp();
		queueTimeoutReportingProp.setValue(queueTimeoutReporting);

		disp.setRsQueueLimit(queueTimeoutReportingProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setReportingQueueTimeout(Configuration disp, DispatcherData dispData) throws CoCoMaC8Exception {

		String queueTimeoutReportingString = dispData.getQueueTimeoutReporting();

		log.debug("Setting queue limit for reporting service to " + queueTimeoutReportingString);

		BigInteger queueTimeoutReporting;

		try {
			queueTimeoutReporting = new BigInteger(queueTimeoutReportingString);

		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Queue limit " + queueTimeoutReportingString
					+ " for reporting service cannot be converted to integer.");
		}

		PositiveIntegerProp queueTimeoutReportingProp = new PositiveIntegerProp();
		queueTimeoutReportingProp.setValue(queueTimeoutReporting);

		disp.setRsQueueLimit(queueTimeoutReportingProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setInteractiveReportingProcessLimit(Dispatcher_Type disp, DispatcherData dispData)
			throws CoCoMaC8Exception {
		int maxProcessesCount;

		String maxProcessesCountString = dispData.getMaxProcessesInteractiveReporting();

		log.debug("Setting maximum number of processes for report service to " + maxProcessesCountString);

		try {
			maxProcessesCount = Integer.parseInt(maxProcessesCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of high affinity connections " + maxProcessesCountString
					+ " cannot be converted to integer.");
		}

		IntProp maxProcessesCountProp = new IntProp();
		maxProcessesCountProp.setValue(maxProcessesCount);

		disp.setRsPeakMaximumProcesses(maxProcessesCountProp);
	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setInteractiveReportingProcessLimit(Configuration conf, DispatcherData dispData)
			throws CoCoMaC8Exception {
		int maxProcessesCount;

		String maxProcessesCountString = dispData.getMaxProcessesInteractiveReporting();

		log.debug("Setting maximum number of processes for report service to " + maxProcessesCountString);

		try {
			maxProcessesCount = Integer.parseInt(maxProcessesCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of high affinity connections " + maxProcessesCountString
					+ " cannot be converted to integer.");
		}

		IntProp maxProcessesCountProp = new IntProp();
		maxProcessesCountProp.setValue(maxProcessesCount);

		conf.setRsPeakMaximumProcesses(maxProcessesCountProp);
	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setInteractiveBatchReportProcessLimit(Dispatcher_Type disp, DispatcherData dispData)
			throws CoCoMaC8Exception {
		int maxProcessesCount;

		String maxProcessesCountString = dispData.getMaxProcessesInteractiveBatch();

		// if maxProcessesCountString is null do nothing
		if (maxProcessesCountString != null) {

			log.debug("Setting maximum number of processes for batch report service to " + maxProcessesCountString);

			try {
				maxProcessesCount = Integer.parseInt(maxProcessesCountString);
			} catch (NumberFormatException nfe) {
				throw new CoCoMaC8Exception("Number of high affinity connections " + maxProcessesCountString
						+ " cannot be converted to integer.");
			}

			IntProp maxProcessesCountProp = new IntProp();
			maxProcessesCountProp.setValue(maxProcessesCount);

			disp.setBrsPeakMaximumProcesses(maxProcessesCountProp);
		}
	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setInteractiveBatchReportProcessLimit(Configuration conf, DispatcherData dispData)
			throws CoCoMaC8Exception {
		int maxProcessesCount;

		String maxProcessesCountString = dispData.getMaxProcessesInteractiveBatch();

		// if maxProcessesCountString is null do nothing
		if (maxProcessesCountString != null) {

			log.debug("Setting maximum number of processes for batch report service to " + maxProcessesCountString);

			try {
				maxProcessesCount = Integer.parseInt(maxProcessesCountString);
			} catch (NumberFormatException nfe) {
				throw new CoCoMaC8Exception("Number of high affinity connections " + maxProcessesCountString
						+ " cannot be converted to integer.");
			}

			IntProp maxProcessesCountProp = new IntProp();
			maxProcessesCountProp.setValue(maxProcessesCount);

			conf.setBrsPeakMaximumProcesses(maxProcessesCountProp);
		}
	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setLowAffintyConnections(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {
		int lowAffinityCount;

		String lowAffinityCountString = dispData.getConnectionsLowAffinity();

		log.debug("Setting low affintity connection count for report service to " + lowAffinityCountString);

		try {
			lowAffinityCount = Integer.parseInt(lowAffinityCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of low affinity connections " + lowAffinityCountString
					+ " cannot be converted to integer.");
		}

		IntProp lowAffinityCountProp = new IntProp();
		lowAffinityCountProp.setValue(lowAffinityCount);

		disp.setBrsPeakMaximumProcesses(lowAffinityCountProp);
	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setLowAffintyConnections(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {
		int lowAffinityCount;

		String lowAffinityCountString = dispData.getConnectionsLowAffinity();

		log.debug("Setting low affintity connection count for report service to " + lowAffinityCountString);

		try {
			lowAffinityCount = Integer.parseInt(lowAffinityCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of low affinity connections " + lowAffinityCountString
					+ " cannot be converted to integer.");
		}

		IntProp lowAffinityCountProp = new IntProp();
		lowAffinityCountProp.setValue(lowAffinityCount);

		conf.setRsPeakNonAffineConnections(lowAffinityCountProp);
	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setHighAffintyConnections(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		int highAffinityCount;

		String highAffinityCountString = dispData.getConnectionsHighAffinity();

		log.debug("Setting high affintity connection count for report service to " + highAffinityCountString);

		try {
			highAffinityCount = Integer.parseInt(highAffinityCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of high affinity connections " + highAffinityCountString
					+ " cannot be converted to integer.");
		}

		IntProp highAffinityCountProp = new IntProp();
		highAffinityCountProp.setValue(highAffinityCount);

		disp.setRsPeakAffineConnections(highAffinityCountProp);
	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setHighAffintyConnections(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		int highAffinityCount;

		String highAffinityCountString = dispData.getConnectionsHighAffinity();

		log.debug("Setting high affintity connection count for report service to " + highAffinityCountString);

		try {
			highAffinityCount = Integer.parseInt(highAffinityCountString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Number of high affinity connections " + highAffinityCountString
					+ " cannot be converted to integer.");
		}

		IntProp highAffinityCountProp = new IntProp();
		highAffinityCountProp.setValue(highAffinityCount);

		conf.setRsPeakAffineConnections(highAffinityCountProp);
	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setPeakEndTime(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		int peakEnd;

		String peakEndString = dispData.getPeakEndHour();

		log.debug("Setting peak end time to " + peakEndString);

		try {
			peakEnd = Integer.parseInt(peakEndString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Peak end hour " + peakEndString + " cannot be converted to integer.");
		}

		IntProp peakEndProp = new IntProp();
		peakEndProp.setValue(peakEnd);

		disp.setNonPeakDemandBeginHour(peakEndProp);

	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setPeakEndTime(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		int peakEnd;

		String peakEndString = dispData.getPeakEndHour();

		log.debug("Setting peak end time to " + peakEndString);

		try {
			peakEnd = Integer.parseInt(peakEndString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Peak end hour " + peakEndString + " cannot be converted to integer.");
		}

		IntProp peakEndProp = new IntProp();
		peakEndProp.setValue(peakEnd);

		conf.setNonPeakDemandBeginHour(peakEndProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setPeakStartTime(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		int peakStart;

		String peakStartString = dispData.getPeakStartHour();

		log.debug("Setting peak start time to " + peakStartString);

		try {
			peakStart = Integer.parseInt(peakStartString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Peak start hour " + peakStartString + " cannot be converted to integer.");
		}

		IntProp peakStartProp = new IntProp();
		peakStartProp.setValue(peakStart);

		disp.setPeakDemandBeginHour(peakStartProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setPeakStartTime(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		int peakStart;

		String peakStartString = dispData.getPeakStartHour();

		log.debug("Setting peak start time to " + peakStartString);

		try {
			peakStart = Integer.parseInt(peakStartString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Peak start hour " + peakStartString + " cannot be converted to integer.");
		}

		IntProp peakStartProp = new IntProp();
		peakStartProp.setValue(peakStart);

		conf.setPeakDemandBeginHour(peakStartProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setGovernorLimit(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		int governorLimit;

		String governorLimitString = dispData.getGovernorLimit();

		log.debug("Setting governor limit to " + governorLimitString);

		try {
			governorLimit = Integer.parseInt(governorLimitString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Governor limit " + governorLimitString + " cannot be converted to integer.");
		}

		IntProp governorLimitProp = new IntProp();
		governorLimitProp.setValue(governorLimit);

		disp.setRdsMaximumDataSize(governorLimitProp);

	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setGovernorLimit(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		int governorLimit;

		String governorLimitString = dispData.getGovernorLimit();

		log.debug("Setting governor limit to " + governorLimitString);

		try {
			governorLimit = Integer.parseInt(governorLimitString);
		} catch (NumberFormatException nfe) {
			throw new CoCoMaC8Exception("Governor limit " + governorLimitString + " cannot be converted to integer.");
		}

		IntProp governorLimitProp = new IntProp();
		governorLimitProp.setValue(governorLimit);

		conf.setRdsMaximumDataSize(governorLimitProp);

	}

	/**
	 * @param disp
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setLoadBalacingMode(Dispatcher_Type disp, DispatcherData dispData) throws CoCoMaC8Exception {

		String lbMode = dispData.getLoadBalancingMode();

		log.debug("Setting loadBalancing mode to " + lbMode);

		LoadBalancingModeEnumProp lbModeProp = new LoadBalancingModeEnumProp();

		if (lbMode.equals(LoadBalancingModeEnum._weightedRoundRobin)) {
			lbModeProp.setValue(LoadBalancingModeEnum.weightedRoundRobin);
		} else if (lbMode.equals(LoadBalancingModeEnum._clusterCompatible)) {
			lbModeProp.setValue(LoadBalancingModeEnum.clusterCompatible);
		} else {
			throw new CoCoMaC8Exception("Invalid value for loadBalacingMode: " + lbMode + ". Possible values are: '"
					+ LoadBalancingModeEnum._clusterCompatible + "' or '" + LoadBalancingModeEnum._weightedRoundRobin
					+ "'.");
		}

		disp.setLoadBalancingMode(lbModeProp);
	}

	/**
	 * @param conf
	 * @param dispData
	 * @throws CoCoMaC8Exception
	 */
	private void setLoadBalacingMode(Configuration conf, DispatcherData dispData) throws CoCoMaC8Exception {

		String lbMode = dispData.getLoadBalancingMode();

		log.debug("Setting loadBalancing mode to " + lbMode);

		LoadBalancingModeEnumProp lbModeProp = new LoadBalancingModeEnumProp();

		if (lbMode.equals(LoadBalancingModeEnum._weightedRoundRobin)) {
			lbModeProp.setValue(LoadBalancingModeEnum.weightedRoundRobin);
		} else if (lbMode.equals(LoadBalancingModeEnum._clusterCompatible)) {
			lbModeProp.setValue(LoadBalancingModeEnum.clusterCompatible);
		} else {
			throw new CoCoMaC8Exception("Invalid value for loadBalacingMode: " + lbMode + ". Possible values are: '"
					+ LoadBalancingModeEnum._clusterCompatible + "' or '" + LoadBalancingModeEnum._weightedRoundRobin
					+ "'.");
		}

		conf.setLoadBalancingMode(lbModeProp);
	}

	private String buildAdvancedPropertyXML(String property, String value) {
		StringBuffer cred = new StringBuffer();
		cred.append("<settings>");
		cred.append("<setting name=\"").append(property).append("\">");
		cred.append(value).append("</setting>");
		cred.append("</settings>");

		XmlEncodedXML credentials = new XmlEncodedXML(cred.toString());
		return credentials.get_value();
	}
}
