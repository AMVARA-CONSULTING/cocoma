/**
 * $Id: C8Deployment.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.Calendar;
//import java.util.Date;
//import java.util.List;

//import org.apache.axis.AxisFault;
//import org.apache.axis.description.TypeDesc;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
//import com.cognos.developer.schemas.bibus._3.AuditLevelEnum;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DeploymentDetail;
import com.cognos.developer.schemas.bibus._3.DeploymentObjectInformation;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
//import com.cognos.developer.schemas.bibus._3.DeploymentOptionAuditLevel;
//import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
//import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionMultilingualString;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
//import com.cognos.developer.schemas.bibus._3.Dispatcher_Type;
//import com.cognos.developer.schemas.bibus._3.History;
//import com.cognos.developer.schemas.bibus._3.HistoryDetailDeploymentSummary;
//import com.cognos.developer.schemas.bibus._3.HistoryDetailRequestArguments;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.FaultDetail;
import com.cognos.developer.schemas.bibus._3.FaultDetailArrayProp;
import com.cognos.developer.schemas.bibus._3.FaultDetailMessage;
import com.cognos.developer.schemas.bibus._3.ImportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MultilingualString;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
//import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.config.DeploymentArchive;
import com.dai.mif.cocoma.config.DeploymentData;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since Mar 17, 2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
/**
 * @author rroeber
 *
 */
/**
 * @author rroeber
 * 
 */
public class C8Deployment {

	private C8Access c8Access;
	private static Logger log;
	private DeploymentData deploymentData;
	private String strLocale = "en";

	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType monitorService = null;

	/*
	 * Target ZIP Archive name of deployment to be place in Deployment folder of
	 * cognos
	 */
	public String deploymentTargetArchive = "";

	private static final String DEPLOY_OPTION_NAME = "com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray";
	private static final String DEPLOY_OPTION_MLSTRING = "com.cognos.developer.schemas.bibus._3.DeploymentOptionMultilingualString";
	private static final String DEPLOY_OPTION_STRING = "com.cognos.developer.schemas.bibus._3.DeploymentOptionString";

	/**
	 * @param deploymentData
	 * @param c8Access
	 */
	public C8Deployment(DeploymentData deploymentData, C8Access c8Access) {
		this.c8Access = c8Access;
		C8Deployment.log = Logging.getInstance().getLog(this.getClass());
		this.deploymentData = deploymentData;
	}

	public boolean prepareDeploymentArchive(String deploymentName,
			String deploymentPassword, String deploymentSourceArchive)
			throws CoCoMaC8Exception {

		String deploymentFolder = this.deploymentData.getDeploymentFolder();
		String archiveName = "";

		log.debug("Preparing deployment archive");
		log.debug("DeploymentName: " + deploymentName);
		log.debug("DeploymentSrcArchive: " + deploymentSourceArchive);
		this.deploymentData
				.setDeploymentSrcArchiveName(deploymentSourceArchive);

		if (deploymentFolder == null) {
			// No deployment folder entered, trying to continue (using
			// deployment name)
			log.error("No deployment folder entered in the configuration.");
			log.error("Without deployment folder, no deployment possible.");
			System.exit(2);
			return true;
		} else {
			log.info("Using Deploymentfolder : " + deploymentFolder);
			this.deploymentData.setDeploymentTargetFolder(deploymentFolder);
		}

		File deploymentFolderFile = new File(deploymentFolder);
		if (!deploymentFolderFile.isDirectory()) {
			// Invalid deployment folder entered, trying to continue (using
			// deployment name)
			log.warn("The deployment folder '" + deploymentFolder
					+ "' does not exist or is not a directory.");
			log.warn("Will try using current folder");
		} else {
			log.debug("Checked Deploymentfolder to exist and be a directory successfull");
		}

		File deploymentSourceFile;
		if (deploymentSourceArchive.length() == 0) {
			// deploymentSourceArchive is emtpy ... will try deploymentName
			// instead
			deploymentSourceArchive = deploymentFolder + File.separatorChar
					+ deploymentName + ".zip";
			this.deploymentData
					.setDeploymentSrcArchiveName(deploymentSourceArchive);

			deploymentSourceFile = new File(deploymentSourceArchive);
			if (!deploymentSourceFile.exists()) {
				log.warn("There is no deployment archive file with the name '"
						+ deploymentName
						+ "'. Using existing Import package if possible.");

				// No additional steps needed, because no renaming would be
				// possible
				return false;
			}

			// Set archive file to rename the file later
			this.deploymentData.setArchive(deploymentSourceArchive);

		} else {
			// Using archive as configured
			deploymentSourceFile = new File(deploymentSourceArchive);
			this.deploymentData
					.setDeploymentSrcArchiveName(deploymentSourceArchive);
			if (!deploymentSourceFile.exists()) {
				log.error("The deployment '" + deploymentSourceArchive
						+ "' does not exist.");
				log.error("Deployment not possible. Check config file and file situation on drive.");
				CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, "The deployment '" + deploymentSourceArchive
						+ "' does not exist. Deployment not possible. Check config file and file situation on drive.");
				throw new CoCoMaC8Exception(
						"An error occurred while deploying content:");
			} else {
				log.debug("Deploymentarchive:" + deploymentSourceArchive);
				// Show size in kBytes
				long filesize = deploymentSourceFile.length() / 1024;
				filesize = Math.round(filesize);
				log.debug("Filesize: " + filesize + " kBytes");
				if (filesize > 1000) {
					log.info("Deployment file is larger than 1MB - pls. be patient.");
				}
				if (filesize > 5000) {
					log.info("Deployment file is larger than 5MB - pls. be very patient. This may take a while.");
				}
			}

			// Checking if it's possible to copy to deployment folder
			boolean writableDir = new File(deploymentFolder).canWrite();
			if (!writableDir) {
				log.error("Unable to write to deployment folder '"
						+ deploymentFolder + "'.");
				return false;
			} else {
				log.info("Check if deploymentFolder has write access done: OK");
			}
		}

