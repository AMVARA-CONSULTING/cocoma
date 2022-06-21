/**
 * $Id: C8Deployment.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.DeploymentDetail;
import com.cognos.developer.schemas.bibus._3.DeploymentObjectInformation;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionArrayProp;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.FaultDetail;
import com.cognos.developer.schemas.bibus._3.FaultDetailArrayProp;
import com.cognos.developer.schemas.bibus._3.FaultDetailMessage;
import com.cognos.developer.schemas.bibus._3.ImportDeployment;
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
import com.dai.mif.cocoma.cognos.util.C8Utility;
import com.dai.mif.cocoma.config.DeploymentArchive;
import com.dai.mif.cocoma.config.DeploymentData;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;
import com.esotericsoftware.wildcard.Paths;

/**
 *
 * @author riedchr
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
	private C8Utility c8Utility = null;
	private static Logger log;
	private DeploymentData deploymentData;
	private String strLocale = "en";

	private ContentManagerService_PortType cmService = null;
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
		this.c8Utility = new C8Utility(c8Access);
		this.c8Access = c8Access;
		C8Deployment.log = Logging.getInstance().getLog(this.getClass());
		this.deploymentData = deploymentData;
	}

	public boolean prepareDeploymentArchive(String deploymentName, String deploymentPassword,
			String deploymentSourceArchive) throws CoCoMaC8Exception {

		String deploymentFolder = this.deploymentData.getDeploymentFolder();
		log.debug("Preparing deployment archive");
		log.debug("DeploymentName: " + deploymentName);
		log.debug("DeploymentSrcArchive: " + deploymentSourceArchive);
		this.deploymentData.setDeploymentSrcArchiveName(deploymentSourceArchive);

		if (deploymentFolder == null || deploymentFolder.length() == 0) {
			// No deployment folder entered, trying to continue (using
			// deployment name)
			log.warn("No deployment folder entered in the configuration.");
			log.warn(
					"Trying deployment without copying files to target folder. Hope it is there already. We will se later, if Cognos finds it.");
		} else {
			log.info("Using Deploymentfolder : " + deploymentFolder);
			this.deploymentData.setDeploymentTargetFolder(deploymentFolder);
			File deploymentFolderFile = new File(deploymentFolder);
			if (!deploymentFolderFile.isDirectory()) {
				// Invalid deployment folder entered, trying to continue (using
				// deployment name)
				log.warn("The deployment folder '" + deploymentFolder + "' does not exist or is not a directory.");
				log.warn("Will try using current folder");
			} else {
				log.debug("Checked Deploymentfolder to exist and be a directory successfull");
			}
			// Checking if it's possible to copy to deployment folder
			boolean writableDir = new File(deploymentFolder).canWrite();
			if (!writableDir) {
				log.error("Unable to write to deployment folder '" + deploymentFolder + "'.");
				return false;
			} else {
				log.info("Check if deploymentFolder has write access done: OK");
			}
		}

		File deploymentSourceFile;
		if (deploymentSourceArchive.length() == 0) {
			// deploymentSourceArchive is emtpy ... will try deploymentName
			// instead
			deploymentSourceArchive = deploymentFolder + File.separatorChar + deploymentName + ".zip";
			this.deploymentData.setDeploymentSrcArchiveName(deploymentSourceArchive);

			deploymentSourceFile = new File(deploymentSourceArchive);
			if (!deploymentSourceFile.exists()) {
				log.warn("There is no deployment archive file with the name '" + deploymentName
						+ "'. Using existing Import package if possible.");

				// No additional steps needed, because no renaming would be
				// possible
				return false;
			}

			// Set archive file to rename the file later
			this.deploymentData.setArchive(deploymentSourceArchive);

		} else {
			if (deploymentSourceArchive.indexOf("*") > 0) {
				log.debug("Archivename contains '*' ... will look for complete filename now");
				String dir = "./";

				Paths paths = new Paths();
				log.debug("First character of deployment Archive check: " + deploymentSourceArchive.substring(0, 1));
				log.debug("System pathSeparator is: " + File.separator);
				log.debug("Checking ArchiveName path adressing for relative or absolut.");

				if (deploymentSourceArchive.substring(0, 1).equals(File.separator)) {
					log.debug("Changed to absolute path adressing, because archive starts with slash.");
					dir = "/";
				} else {
					log.debug("PathSeparator is not equal to first char of deploymentArchive. So ... ");
					log.debug("Using relative adressing with: " + dir);
				}

				paths.glob(dir, deploymentSourceArchive);
				log.debug(
						"Looked for files in directory: " + dir + " (Check if relative or absolute adressing is used");
				log.debug("Found " + paths.count() + " file(s) matching [" + deploymentSourceArchive + "]");
				for (File file : paths.getFiles()) {
					try {
						deploymentSourceArchive = file.getCanonicalPath();
						log.debug("File:" + file.getCanonicalPath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						log.error("Something went wrong looking for files.");
						log.error("To be debugged.");
						e.printStackTrace();
						System.exit(2);
					}
				}
				log.debug("Will use deploymentarchive now: " + deploymentSourceArchive);
			}

			// Using archive as configured
			deploymentSourceFile = new File(deploymentSourceArchive);
			this.deploymentData.setDeploymentSrcArchiveName(deploymentSourceArchive);
			if (!deploymentSourceFile.exists()) {
				log.warn("The deployment '" + deploymentSourceArchive + "' does not exist.");
				log.warn("Will try to deploy anyhow. See what Cognos portal can do for us.");
				deploymentTargetArchive = deploymentSourceArchive;

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
				// Determine the correct name of the original archive inside the
				// ZIP
				// file (i. e. after manual
				// renaming) with parsing the zip archive (if deployment is not
				// encrypted)
				DeploymentArchive zipArchiv = new DeploymentArchive(deploymentSourceArchive, deploymentPassword);
				this.deploymentData.setDeploymentNameOfArchiveInZipDeploymentFile(zipArchiv.determineOriginName());
				if (this.deploymentData.getDeploymentNameOfArchiveInZipDeploymentFile().length() == 0) {
					log.warn("Archive name could not be determined. Using \"" + deploymentName + "\" as fallback.");
				}
			}
		}

		// String for DateTime as Suffix or Prefix
		String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime());

		// ----
		// sanity Check on deployment folders and copy file to the folder
		// ----
		if (deploymentFolder != null && deploymentFolder.length() > 0) {
			// Check if deploymentFolder ends with "/" (trailing slash)
			if (deploymentFolder.endsWith("/") || deploymentFolder.endsWith("\\")) {
				log.debug("DeploymentFolder config option has trailing slash. ok.");
			} else {
				log.debug("Deploymentfolder is missing trailing slash. Added " + File.separatorChar);
				deploymentFolder += File.separatorChar;
			}

			// Add Timestamp to deploymenttargtArchive
			deploymentTargetArchive = deploymentFolder + "_autodepl_" + datetime + "_" + deploymentSourceFile.getName();

			File deploymentTargetFile = new File(deploymentTargetArchive);

			// Check if the target file already exists
			if (deploymentTargetFile.exists()) {
				log.warn("The file '" + deploymentTargetFile.getName()
						+ "' already exists in the deployment folder. Renaming the file.");
				if (!deploymentTargetFile.canWrite()) {
					// No write permissions, rename wouldn't be possible
					log.error("No write permissions for '" + deploymentTargetFile.getName() + "'.");
					return false;
				}

				String tmpTargetName = deploymentTargetArchive + "-" + datetime;
				File tmpTargetFile = new File(tmpTargetName);
				boolean targetSuccess = deploymentTargetFile.renameTo(tmpTargetFile);
				if (!targetSuccess) {
					log.error("Renaming of the existing archive failed");
					return false;
				} else {
					log.info("File was renamed to: " + tmpTargetFile);
				}
			}

			// Copy archive to deployment folder (with correct name)
			log.debug("Copy " + deploymentSourceArchive + " --> " + deploymentTargetArchive);
			this.deploymentData.setDeploymentSrcArchiveName(deploymentSourceArchive);
			this.deploymentData.setDeploymentTargetArchiveName(deploymentTargetArchive);

			FileInputStream fiss = null;
			FileOutputStream fisd = null;
			long transfered = 0;
			try {
				try {
					fiss = new FileInputStream(deploymentSourceArchive);
					FileChannel sourceChannel = fiss.getChannel();

					fisd = new FileOutputStream(deploymentTargetArchive);
					FileChannel destChannel = fisd.getChannel();

					transfered = destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
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
				log.debug("Will try to continue anyhow. This maybe a reimport backup job.");
				log.debug(e.getMessage());
				return true;
			}

			if (transfered > 0) {
				log.debug("Copy process successful.");
				return true;
			} else {
				log.error("Copy process failed. Transfered bytes = 0. Check target permissions, check network.");
				return false;
			}
		} else {
			log.debug("No sanity check on target folder and archive done."); // end
																				// of
																				// deployFolder
																				// sanity
																				// check
			deploymentTargetArchive = deploymentSourceFile.getName();
			log.debug("Will use " + deploymentTargetArchive
					+ " presuming it exists. Cognos will check this. Let's see later.");
		}
		return true;
	}// end of function

	private BaseClass[] addArchive(String deploySpec, String nameOfArchive) {

		String importName = nameOfArchive + "_import";
		ImportDeployment importDeploy = null;
		BaseClass[] addedDeploymentObjects = null;
		BaseClass[] bca = new BaseClass[1];
		AddOptions addOpts = null;

		SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject("/adminFolder");

		MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
		MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
		MultilingualToken myMultilingualToken = new MultilingualToken();

		myMultilingualToken.setLocale(strLocale);
		myMultilingualToken.setValue(importName);
		multilingualTokenArr[0] = myMultilingualToken;
		multilingualTokenProperty.setValue(multilingualTokenArr);

		importDeploy = new ImportDeployment();
		importDeploy.setName(multilingualTokenProperty);

		// deploymentOptions
		DeploymentOptionArrayProp doap = new DeploymentOptionArrayProp();
		String password = this.deploymentData.getPassword();
		if (password != null && password.length() >= 1) {
			DeploymentOption[] opt = new DeploymentOption[1];
			opt = (DeploymentOption[]) preparePasswordOptionForPackage(password);

			log.debug("Getting deploymentOptions for: " + deploymentTargetArchive.replaceAll(".zip", ""));
			opt = (DeploymentOption[]) getDeployedOption(deploymentTargetArchive.replaceAll(".zip", ""), opt);
			log.debug("Got deploymentoptions using deployment password.");
			DeploymentOption optNew[] = new DeploymentOption[opt.length + 1];
			log.debug("Looping over deploymentOptions ");
			for (int i = 0; i < opt.length; i++) {
				optNew[i] = opt[i];
			}

			log.debug("Adding password as options to new option-set.");
			log.debug(opt[0].getName());
			optNew[opt.length] = deploymentData.getArchiveEncryptPassword();
			opt = optNew;
			deploymentData.setDeploymentOptions(opt);

			doap.setValue(opt);
//			importDeploy.setDeploymentOptions(doap);

		} else {
			log.debug("No password for archiv provided. Skipped password section.");
			// doap.setValue(importName);
		}

		// AddOptions
		addOpts = new AddOptions();
		addOpts.setUpdateAction(UpdateActionEnum.replace);

		bca[0] = importDeploy;

		// setdescription
		// MultilingualString[] myDescription = null;
		// myDescription[0].setValue("AMVARA CONSULTING");
		// MultilingualStringProp myDescriptionStringProp = null;
		// myDescriptionStringProp.setValue( myDescription );
		// importDeploy.setDescription(myDescriptionStringProp);

		log.debug("baseClass and options have been prepared. Will call cmService.add() new.");
		ContentManagerService_PortType cmService = this.c8Access.getCmService();
		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath, bca, addOpts);
			log.debug("Adding done();");
		} catch (RemoteException remoEx) {
			String msg = "An error occurred when adding a deployment object. See cogserver.log for details.";
			log.error(msg);
			System.out.println("An error occurred when adding a deployment object:" + "\n" + remoEx.getMessage());
		}

		if ((addedDeploymentObjects != null) && (addedDeploymentObjects.length > 0)) {
			return addedDeploymentObjects;
		} else {
			return null;
		}
	} // addArchive

	private Object preparePasswordOptionForPackage(String password) {
		log.debug("Found password in deployment section of configuration file.");
		log.debug("Will add encryption password to deploymentOptions");
		DeploymentOption[] opt = new DeploymentOption[1];
		DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();
		archiveEncryptPassword.setValue("<credential><password>" + password + "</password></credential>");
		deploymentData.setArchiveEncryptPassword(archiveEncryptPassword);
		archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
		opt[0] = archiveEncryptPassword;
		return opt;
	}

	public Option[] getDeployedOption(String myArchive) {
		Option[] deployOptEnum = new Option[] {};

		cmService = this.c8Access.getCmService();

		try {
			deployOptEnum = cmService.getDeploymentOptions(myArchive, new Option[] {});
		} catch (RemoteException e) {
			log.error("!! Severe !! An error occurred in getting Deployment options." + "\n" + "The error: "
					+ e.getMessage());
			e.getCause();
			e.getStackTrace();

			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR,
					"Deployment Options not found! Archive not found is possibl reason. Check filesystem");
			log.error(
					"Deployment Options not found! Archive not found is possible reason. Check filesystem or run on other dispatcher?");
			log.error("No further execution possible.");
			log.error("System.exit code=2.");
			System.exit(2);
		}

		return deployOptEnum;
	} // getDeployedOption

	public Option[] getDeployedOption(String myArchive, Option[] p_opt) {
		Option[] deployOptEnum = new Option[] {};

		cmService = this.c8Access.getCmService();

		try {
			deployOptEnum = cmService.getDeploymentOptions(myArchive, p_opt);
		} catch (RemoteException e) {
			log.error("!! Severe !! An error occurred in getting Deployment options." + "\n" + "The error: "
					+ e.getMessage());
			e.getCause();
			e.getStackTrace();
			e.printStackTrace();

			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR,
					"Deployment Options not found! Archive not found or no access is possible reason. Check filesystem.");
			log.error(
					"Deployment Options not found! Archive not found or no access is possible reason. Check filesystem, check file access or run on other dispatcher?");
			log.error("No further execution possible.");
			log.error("System.exit code=2.");
			System.exit(2);
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
	public String deployContent(String strNewImportName, String strDeployedArchive)
			throws CoCoMaC8Exception, RemoteException, InterruptedException {

		log.debug("Create deployment in Contentstore now ... ");
		log.debug("------------------------------------------");

		// put the names of ZIP file and so on in a meaningfull place
		log.debug("NewImportName: " + strNewImportName);
		log.debug("DeployedArchive: " + strDeployedArchive);
		log.debug("------------------------------------------");
		deploymentData.setDeploymentTargetCognosPortalArchiveName(strNewImportName);
		deploymentData.setDeploymentTargetArchiveName(strDeployedArchive);

		AsynchReply asynchReply = null;
		String reportEventID = "false";
		String deployType = "import";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		log.debug("adding Archive: " + strNewImportName);
		BaseClass[] ArchiveInfo = addArchive(deployType, strNewImportName);
		log.debug("done");

		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
			log.info("Import Job prepared at: " + deployPath);
			deploymentData.setDeploymentSearchpath(deployPath);
		} else {
			return reportEventID;
		}
		
		//
		log.debug("Finally getting deployment options and finetuning.");
		Option[] myDeploymentOptionsEnum = null;
		if (deploymentData.getPassword() != null) {
			myDeploymentOptionsEnum = getDeployedOption(strDeployedArchive, deploymentData.getDeploymentOptions());
		} else {
			myDeploymentOptionsEnum = getDeployedOption(strDeployedArchive);
		}
		if (myDeploymentOptionsEnum == null) {
			reportEventID = "false";
			return reportEventID;
		}

		// Loop over DeploymentOptionsEnum
		log.debug("Deployment-Options:");
		log.debug("----");
		for (int i = 0; i < myDeploymentOptionsEnum.length; i++) {
			Option oname = myDeploymentOptionsEnum[i];
			DeploymentOption onameS = (DeploymentOption) oname;
			String optionName = onameS.getName().getValue();
			String OptionClassName = oname.getClass().getName();
			log.debug(i + ".) Deployment Option: " + optionName + " (" + OptionClassName.substring(53) + ")");

			if (DEPLOY_OPTION_NAME == OptionClassName) {
				DeploymentObjectInformation[] packDeployInfo = ((DeploymentOptionObjectInformationArray) myDeploymentOptionsEnum[i])
						.getValue();

				// Loop over DeploymentObjectInformation
				for (int j = 0; j < packDeployInfo.length; j++) {
					String packFolderName = packDeployInfo[j].getDefaultName();
					SearchPathSingleObject packagePath = packDeployInfo[j].getSearchPath();
					log.debug("FolderName: " + packFolderName + " PackagePath: " + packagePath);
				}

				// Option_MultilingualString
			} else if (DEPLOY_OPTION_MLSTRING == OptionClassName) {
				// Loop over DeploymentObjectInformation
				log.debug("Loop over deploymentOption MultilingualString");
				// TODO Fix this code
				// getName and getValue are available here

				// for (int j = 0; j < packDeployOptionStrings.length; j++) {
				// String packDeployStringsValue =
				// packDeployOptionStrings[j].getValue();
				// if (packDeployStringsValue.length() == 0) {
				// packDeployStringsValue = "<not set>";
				// }
				// if (optionName == "deploymentScreenTip"
				// || optionName == "deploymentDescription") {
				// packDeployOptionStrings[j]
				// .setValue("This is an autodeployment entry. - AMVARA
				// CONSULTING -");
				// ((DeploymentOptionMultilingualString)
				// myDeploymentOptionsEnum[i])
				// .setValue(packDeployOptionStrings);
				// log.debug("Setting " + optionName);
				// }
				// String packDeployStringsClass = packDeployOptionStrings[j]
				// .getClass().toString();
				// log.debug(optionName + ": " + packDeployStringsValue + " ["
				// + packDeployStringsClass + "]");
				// }

				// Option_string
			} else if (DEPLOY_OPTION_STRING == OptionClassName) {
				String deployOptionString = ((DeploymentOptionString) myDeploymentOptionsEnum[i]).getValue();
				log.debug(optionName + ": " + deployOptionString);
				if (optionName == "archive") {
					((DeploymentOptionString) myDeploymentOptionsEnum[i]).setValue(strDeployedArchive);
					log.debug("Archive name to be imported from filesystem set to: " + strDeployedArchive);
				}

			} else {
				// log.debug("ClassDebug Information : Not implemented yet.");
			}
		}
		log.debug("----");

		// Set the password as Option for the archive?
		if (deploymentData.getPassword() != null && deploymentData.getPassword().length() > 0) {
			log.debug("Found password in configuration ... will add this as option to deployment import job.");
			myDeploymentOptionsEnum = addOption(myDeploymentOptionsEnum, deploymentData.getArchiveEncryptPassword());
		}

		// Put the options to the BC
		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ImportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

		// Update the archive specs on the server
		try {
			log.info("Executing deployment ... pls. be patient ");
			cmService.update(ArchiveInfo, new UpdateOptions());

		} catch (RemoteException remoteEx) {
			log.error("RemoteException caught");
			reportEventID = "remoteException";
		}

		// Check for deployment to finish
		if (deploymentData.getDisplayHistoryAfterDeployment()) {
			log.debug("Executing Monitoring for deployment ");
			/**
			 * OLD IMPORT CODE
			 * monitorService = c8Access.getMonitorService(false, this.c8Access.getUrl());
			 * asynchReply = monitorService.run(searchPathObject, new ParameterValue[] {}, new Option[] {});
			 */
			
			java.net.URL serverURL = null;
			try {
				serverURL = new java.net.URL(c8Access.getUrl());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        try {
				c8Access.getMonitorServiceLocator().getmonitorService(serverURL);
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        asynchReply = c8Access.getMonitorService().run(searchPathObject, new ParameterValue[] {}, new Option[] {});
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
		} else {
			log.debug("Will not display deployment history, because option was configured 'false'.");
			log.debug(
					"E.g. for FullBackup-reimports it makes no sense to look for a history. Because the history will be overwritten with the very same deployment.");
		}
		reportEventID = "true";
		log.debug("Checking if there are dispatchers to removed...");
		removeUnnecessaryDispatchers();
		return reportEventID;
	} // deployContent

	private Option[] addOption(Option[] ori_opts, DeploymentOptionString new_opts) {
		// TODO Auto-generated method stub
		Option[] newOpt = new Option[ori_opts.length + 1];
		for (int i = 0; i < ori_opts.length; i++) {
			newOpt[i] = ori_opts[i];
		}
		newOpt[ori_opts.length] = new_opts;
		ori_opts = newOpt;
		return ori_opts;
	}

	/**
	 * displayImportHistory
	 * 
	 * @parameters String name, String impDeploymentName
	 */
	public void displayImportHistory(DeploymentData deploymentData) {

		// public void displayImportHistory(String name, String
		// impDeploymentName) {
		PropEnum props[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.deployedObjectStatus,
				PropEnum.objectClass, PropEnum.status, PropEnum.hasMessage, PropEnum.deployedObjectClass,
				PropEnum.message, PropEnum.detail, PropEnum.actualExecutionTime, PropEnum.actualCompletionTime };

		String impPath = deploymentData.getDeploymentSearchpath() + "//history//*";
		;
		// String impPath = "/adminFolder/importDeployment[@name='"
		// + (impDeploymentName.length() > 0 ? impDeploymentName : name)
		// + "']" + "//history//*";

		log.info("History for: " + impPath);
		String msg = "Import started on " + Calendar.getInstance().getTime().toString() + ": " + "Importing \""
				+ deploymentData.getDeploymentSearchpath() + "\"";
		log.info(msg);

		SearchPathMultipleObject spMulti = new SearchPathMultipleObject(impPath);

		BaseClass bc[] = null;
		try {
			bc = cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (bc != null && bc.length > 0) {
			for (int i = 0; i < bc.length; i++) {
				if (bc[i].getObjectClass().getValue().toString().equalsIgnoreCase("deploymentDetail")) {
					DeploymentDetail dd = (DeploymentDetail) bc[i];
					// Print messages if any
					FaultDetailArrayProp faultArray = dd.getMessage();
					FaultDetail[] faultDetail = faultArray.getValue();
					if (faultDetail != null) {
						for (int j = 0; j < faultDetail.length; j++) {
							FaultDetailMessage[] faultMessage = faultDetail[j].getMessage();
							log.info("Message: " + faultMessage[0].getMessage());
						}
					}
				}
			}
		} else {
			log.debug("Strange ... no response received quering the history of the deployment.");
			log.debug("Check it yourself. And maybe investigate for reason.");
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

		if (this.deploymentData.get_nameSetDateTimeSuffix().equalsIgnoreCase("true")) {
			String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime());
			deploymentName = deploymentName + "_" + datetime;
			log.debug("Changed deployment name to : " + deploymentName);
		} else {
			log.debug("No suffix configured in config file. Deployment name remains unchanged.");
		}

		String deploymentPassword = this.deploymentData.getPassword();
		String deploymentArchive = this.deploymentData.getArchive();
		this.deploymentData.setDeploymentNameOfArchiveInZipDeploymentFile(deploymentArchive);

		log.debug("Deploymentname : " + deploymentName);
		log.debug("Deploymentarchive Incoming : " + deploymentArchive);

		log.info("Prepare Deploymentarchive");
		if (prepareDeploymentArchive(deploymentName, deploymentPassword, deploymentArchive)) {
			/* Build name for Cognos Deployment Archive without ZIP */

			String cognosDeploymentArchive = new File(deploymentTargetArchive).getName().toString();
			cognosDeploymentArchive = cognosDeploymentArchive.replace(".zip", "");

			log.debug("Deploymentarchive at Cognos: " + cognosDeploymentArchive);
			log.info("Deploying the prepared archive now.");
			String strNewImportName = "_autodeployment_" + deploymentName + "_"
					+ this.deploymentData.getDeploymentNameOfArchiveInZipDeploymentFile();

			/* Deploy content now */
			String returnresult_deployContent = "";
			try {
				returnresult_deployContent = this.deployContent(strNewImportName, cognosDeploymentArchive);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.debug("return value: " + returnresult_deployContent);
			displayImportHistory(deploymentData);

		} else {
			log.error("prepareDeploymentArchive not successfull.");
			log.error("Deployment execution not possible. Pls. check CoCoMa Logfile for details.");
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR,
					"prepareDeploymentArchive failed. Deployment not executed. Pls. check CoCoMa Logfile for details. ");
		}
	}
	
	/**
	 * Removes wrong port dispatchers imported via DEV1 import
	 */
	private void removeUnnecessaryDispatchers() {
		
		String port = c8Access.getUrl().split(":")[2].split("/")[0];
		
		// get all dispatchers
		BaseClass[] dispatchers = c8Utility.findObjectsInSearchPath("/configuration/dispatcher");
		// convert array to arraylist for easy array manipulation
		List<BaseClass> disp = new ArrayList<BaseClass>(Arrays.asList(dispatchers));
		// remove all the Object that contain the correct port.
		disp.removeIf(d -> d.getDefaultName().getValue().contains(":" + port));
		// convert back the arraylist to BaseClass array.
		disp.toArray(dispatchers);
		
		if(disp.size() > 0) {
			// print out the dispatchers that will be removed.
			for(BaseClass dispatcher : disp) {
				log.debug("Removing dispatcher \"" + dispatcher.getDefaultName().getValue() + "\", Reason: Wrong port.");
			}
			
			try {
				DeleteOptions delete_options = new DeleteOptions();
				delete_options.setRecursive(true);
				c8Access.getCmService().delete(dispatchers, delete_options);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			log.debug("No wrong dispatchers found... will keep all the dispatchers.");
		}
	}
}
