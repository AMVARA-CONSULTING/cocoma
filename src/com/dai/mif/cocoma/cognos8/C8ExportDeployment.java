/**
 * $Id$
 */
package com.dai.mif.cocoma.cognos8;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DeploymentDetail;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionAnyType;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.ExportDeployment;
import com.cognos.developer.schemas.bibus._3.FaultDetail;
import com.cognos.developer.schemas.bibus._3.FaultDetailMessage;
import com.cognos.developer.schemas.bibus._3.History;
import com.cognos.developer.schemas.bibus._3.HistoryDetail;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualString;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.config.BackupData;
import com.dai.mif.cocoma.logging.Logging;

/**
 * Full Content Store Backup based on IBM examples
 *
 * @author NOW! Consulting GmbH
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since 13.10.2014
 * @version $Revision$ ($Date:: YYYY-MM-DD hh:mm:ss #$)
 *
 * @see http://www-01.ibm.com/support/docview.wss?uid=swg21338960
 */
public class C8ExportDeployment {
	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType mService = null;
	private Logger log;

	public static String logFile = "Export_Final.csv";
	private String deployType = "export";
	String strLocale = "en";
	private String password = null;
	private String name;
	private boolean use_DateTimeSuffix;
	private C8Access c8Access = null;
	private C8Utility c8Utility = null;

	public C8ExportDeployment(C8Access c8Access, BackupData backupData) {
		this.c8Utility = new C8Utility(c8Access);
		this.c8Access = c8Access;
		this.cmService = c8Access.getCmService();
		// XXX Throws error with CA11 - this.mService =
		// c8Access.getMonitorService(true,c8Access.getUrl());
		java.net.URL serverURL = null;
		try {
			serverURL = new java.net.URL(c8Access.getUrl());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			this.mService = c8Access.getMonitorServiceLocator().getmonitorService(serverURL);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.log = Logging.getInstance().getLog(this.getClass());

		this.name = backupData.getName();
		this.password = backupData.getPassword();
		this.use_DateTimeSuffix = backupData.getUseDateTimeSuffix();
	}

	public void createExport() {

		// Setup name of JOB in Cognos to be executed as FullCSBackup
		String expDeploymentName = this.name;

		// Add datatime suffix to the JOB name?
		if (this.use_DateTimeSuffix) {
			String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime());
			expDeploymentName += "-" + datetime;
		}

		if (this.password != null) { // required
			log.debug("Creating full content store backup: " + expDeploymentName);
			this.deployContentCS(expDeploymentName);
		} else {
			log.error("You must provide a password to create FullContentStore Backup deployment archive.");
		}
	}