		// Determine the correct name of the original archive inside the ZIP
		// file (i. e. after manual
		// renaming) with parsing the zip archive (if deployment is not
		// encrypted)
		DeploymentArchive zipArchiv = new DeploymentArchive(
				deploymentSourceArchive, deploymentPassword);
		this.deploymentData
				.setDeploymentNameOfArchiveInZipDeploymentFile(zipArchiv
						.determineOriginName());
		if (this.deploymentData.getDeploymentNameOfArchiveInZipDeploymentFile()
				.length() == 0) {
			log.warn("Archive name could not be determined. Using \""
					+ deploymentName + "\" as fallback.");
			archiveName = deploymentName + ".zip";
		}

		// String for DateTime as Suffix or Prefix
		String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss")
				.format(Calendar.getInstance().getTime());

		// Check if deploymentFolder ends with "/" (trailing slash)
		if (deploymentFolder.endsWith("/") || deploymentFolder.endsWith("\\")) {
			log.debug("DeploymentFolder config option has trailing slash. ok.");
		} else {
			log.debug("Deploymentfolder is missing trailing slash. Added "
					+ File.separatorChar);
			deploymentFolder += File.separatorChar;
		}

		// Check if a new target name is needed
		// String oldArchiveName = deploymentSourceFile.getName();
		// if (oldArchiveName.compareTo(archiveName) != 0
		// && archiveName.length() > 0) {
		// deploymentTargetArchive = deploymentFolder + archiveName;
		// this.deploymentData.setName(archiveName.replace(".zip", ""));
		// log.debug("Changed archive name from '" + oldArchiveName + "' to '"
		// + archiveName + "'");
		//
		// } else if (deploymentSourceArchive.startsWith(deploymentFolder)) {
		// // Same name + same target = no further steps needed
		// log.debug("Same name + same target ... nothing special todo, just logging.");
		// } else {
		// deploymentTargetArchive = deploymentFolder
		// + "_autodepl_" +datetime+ "_" +deploymentSourceFile.getName();
		// }

		// Add Timestamp to deploymenttargtArchive
		deploymentTargetArchive = deploymentFolder + "_autodepl_" + datetime
				+ "_" + deploymentSourceFile.getName();

		File deploymentTargetFile = new File(deploymentTargetArchive);

		// Check if the target file already exists
		if (deploymentTargetFile.exists()) {
			log.warn("The file '"
					+ deploymentTargetFile.getName()
					+ "' already exists in the deployment folder. Renaming the file.");
			if (!deploymentTargetFile.canWrite()) {
				// No write permissions, rename wouldn't be possible
				log.error("No write permissions for '"
						+ deploymentTargetFile.getName() + "'.");
				return false;
			}

			String tmpTargetName = deploymentTargetArchive + "-" + datetime;
			File tmpTargetFile = new File(tmpTargetName);
			boolean targetSuccess = deploymentTargetFile
					.renameTo(tmpTargetFile);
			if (!targetSuccess) {
				log.error("Renaming of the existing archive failed");
				return false;
			} else {
				log.info("File was renamed to: " + tmpTargetFile);
			}
		}

