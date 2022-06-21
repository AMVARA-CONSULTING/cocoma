/**
 * $Id$
 */
package com.dai.mif.cocoma.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.logging.Logging;

/**
 * @author Stefan Brauner
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since 08.07.2014
 * @version $Revision$ ($Date:: YYYY-MM-DD hh:mm:ss #$)
 *
 */
public class DeploymentArchive {

    /**
     * Name of the archive
     */
    private ZipFile archive;

    /**
     * Encrypted password
     */
    private String password;

    private Logger log;

    public DeploymentArchive(String path, String password) {
        this.password = password;
        this.log = Logging.getInstance().getLog(this.getClass());

        try {
        	this.archive = new ZipFile(path);
        	
        } catch (IOException e) {
        	log.error("IO Exception. ZIP password protected? Check file: "+path);
            log.error(e.getMessage());
        }
    }

    public String determineOriginName() {
        String archiveName = "";

        /*
         * try { if (archive.isEncrypted()) { if (password.length() == 0) {
         * log.error("Missing password for deployment archive"); return
         * archiveName; } archive.setPassword(password); }
         *
         * log.debug("Reading " + this.path);
         * archive.extractFile("exportRecord.xml",
         * System.getProperty("java.io.tmpdir"));
         *
         * } catch (ZipException e1) { log.error(e1.getMessage()); }
         */

        boolean encrypted = false;
        log.debug("Will try determine original archive name contained in exportRecord.xml from deployment archive.");
        log.debug("ZIP-archive: "+archive.getName());
        
        

		if (password!=null && password.length()>0) {
			log.debug("Password for archive was set. Archive is encrypted. ");
			encrypted=true;
		}
		
        if(encrypted){
            log.debug(" .. checking PW");
            if(password == null){
                log.error("Empty password value, possibly an error while decrypting the password.");
            } else if(password.length() == 0){
                log.error("Missing deployment password");
                return archiveName;
            }

            log.warn("Deployment is encrypted. Decryption of the zip file content is not possible");
            return archiveName;
        } else {
	        // Unzip possible
	        unzipFunction(System.getProperty("java.io.tmpdir"), archive.getName());
			log.debug("Archive "+archive.getName()+" unzipped to directory: "+System.getProperty("java.io.tmpdir"));
	
	        // Determine the name of the deployment
	        String tmpXmlFileName = System.getProperty("java.io.tmpdir")+ File.separator + "exportRecord.xml";
	
	        File tmpXmlFile = new File(tmpXmlFileName);
	        if (!tmpXmlFile.exists()) {
	            log.error("Can't access unzipped files. Perhaps insufficient permissions for temporary directory?");
	            log.debug("tmpXmlFilePath: "+tmpXmlFile.getAbsolutePath());
	            log.debug("tmpXmlFileName: "+tmpXmlFile.getName());
	            return archiveName;
	        } else {
	        	log.debug("Will try reading "+tmpXmlFile.getAbsolutePath());
	        }
	
	        XMLConfiguration xmlConfig;
	        try {
	            xmlConfig = new XMLConfiguration(tmpXmlFileName);
	            xmlConfig.setBasePath("deploymentRecord");
	            archiveName = xmlConfig.getString("archive") + ".zip";
	            log.debug("Found archive name in ZIP file: "+archiveName);
	        } catch (ConfigurationException e) {
	            log.error(e.getMessage());
	        }
	        
	        
	        // Delete File
	        File tmp = new File(tmpXmlFileName);
	        log.debug("Removing tempfile "+tmpXmlFile.getAbsolutePath());
	        tmp.delete();
	        if (!tmpXmlFile.exists()) {
	        	log.debug("File has been removed.");
	        } else {
	        	log.error("Tempfile was not removed. Remove it yourself.");
	        }
        
        } // else if encrypted
	        
        return archiveName;
    }
    
    public void unzipFunction(String destinationFolder, String zipFile) {
    	File directory = new File(destinationFolder);
         
        // if the output directory doesn't exist, create it
        if(!directory.exists())
            directory.mkdirs();
 
        // buffer for read and write data to file
        byte[] buffer = new byte[2048];
         
        try {
            FileInputStream fInput = new FileInputStream(zipFile);
            ZipInputStream zipInput = new ZipInputStream(fInput);
             
            ZipEntry entry = zipInput.getNextEntry();
             
            while(entry != null){
                String entryName = entry.getName();
                File file = new File(destinationFolder + File.separator + entryName);
                 
                log.debug("Unzip file " + entryName + " to " + file.getAbsolutePath());
                if (entryName.equals("exportRecord.xml") ) { 
	                // create the directories of the zip directory
	                if(entry.isDirectory()) {
	                    File newDir = new File(file.getAbsolutePath());
	                    if(!newDir.exists()) {
	                        boolean success = newDir.mkdirs();
	                        if(success == false) {
	                            log.debug("Problem creating Folder");
	                        }
	                    }
	                }
	                else {
	                	// Create subdirectories of ZIP File
	                	new File(file.getParent()).mkdirs();
	                	
	                    FileOutputStream fOutput = new FileOutputStream(file);
	                    int count = 0;
	                    while ((count = zipInput.read(buffer)) > 0) {
	                        // write 'count' bytes to the file output stream
	                        fOutput.write(buffer, 0, count);
	                    }
	                    fOutput.close();
	                    log.debug("Wrote file: "+file.getAbsolutePath());
	                }
                } // endif File=exportRecord.xml
                else {
                	log.debug("... does not equal exportRecord.xml ... skipping ");
                }
                
	            // close ZipEntry and take the next one
                zipInput.closeEntry();
                entry = zipInput.getNextEntry();
            }
             
            // close the last ZipEntry
            zipInput.closeEntry();
             
            zipInput.close();
            fInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