	private BaseClass[] addArchive(String deploySpec, String nameOfArchive) {
		ExportDeployment exportDeploy = null;
		BaseClass[] addedDeploymentObjects = null;
		BaseClass[] bca = new BaseClass[1];
		AddOptions addOpts = null;

		SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject("/adminFolder");

		MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
		MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
		MultilingualToken myMultilingualToken = new MultilingualToken();

		myMultilingualToken.setLocale(strLocale);
		myMultilingualToken.setValue(nameOfArchive);
		multilingualTokenArr[0] = myMultilingualToken;
		multilingualTokenProperty.setValue(multilingualTokenArr);

		exportDeploy = new ExportDeployment();
		addOpts = new AddOptions();
		exportDeploy.setName(multilingualTokenProperty);
		addOpts.setUpdateAction(UpdateActionEnum.replace);
		bca[0] = exportDeploy;
		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath, bca, addOpts);
		} catch (RemoteException remoEx) {
			log.error("An error occurred when adding a deployment object:" + "\n" + remoEx.getMessage());
		}
		if ((addedDeploymentObjects != null) && (addedDeploymentObjects.length > 0)) {
			return addedDeploymentObjects;
		} else {
			return null;
		}
	}

	// setting options to export ENTIRE CS
	private Option[] setDeploymentOptionEnumCS(String deploymentType, String nameOfArchive, String exportPassword) {
		Option[] deploymentOptions = null;
		int num = 0;
		int eOptionCount = 0;

		log.debug("Setting options to export Content Store");
		String[] deployOptionEnumBoolean = { "entireContentStoreSelect", "archiveOverwrite", "personalDataSelect" };
		String[] deployOptionEnumResolution = { "archive", "archiveEncryptPassword" };

		deploymentOptions = new DeploymentOption[eOptionCount + deployOptionEnumBoolean.length
				+ deployOptionEnumResolution.length];

		deploymentOptions[num] = this.setEntireContentStoreSelect(true); // choose
																			// entire
																			// CS
		deploymentOptions[++num] = this.setArchiveOverWrite(true); // overwrite
		deploymentOptions[++num] = this.setPersonalDataSelect(true); // default
																		// is
																		// false

		deploymentOptions[++num] = this.setDeploymentOptionString(nameOfArchive); // archive
																					// name
		deploymentOptions[++num] = this.setArchiveEncryptPassword(exportPassword); // secure
																					// by
																					// password

		return deploymentOptions;
	}

	public String deployContentCS(String strArchiveName) {
				
		AsynchReply asynchReply = null;
		String reportEventID = "-1";
		int sleepTimerMS=15000;
		int maxTries=15;
		int counter=0;
		

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(deployType, strArchiveName);

		log.debug("Added archive: " + strArchiveName);
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
		} else {
			return reportEventID;
		}

		log.debug("Preparing runoptions for archive");
		Option[] myDeploymentOptionsEnum = null;
		myDeploymentOptionsEnum = setDeploymentOptionEnumCS(deployType, strArchiveName, password);

		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ExportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

		log.debug("Executing archive");
		try {
			cmService.update(ArchiveInfo, new UpdateOptions());
		} catch (RemoteException remoteEx) {
			log.error("An error occurred while deploying content:" + "\n" + remoteEx.getMessage());
			remoteEx.printStackTrace();
		}
		log.debug("Getting async reply..");
		try {
			// asynchReply = mService.run(searchPathObject, new ParameterValue[] {}, new Option[] {});
			
			asynchReply = c8Access.getMonitorService().run(searchPathObject, new ParameterValue[] {}, new Option[] {});
			
			/**
			 * FIX FOR
			 * <errorCode>
			 * 	CANNOT_FORWARD_TO_ABSOLUTE_AFFINITY_NODE
			 * </errorCode>
			 * <messageString>
			 * 	DPR-ERR-2072 Unable to load balance a request with absolute affinity, most likely due to a failure to connect to the remote dispatcher. See the remote dispatcher detailed logs for more information. Check the health status of the installed system by using the dispatcher diagnostics URIs.
			 * </messageString> 
			 */
			BiBusHeader bibus = C8Access.getHeaderObject(((Stub)c8Access.getMonitorService()).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), false, "");
	        
			if(bibus == null) {
				BiBusHeader CMbibus = null;
				CMbibus = C8Access.getHeaderObject(((Stub)c8Access.getCmService()).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), true, "");
				((Stub)c8Access.getMonitorService()).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", CMbibus);
			} else {
				((Stub)c8Access.getMonitorService()).clearHeaders();
				((Stub)c8Access.getMonitorService()).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", bibus);
			}
			
			
			if (!(asynchReply.getStatus().equals(AsynchReplyStatusEnum.complete))
					&& !(asynchReply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) {
				log.debug("Call AsyncReply wait");
				asynchReply = c8Access.getAsyncReply(asynchReply);
				log.debug("Waiting Finished.");
			}
			log.debug("AsynchReplyStatus: " + asynchReply.getStatus());			
			
			History history = null;
			
			do {
				BaseClass[] histories = c8Utility.findObjectsInSearchPath("/adminFolder/exportDeployment[@name='" + strArchiveName + "']/*");
				
				// history path: asynchReply.getPrimaryRequest().getOptions()[1].getValue().toString(); // maybe?
				// Option[] asynchOptions = asynchReply.getPrimaryRequest().getOptions();
				String eventHistoryLocation = null; // ((AsynchOptionSearchPathSingleObject) asynchOptions[1]).getValue().get_value();
				eventHistoryLocation = histories[0].getSearchPath().getValue(); 
				
				
				// get the object from CM
				BaseClass[] historyObject = c8Utility.fetchObjectsWithQueryOptions(eventHistoryLocation, c8Utility.setPropEnum(), new Sort[] {}, c8Utility.setQORefProps());
				history = (History) historyObject[0];
				if(history.getStatus().getValue().equals("succeeded")) {
					log.debug("The export has been executed successfully... job status: " + history.getStatus().getValue());
					printHistoryDetails(eventHistoryLocation);
				}else if (history.getStatus().getValue().equals("executing")) {
					log.debug("Try: " + counter + " | MAX: " + maxTries);
					log.debug("Got Job status: " + history.getStatus().getValue());
					log.debug("Will wait for little while to check if the job has finished.");
					try {
						log.debug("Sleeping " + sleepTimerMS + " milliseconds.");
						Thread.sleep(sleepTimerMS);
					} catch (InterruptedException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}else {
					log.error("The export did not execute successfully... job status: " + history.getStatus().getValue());
					printHistoryDetails(eventHistoryLocation);
					log.error("Will exit with code 5 to prevent further execution.");
					System.exit(5);
				}
				counter++;
			} while(history.getStatus().getValue().equals("executing") && counter <= maxTries);
		} catch (RemoteException remoteEx) {
			log.error("An error occurred while deploying content:" + "\n" + remoteEx.getMessage());
			remoteEx.printStackTrace();
			log.error("Severe Error! Content Store Backup failed.");
			log.error("Check your cogserver.log for more details.");
			System.exit(5);
		}

		return reportEventID;
	}

	/**
	 * Pass in the job history location and get the output message from the history.
	 * @param eventHistoryLocation
	 */
	private void printHistoryDetails(String eventHistoryLocation) {
		log.debug("Export details:");
		BaseClass[] historyObject = c8Utility.fetchObjectsWithQueryOptions(eventHistoryLocation + "/*", c8Utility.setPropEnum(), new Sort[] {}, c8Utility.setQORefProps());
		
		int index = historyObject[0] instanceof HistoryDetail || historyObject[0] instanceof DeploymentDetail ? 0 : 1;
		
		if(historyObject[index] instanceof DeploymentDetail) {
			DeploymentDetail exportDetails = (DeploymentDetail) c8Utility.fetchObjectsWithQueryOptions(historyObject[index].getSearchPath().getValue(), c8Utility.setPropEnum(), new Sort[] {}, c8Utility.setQORefProps())[0];
			for(FaultDetail detail : exportDetails.getMessage().getValue()) {
				for(FaultDetailMessage message : detail.getMessage()) {
					log.log(Level.toLevel(detail.getSeverity().equals("warning") ? "warn" : detail.getSeverity()), message.getMessage());
				}
			}
		} else if (historyObject[index] instanceof HistoryDetail) {
			HistoryDetail exportDetails = (HistoryDetail) c8Utility.fetchObjectsWithQueryOptions(historyObject[index].getSearchPath().getValue(), c8Utility.setPropEnum(), new Sort[] {}, c8Utility.setQORefProps())[0];
			log.error(exportDetails.getDetail().getValue());
		} else {
			log.debug("Was expectin HistoryDetail or DeploymentDetail object but found: " + historyObject[index].getClass().getName() + ", which is not implemented yet.");
		}
		
	}

	// /This method logs the user to Cognos BI
	public String quickLogon(String namespace, String uid, String pwd) {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace).append("</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		// Invoke the ContentManager service logon() method passing the
		// credential string
		// You will pass an empty string in the second argument. Optionally,
		// you could pass the Role as an argument
		try {
			cmService.logon(xmlCredentials, null);
			SOAPHeaderElement temp = ((Stub) cmService)
					.getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader) temp
					.getValueAsType(new QName("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"));
			((Stub) cmService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
			((Stub) mService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
		} catch (Exception e) {
			System.out.println(e);
		}
		return ("Logon successful as " + uid);
	}// quickLogon

	public void connectToReportServer(String endPoint) { // This method connects
															// to Cognos BI
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
		MonitorService_ServiceLocator mServiceLocator = new MonitorService_ServiceLocator();

		try {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
			mService = mServiceLocator.getmonitorService(new java.net.URL(endPoint));

			// set the Axis request timeout
			((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the
												// timeout off
			((Stub) mService).setTimeout(0); // in milliseconds, 0 turns the
												// timeout off
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
	}// connectToReportServer

	private DeploymentOptionBoolean setEntireContentStoreSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("entireContentStoreSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}// setArchiveOverWrite

	private DeploymentOptionAnyType setArchiveEncryptPassword(String pPassword) {
		DeploymentOptionAnyType archiveEncryptPassword = null;
		if (pPassword != null && pPassword.length() >= 1) {
			archiveEncryptPassword = new DeploymentOptionAnyType();
			archiveEncryptPassword.setValue("<credential><password>" + pPassword + "</password></credential>");
			archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
		}
		return archiveEncryptPassword;
	}// setArchiveEncryptPassword

	private DeploymentOptionString setDeploymentOptionString(String archiveName) { // mandatory
		MultilingualString archiveDefault = new MultilingualString();
		archiveDefault.setLocale(strLocale);
		archiveDefault.setValue(archiveName);

		DeploymentOptionString deployOptionStr = new DeploymentOptionString();
		deployOptionStr.setName(DeploymentOptionEnum.fromString("archive"));
		deployOptionStr.setValue(archiveDefault.getValue());

		return deployOptionStr;
	}// setDeploymentOptionString

	private DeploymentOptionBoolean setArchiveOverWrite(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}// setArchiveOverWrite

	// allow the deployment overwrites the archive
	private DeploymentOptionBoolean setPersonalDataSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("personalDataSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}// setPersonalDataSelect

}
