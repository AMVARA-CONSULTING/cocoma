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
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionAnyType;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.ExportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualString;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.dai.mif.cocoma.cognos.util.C8Access;
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
    
    
    public C8ExportDeployment(C8Access c8Access, BackupData backupData){
        this.cmService = c8Access.getCmService();
        this.mService = c8Access.getMonitorService(true,c8Access.getUrl());
        this.log = Logging.getInstance().getLog(this.getClass());

        this.name = backupData.getName();
        this.password = backupData.getPassword();
        this.use_DateTimeSuffix = backupData.getUseDateTimeSuffix();
    }

    public void createExport(){

    	// Setup name of JOB in Cognos to be executed as FullCSBackup
    	String expDeploymentName = this.name;

    	// Add datatime suffix to the JOB name?
    	if (this.use_DateTimeSuffix) {
	    	String datetime = new SimpleDateFormat("yyyyMMdd-HHmmss")
	                .format(Calendar.getInstance().getTime());
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

        SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject(
                "/adminFolder");

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
            addedDeploymentObjects = cmService.add(objOfSearchPath, bca,
                    addOpts);
        } catch (RemoteException remoEx) {
            log.error("An error occurred when adding a deployment object:"
                            + "\n" + remoEx.getMessage());
        }
        if ((addedDeploymentObjects != null)
                && (addedDeploymentObjects.length > 0)) {
            return addedDeploymentObjects;
        } else {
            return null;
        }
    }

    // setting options to export ENTIRE CS
    private Option[] setDeploymentOptionEnumCS(String deploymentType,
            String nameOfArchive, String exportPassword) {
        Option[] deploymentOptions = null;
        int num = 0;
        int eOptionCount = 0;

        log.debug("Setting options to export Content Store");
        String[] deployOptionEnumBoolean = { "entireContentStoreSelect", "archiveOverwrite", "personalDataSelect" };
        String[] deployOptionEnumResolution = { "archive",  "archiveEncryptPassword" };

        deploymentOptions = new DeploymentOption[eOptionCount  +
                            deployOptionEnumBoolean.length + deployOptionEnumResolution.length];

        deploymentOptions[num]   = this.setEntireContentStoreSelect(true); // choose entire CS
        deploymentOptions[++num] = this.setArchiveOverWrite(true); // overwrite
        deploymentOptions[++num] = this.setPersonalDataSelect(true); // default is false

        deploymentOptions[++num] = this.setDeploymentOptionString(nameOfArchive); // archive name
        deploymentOptions[++num] = this.setArchiveEncryptPassword(exportPassword);  //secure by password

        return deploymentOptions;
    }

    public String deployContentCS(String strArchiveName) {
        AsynchReply asynchReply = null;
        String reportEventID = "-1";

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
            asynchReply = mService.run(searchPathObject, new ParameterValue[] {}, new Option[] {});
        } catch (RemoteException remoteEx) {
            log.error("An error occurred while deploying content:"
                    + "\n" + remoteEx.getMessage());
            remoteEx.printStackTrace();
        }

        if (asynchReply != null) {
            reportEventID = "Success";
            log.debug("Content Store Backup was successful");
        } else {
            log.error("Severe Error! Content Store Backup failed.");
            log.error("Check your cogserver.log for more details.");
            System.exit(5);
        }

        return reportEventID;
    }

    // /This method logs the user to Cognos BI
    public String quickLogon(String namespace, String uid, String pwd) {
        StringBuffer credentialXML = new StringBuffer();

        credentialXML.append("<credential>");
        credentialXML.append("<namespace>").append(namespace)
                .append("</namespace>");
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
            SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(
                    "http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
            BiBusHeader cmBiBusHeader = (BiBusHeader) temp.getValueAsType(new QName(
                            "http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"));
            ((Stub) cmService).setHeader(
                    "http://developer.cognos.com/schemas/bibus/3/",
                    "biBusHeader", cmBiBusHeader);
            ((Stub) mService).setHeader(
                    "http://developer.cognos.com/schemas/bibus/3/",
                    "biBusHeader", cmBiBusHeader);
        } catch (Exception e) {
            System.out.println(e);
        }
        return ("Logon successful as " + uid);
    }// quickLogon

    public void connectToReportServer(String endPoint) { // This method connects to Cognos BI
        ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
        MonitorService_ServiceLocator mServiceLocator = new MonitorService_ServiceLocator();

        try {
            cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
            mService = mServiceLocator.getmonitorService(new java.net.URL(endPoint));

            // set the Axis request timeout
            ((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off
            ((Stub) mService).setTimeout(0);  // in milliseconds, 0 turns the timeout off
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
            archiveEncryptPassword.setValue("<credential><password>"
                    + pPassword + "</password></credential>");
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