		// Copy archive to deployment folder (with correct name)
		log.debug("Copy " + deploymentSourceArchive + " --> "
				+ deploymentTargetArchive);
		this.deploymentData
				.setDeploymentSrcArchiveName(deploymentSourceArchive);
		this.deploymentData
				.setDeploymentTargetArchiveName(deploymentTargetArchive);

		FileInputStream fiss = null;
		FileOutputStream fisd = null;
		long transfered = 0;
		try {
			try {
				fiss = new FileInputStream(deploymentSourceArchive);
				FileChannel sourceChannel = fiss.getChannel();

				fisd = new FileOutputStream(deploymentTargetArchive);
				FileChannel destChannel = fisd.getChannel();

				transfered = destChannel.transferFrom(sourceChannel, 0,
						sourceChannel.size());
			} finally {
				if (fiss != null) {
					fiss.close();
				}
				if (fisd != null) {
					fisd.close();
				}
			}
		} catch (IOException e) {
			log.error("Copy process failed.");
			log.debug(e.getMessage());
			return false;
		}

		if (transfered > 0) {
			log.debug("Copy process successful.");
			return true;
		} else {
			log.error("Copy process failed. Transfered bytes = 0. Check target permissions, check network.");
			return false;
		}
	}

	private BaseClass[] addArchive(String deploySpec, String nameOfArchive) {

		ImportDeployment importDeploy = null;
		BaseClass[] addedDeploymentObjects = null;
		BaseClass[] bca = new BaseClass[1];
		AddOptions addOpts = null;

		SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject(
				"/adminFolder");

		MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
		MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
		MultilingualToken myMultilingualToken = new MultilingualToken();

		myMultilingualToken.setLocale(strLocale);
		myMultilingualToken.setValue(nameOfArchive);
		multilingualTokenArr[0] = myMultilingualToken;
		multilingualTokenProperty.setValue(multilingualTokenArr);

		importDeploy = new ImportDeployment();
		addOpts = new AddOptions();
		importDeploy.setName(multilingualTokenProperty);
		addOpts.setUpdateAction(UpdateActionEnum.replace);
		bca[0] = importDeploy;

		try {
			ContentManagerService_PortType cmService = this.c8Access
					.getCmService();
			addedDeploymentObjects = cmService.add(objOfSearchPath, bca,
					addOpts);
		} catch (RemoteException remoEx) {
			System.out
					.println("An error occurred when adding a deployment object:"
							+ "\n" + remoEx.getMessage());
		}
		if ((addedDeploymentObjects != null)
				&& (addedDeploymentObjects.length > 0)) {
			return addedDeploymentObjects;
		} else {
			return null;
		}
	} // addArchive

	public Option[] getDeployedOption(String myArchive) {
		Option[] deployOptEnum = new Option[] {};

		cmService = this.c8Access.getCmService();

		try {
			deployOptEnum = cmService.getDeploymentOptions(myArchive,
					new Option[] {});
		} catch (RemoteException e) {
			System.out
					.println("An error occurred in getting Deployment options."
							+ "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return deployOptEnum;
	} // getDeployedOption

	/**
	 * Imports the deployment
	 * 
	 * @throws CoCoMaC8Exception
	 * @throws RemoteException
	 * @throws InterruptedException 
	 */
	public String deployContent(String strNewImportName,
			String strDeployedArchive) throws CoCoMaC8Exception,
			RemoteException, InterruptedException {

		log.debug("Create deployment in Contentstore now ... ");
		log.debug("NewImportName: " + strNewImportName);
		log.debug("DeployedArchive: " + strDeployedArchive);
		AsynchReply asynchReply = null;
		String reportEventID = "false";
		String deployType = "import";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(deployType, strNewImportName);

		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
			log.info("Import Archive prepared at: " + deployPath);
		} else {
			return reportEventID;
		}

		// Log ArchiveInfo
		// log.debug("ArchiveInfo:");
		// log.debug("----");
		// log.debug(" CreationTime: " + ArchiveInfo[0].getCreationTime());
		// log.debug(" DefaultName: " + ArchiveInfo[0].getDefaultName());
		// log.debug(" Disabled: " + ArchiveInfo[0].getDisabled());
		// log.debug(" Name: " + ArchiveInfo[0].getName());
		// log.debug(" Owner: " + ArchiveInfo[0].getOwner());
		// log.debug(" Position: " + ArchiveInfo[0].getPosition());
		// log.debug(" HasChildren: " + ArchiveInfo[0].getHasChildren());
		// log.debug(" Version: " + ArchiveInfo[0].getVersion());
		// log.debug("----");

		Option[] myDeploymentOptionsEnum = null;
		myDeploymentOptionsEnum = getDeployedOption(strDeployedArchive);

		// Loop over DeploymentOptionsEnum
		log.debug("ArchiveOptions:");
		log.debug("----");
		for (int i = 0; i < myDeploymentOptionsEnum.length; i++) {
			Option oname = myDeploymentOptionsEnum[i];
			DeploymentOption onameS = (DeploymentOption) oname;
			String optionName = onameS.getName().getValue();
			log.debug(i + ".) Deployment Option: " + optionName + " ");

			String OptionClassName = oname.getClass().getName();

			if (DEPLOY_OPTION_NAME == OptionClassName) {
				DeploymentObjectInformation[] packDeployInfo = ((DeploymentOptionObjectInformationArray) myDeploymentOptionsEnum[i])
						.getValue();

				// Loop over DeploymentObjectInformation
				for (int j = 0; j < packDeployInfo.length; j++) {
					String packFolderName = packDeployInfo[j].getDefaultName();
					SearchPathSingleObject packagePath = packDeployInfo[j]
							.getSearchPath();
					log.debug("FolderName: " + packFolderName
							+ " PackagePath: " + packagePath);
				}
			} else if (DEPLOY_OPTION_MLSTRING == OptionClassName) {
				MultilingualString[] packDeployptionStrings = ((DeploymentOptionMultilingualString) myDeploymentOptionsEnum[i])
						.getValue();
				// Loop over DeploymentObjectInformation
				for (int j = 0; j < packDeployptionStrings.length; j++) {
					String packDeployStringsValue = packDeployptionStrings[j]
							.getValue();
					if (packDeployStringsValue.length() == 0) {
						packDeployStringsValue = "<not set>";
					}
					if (optionName == "deploymentScreenTip") {
						packDeployptionStrings[j]
								.setValue("This is an autodeployment entry.");
						((DeploymentOptionMultilingualString) myDeploymentOptionsEnum[i])
								.setValue(packDeployptionStrings);
						packDeployStringsValue = packDeployptionStrings[j]
								.getValue();
					}
					String packDeployStringsClass = packDeployptionStrings[j]
							.getClass().toString();
					log.debug(optionName + ": " + packDeployStringsValue + " ["
							+ packDeployStringsClass + "]");
				}
			} else if (DEPLOY_OPTION_STRING == OptionClassName) {
				String deployOptionString = ((DeploymentOptionString) myDeploymentOptionsEnum[i])
						.getValue();
				log.debug(optionName + ": " + deployOptionString);
				if (optionName == "archive") {
					((DeploymentOptionString) myDeploymentOptionsEnum[i])
							.setValue(strDeployedArchive);
					log.debug("Archive name set to " + strDeployedArchive);
				}

			} else {
				// log.debug("ClassDebug Information : Not implemented yet.");
			}
		}
		log.debug("----");

		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);

		((ImportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

		try {
			log.debug("Executing deployment ");
			cmService.update(ArchiveInfo, new UpdateOptions());

			
		} catch (RemoteException remoteEx) {
//			log.error("An error occurred while deploying content:"
//					+ remoteEx.getMessage());
//			remoteEx.printStackTrace();
//			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, remoteEx
//					.getMessage().toString());
//			throw new CoCoMaC8Exception(
//					"An error occurred while deploying content:", remoteEx);
			log.error("RemoteException caught");
			reportEventID = "remoteException";
		}
		
		log.debug("Executing Monitoring for deployment ");
		monitorService = this.c8Access.getMonitorService(false, this.c8Access.getUrl());
		asynchReply = monitorService.run(searchPathObject,
				new ParameterValue[] {}, new Option[] {});
		
		// Check for deployment to finish
		if (!(asynchReply.getStatus().equals(AsynchReplyStatusEnum.complete))
				&& !(asynchReply.getStatus()
						.equals(AsynchReplyStatusEnum.conversationComplete))) {
			log.debug("Call AsyncReply wait");
			asynchReply = c8Access.getAsyncReply(asynchReply);
			log.debug("Waiting Finished.");
		}
		
		log.debug("AsynchReplyStatus: "+asynchReply.getStatus());
		
		reportEventID = "true";
		
		return reportEventID;
	}// deployContent


	/**
	 * displayImportHistory
	 * 
	 * @parameters String name, String impDeploymentName
	 */
	public void displayImportHistory(String name, String impDeploymentName) {
		PropEnum props[] = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath, PropEnum.deployedObjectStatus,
				PropEnum.objectClass, PropEnum.status, PropEnum.hasMessage,
				PropEnum.deployedObjectClass, PropEnum.message,
				PropEnum.detail, PropEnum.actualExecutionTime,
				PropEnum.actualCompletionTime };

		String impPath = "/adminFolder/importDeployment[@name='"
				+ (impDeploymentName.length() > 0 ? impDeploymentName : name)
				+ "']" + "//history//*";

		log.info("History for: " + impPath);
		String msg = "Import started on "
				+ Calendar.getInstance().getTime().toString() + ": "
				+ "Importing \"" + name + "\"";
		log.info(msg);
		
		SearchPathMultipleObject spMulti = new SearchPathMultipleObject(impPath);

		BaseClass bc[] = null;
		try {
			bc = cmService.query(spMulti, props, new Sort[] {},
						new QueryOptions());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (bc != null && bc.length > 0)
			for (int i = 0; i < bc.length; i++) {
				if (bc[i].getObjectClass().getValue().toString() == "deploymentDetail" ) {
					DeploymentDetail dd = (DeploymentDetail) bc[i];
					// Print messages if any
					FaultDetailArrayProp faultArray = dd.getMessage();
					FaultDetail[] faultDetail = faultArray.getValue();
					for(int j=0;j<faultDetail.length;j++) {
						FaultDetailMessage[] faultMessage = faultDetail[j].getMessage();
						log.info("Message: "+faultMessage[0].getMessage() );
					}
				}
			}

	}// displayImportHistory

	/**
	 * Imports the deployment
	 * 
	 * @throws CoCoMaC8Exception
	 */
	public void execute() throws CoCoMaC8Exception {

		// ContentManagerService_PortType cms = this.c8Access.getCmService();
		// MonitorService_PortType monitorService = this.c8Access
		// .getMonitorService();

		String deploymentName = this.deploymentData.getName();

		if (this.deploymentData.get_nameSetDateTimeSuffix().equalsIgnoreCase(
				"true")) {
			String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss")
					.format(Calendar.getInstance().getTime());
			deploymentName = deploymentName + "_" + datetime;
			log.debug("Changed deployment name to : " + deploymentName);
		} else {
			log.debug("No suffix configured in config file. Deployment name remains unchanged.");
		}

		String deploymentPassword = this.deploymentData.getPassword();
		String deploymentArchive = this.deploymentData.getArchive();

		log.debug("Deploymentname : " + deploymentName);
		log.debug("Deploymentarchive Incoming : " + deploymentArchive);

		log.info("Prepare Deploymentarchive");
		if (prepareDeploymentArchive(deploymentName, deploymentPassword,
				deploymentArchive)) {
			/* Build name for Cognos Deployment Archive without ZIP */

			String cognosDeploymentArchive = new File(deploymentTargetArchive)
					.getName().toString();
			cognosDeploymentArchive = cognosDeploymentArchive.replace(".zip",
					"");

			log.debug("Deploymentarchive at Cognos: " + cognosDeploymentArchive);
			log.info("Deploying the prepared archive now.");
			String strNewImportName = "_autodeployment_"
					+ deploymentName
					+ "_"
					+ this.deploymentData
							.getDeploymentNameOfArchiveInZipDeploymentFile();

			/* Deploy content now */
			String returnresult_deployContent = "";
			try {
				returnresult_deployContent = this.deployContent(
						strNewImportName, cognosDeploymentArchive);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.debug("return value: " + returnresult_deployContent);
			displayImportHistory(strNewImportName, strNewImportName);

		} else {
			log.error("prepareDeploymentArchive not successfull.");
			log.error("No further deployment possible. Pls. check CoCoMa Logfile for details.");
		}
	}
}
