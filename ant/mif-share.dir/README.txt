$Id: README.txt 165 2015-05-07 10:13:28Z rroeber $

CoCoMa - Cognos Configuration Manager
-------------------------------------

This file describes the configuration and usage of the Cognos Configuration 
Manager CoCoMa.

CoCoMa configures a Cognos8 system based on a configuration given as XML file. 
The current version has been designed for an tested with IBM Cognos 8.4.1 and
Sun Java JRE/JDK 1.5.

As standard, the config file is assumed to be named CoCoMa.xml next to the 
program JAR. This can be overridden by specifying the command line argument
--config

An extra option --check allows for checking whether the config file is correct
syntactically.

Passwords, that are needed to access the dispatcher, and to set up the data 
sources within Cognos8 are encrypted in the config file. To add the encrypted
passwords to the config, the config file has to be prepared manually specifying 
all user names etc. The password fields can be left blank.

By specifying the option --setpass the program starts in interactive mode and
queries the passwords for the currently loaded config file via console input.
The values entered here are encrypted and added to the config file. 

Once the passwords have been added to the config file, the file can be used to
perform the actual configuration of the Cognos8 system.

By default CoCoMa will log all messages to a log file created in the current 
directory. However logging directly to console can be activated by adding the
option --console to the program call.

CoCoMa's work process can be divided into three different phases: a basic 
configuration phase, a deployment phase and a content configuration phase. Each 
of these three phases can be triggered by the command line arguments 
--phasebasic, --phasedeployment and --phasecontent respectively. If none of the
three options is specified, all three phases are performed successively. 

Other commandline argument:
--dispatcherinfo ... generates information about dispatchers found within the 
					 connected environment(s).

--dumpaccounts


As of Version 3.1: Cognos 10.2.1/2016-05-25_1825/153
--------------------------------------------------------


As of Version 2.9: Cognos 10.2.1/2016-05-25_1825/153
--------------------------------------------------------
- received ticket with issue description: 0031052397
- Version counting changed to automated build counting; 
	future version will not contain version string "2.8" anymore
	version string will be replace by automated version-counting 
- checked, and switched to java 1.8.72, also running under 1.8.74
- option "--setpass" now check if mailhost and backup is configured. will only ask for password, if configured.
- datasource option "<openSession>SET CURRENT SCHEMA=MIF_DEV</openSession>" may be repeated to include several sql statements in datasource
  e.g.
				<openSession>SET CURRENT SCHEMA=MIF_DEV</openSession>
				<openSession>set current optimization profile=MIF_DEV.QS101WFILTER_STMTKEY</openSession>
- development framework "eClipse" was changed to Version: Mars.1 Release (4.5.1), Build id: 20150924-1200
- smoketests against Test4 and localhost successfull

As of version 2.7 there are a couple of new features:
--------------------------------------------------------
- Fullcontentstore Backup reimport is now possible, secured archives can be openend with password by CoCoMa
- Wildcards in deployment archive filenames may contain absolute or relative foldernames. Archive is searched for on disc. If found archive name is logged to console. Deployment in Cognos will be prepared if found or not, as archive might also already be on the target application server machine. Carefully read warnings and error messages if in doubt.

As of version 1.9 the config file has to be extended:
-----------------------------------------------------

A new block is introduced:

	<restrictedContent>
		<unrestricted>
			<role>System Administrators</role>
		</unrestricted>
		<visible>
			<searchPath>/content/folder[@name='T2-0']</searchPath>
		</visible>
	</restrictedContent>


<unrestricted> lists the roles, groups and users that will have the "show hidden"
option set by default.

<visible> lists the folders and packages (top level in Cognos Connection) that
will be set to visible. All other entries on the top level will be hidden by
default.

Additionally a new capability definition has to be added to the <capabilities>
block. This defines that all listed roles, groups and users will not be able
to edit the "show hidden" option. I fact they wont even see the option:

		<!-- =========================== -->
		<!-- Remove 'Show Hidden' option -->
		<!-- =========================== -->

		<capability>
			<name>Hide Entries</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>


As of version 2.5 there are a couple of new features:
--------------------------------------------------------
- Option of deployment folder. This parameter will be used to store 
files for later deployment. This is a directory which resides within 
the active ContentManager directory structure, normally references as
<cognos-installation>/deployment.
The deploymentFolder has to be writeable by CoCoMa. A sanity check will
be done during program execution.
Use "net use" to view the complete information about mounted drives in
dos commandbox. You may not use the DOS-driveletters to access a mounted
device.

	<server>
		[...]
		<deploymentFolder>\\192.168.33.135\c$\Programme\ibm\cognos\c1021_server\deployment</deploymentFolder>
		[...]
	</server> 

- Mailserver Parameters to connect to a mailserver for sending resumes
about deployments executed.

	<mailserver>
        <username>amvara.roeber@extaccount.com</username>
        <host>mailhost</host>
        <port>25</port>
        <!-- Strategy: TLS  -->
        <strategy/>
		<password>hdb7fPCDpRl9uORM1zzYMw==</password>
	</mailserver>

- Backup Parameter describes the need information for executing a
full contentstore backup before any modification like dispatcher settings
or deployments.
The password used to secure the backup file stored encrypted in this config.
You cannot decrypt the password. Store the cleartext version in a save place
like keepass.

	<backup>
	   <name>FullBackup</name>
	   <enabled>true</enabled>
	   <password>DRTuZlLmF6Y4KLV78IPZZQ==</password>
	   <use_datetimesuffix>false</use_datetimesuffix>
	</backup>


--------------------------------------------------------
Complete example configuration file:
--------------------------------------------------------
<?xml version="1.0" encoding="UTF-8"?>
<CoCoMa>
<version>$Revision: 140 $</version>

	<server>
			<dispatcherURL>http://192.168.33.135:9300/p2pd/servlet/dispatch</dispatcherURL>
			<namespace>LDAP</namespace>
			<username>administrator</username>
			<password>jxWkxnSGloWpa2NAcfhFVw==</password>
			<version>10</version>
			<!-- ================================================================ -->
			<!-- Using Windows netshares with Java Programm requires  -->
			<!-- drive letters to be replace with remote information from command "net use" -->
			<!-- Y:\temp_rrr\deployment > net use -->
			<!-- Neue Verbindungen werden nicht gespeichert. -->
			<!-- Status       Lokal     Remote                    Netzwerk -->
			<!-- ================================================================ -->
			<!-- OK           Y:        \\192.168.33.135\c$       Microsoft Windows Network -->
			<!-- ================================================================ -->
			<!-- Y:\temp_rrr translates into \\192.168.33.135\c$\temp_rrr -->
			<deploymentFolder>\\192.168.33.135\c$\Programme\ibm\cognos\c1021_server\deployment</deploymentFolder>
	</server>
	
	<mailserver>
        <username>amvara.roeber@extaccount.com</username>
        <host>mailhost</host>
        <port>25</port>
        <!-- Strategy: TLS  -->
        <strategy/>
		<password>hdb7fPCDpRl9uORM1zzYMw==</password>
	</mailserver>
	
	<backup>
	   <name>FullBackup</name>
	   <enabled>true</enabled>
	   <password>DRTuZlLmF6Y4KLV78IPZZQ==</password>
	   <use_datetimesuffix>false</use_datetimesuffix>
	</backup>
	
	<deployments>

		<deployment>
			<name>IBM_Cognos_PowerCube_rrr_Testdeployment</name>
			<name_set_datetime_suffix>
				<!-- 
					Set Datetime String as Suffix on deployment name appearing in CognosConnection
				 	true = YYYYMMDD_HHmmss will be appended to name
				 	false = name as specified will be used, if exists older deployment with same name, it will be overwritten
				 -->
				true
			</name_set_datetime_suffix>
			<password>Ad63BGTKYKwoMqGMW3xGYw==</password>
			<prefix_archive_after_deployent>yyyyMMdd_HHmmss</prefix_archive_after_deployent>
			<recordingLevel>
				<!-- basic -->
				full
			</recordingLevel>
			<archive>deployment_incoming/IBM_Cognos_PowerCube.zip</archive>
			<delete_items>
				<item>/content/folder[@name='Samples_PowerCube']</item>
				<item>/content/folder[@name='Samples_PowerCube_Dummy']</item>
			</delete_items>
			<runCureJar>true</runCureJar>
			<runCureJar_searchPath>			
			<!-- reportSearchPath is a CognosSearchPath from Cognos Connection -->
			<!-- /content/folder[@name='Beispiele']//report | /content/folder[@name='Beispiele']//query -->
			<!-- ATTENTION!!! following searchpath may read thousands of reports!!!! --> 
			//query
			</runCureJar_searchPath>
			<runCureJar_PackagePath>		
				<!-- 
				reportPackagePath is a CognosSearchPath from Cognos 
				Connection for the Package that was upgraded 
				This element is optional.
				-->
			</runCureJar_PackagePath>		
	        <mail_sender>amvara.roeber@extaccount.com</mail_sender>
	        <mail_recipient>amvara.roeber@extaccount.com</mail_recipient>
	        <mail_subject>[MIF] Cognos Autodeployment {HOST} {STATUS}</mail_subject>
	        <mail_text>Deployment-Archive: {ARCHIVE}
	Status: {STATUS}
	Date: {DATE}
	Time: {TIME}
	---
	Cognos Autodeployment using COCOMA Version {VERSION} on Host {HOST}</mail_text>
	
		</deployment>

	</deployments>
	
	<curjar_searchpath>
		<!-- reportSearchPath is a CognosSearchPath from Cognos Connection -->
		//query
	</curjar_searchpath>
 
	<ui>

		<skins>
			<skin>
				<name>Daimler</name>
				<resourceLocation>Daimler</resourceLocation>
				<preview>../skins/Daimler/preview.htm</preview>
			</skin>
		</skins>
		
		<defaultProfile>
			<skin>Daimler</skin>
			<linesPerPage>999</linesPerPage>
			<listSeparator>background</listSeparator>
		</defaultProfile>

	</ui>

	<dataSources>
		<dataSource>
			<name>QSUSERDS</name>
			<dbalias>APP0</dbalias>
			<username>mifweb</username>
			<password>6Fl+jZoL3EMca/9adLMUCA==</password>
			<isolationLevel>readUncommitted</isolationLevel>
			<connectionCommands>
				<openConnection/>
				<closeConnection/>
				<openSession>SET CURRENT SCHEMA=MIF_DEV</openSession>
				<closeSession/>
			</connectionCommands>
		</dataSource>

		<dataSource>
			<name>FK1XAT03</name>
			<dbalias>APP0</dbalias>
			<username>mifweb</username>
			<password>6Fl+jZoL3EMca/9adLMUCA==</password>
			<isolationLevel>readUncommitted</isolationLevel>
			<connectionCommands>
				<openConnection/>
				<closeConnection/>
				<openSession>SET CURRENT SCHEMA=MIF_DEV</openSession>
				<closeSession/>
			</connectionCommands>
		</dataSource>

		<dataSource>
			<name>Audit</name>
			<dbalias>CRN0</dbalias>
			<username>AUD008A1</username>
			<password>ICv60/kgD/3gSpkXTeFs8Q==</password>
			<isolationLevel>readUncommitted</isolationLevel>
		</dataSource>
	</dataSources>

	<dispatchers>
		<dispatcher global="true">
			<name>http://rroeber-fwewbgp:9300/p2pd</name>
			<loadBalancingMode>weightedRoundRobin</loadBalancingMode>
			<governorLimit>50</governorLimit>
			<peakStartHour>0</peakStartHour>
			<peakEndHour>23</peakEndHour>
			<connectionsHighAffinity>1</connectionsHighAffinity>
			<connectionsLowAffinity>2</connectionsLowAffinity>
			<maxProcessesInteractiveReporting>10</maxProcessesInteractiveReporting>
			<queueTimeoutReporting>120</queueTimeoutReporting>
			<advancedSettings>
				<name>CM.AMVARA_TEST_FROM_CONFIG1</name>
				<value>AMVARA_XXX_FROM_CONFIG1\,SECOND_VALUE</value>
				<name>CM.AMVARA_TEST_FROM_CONFIG2</name>
				<value>AMVARA_XXX_FROM_CONFIG2</value>
				<name>CM.AMVARA_TEST_FROM_CONFIG3</name>
				<value>AMVARA_XXX_FROM_CONFIG3</value>
			</advancedSettings>
			<auditLevel>
				<contentManagerService>BASIC</contentManagerService>
				<reportDataService>REQUEST</reportDataService>
				<eventManagementService>BASIC</eventManagementService>
				<jobService>BASIC</jobService>
				<monitorService>BASIC</monitorService>
				<presentationService>BASIC</presentationService>
				<reportService>REQUEST</reportService>
				<reportServiceNativeQuery>TRUE</reportServiceNativeQuery>
				<systemService>BASIC</systemService>
				<dispatcher>BASIC</dispatcher>
			</auditLevel>

		</dispatcher>

	</dispatchers>

	<security>
		<roles>
			<role>
				<name>System Administrators</name>
				<keepExisting>true</keepExisting>
				<members>
					<user>apiuser</user>
					<user>rroeber</user>
				</members>
				<removeMembers>
					<group>Everyone</group>
				</removeMembers>
			</role>
			<role>
				<name>System Administrators (ReadOnly)</name>
				<members>
					<user>RROEBER</user>
				</members>
			</role>
            
			<role>
				<name>WorklistUser</name>
				<members>
					<user>test111</user>
					<user>test2111</user>
					<user>DE_15</user>
					<user>KESSLERA</user>
				</members>
			</role>

			<role>
				<name>QSWorklistUser</name>
				<members>
					<user>test111</user>
					<user>test2111</user>
					<user>DE_15</user>
					<user>KESSLERA</user>
				</members>
			</role>

		</roles>

		<permissions>

			<permission>
				<target>/content</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/folder[@name='MIF_QueryStudio']/folder[@name='MIF_QueryStudio new (3.1)']</target>
				<read>true</read>
				<write>true</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_Adapter"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_CFO"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_CONS"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_DERO"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_Drucklisten"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_MDR"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_MNTR"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>


			<permission>
				<target>/content/package[@name="MIF_Schatten"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>


			<permission>
				<target>/content/package[@name="SMB"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>WorklistUser</role>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_CAVLGL"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_CAVMGNT"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_CRI"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_ELI"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_ELIALL"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_EXTVAL"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_HIDDEN"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_OTHER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_RAVLGL"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_RAVMGNT"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_RAVSALES"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_SPR_LGL"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="MIF_QS3.1_SPR_MGNT"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>

		</permissions>
	</security>

	<capabilities>
		<capability>
			<name>Query Studio</name>
			<permission>
				<read>true</read>
				<write>true</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
				</members>
			</permission>
			<features>
				<feature>
					<name>Advanced</name>
					<permission>
						<read>true</read>
						<write>true</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>QSWorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Create</name>
					<permission>
						<read>true</read>
						<write>true</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>QSWorklistUser</role>
						</members>
					</permission>
				</feature>
			</features>
		</capability>

		<capability>
			<name>Report Studio</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
				<feature>
					<name>User Defined SQL</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>HTML Items in Report</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Bursting</name>
					<permission>
						<read>false</read>
						<write>false</write>
						<execute>false</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Create/Delete</name>
					<permission>
						<read>false</read>
						<write>false</write>
						<execute>false</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
			</features>
		</capability>
		
		<capability>
			<name>Cognos Viewer</name>
			<permission>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
				<feature>
					<name>Context Menu</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Selection</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Run With Options</name>
					<permission>
						<read>false</read>
						<write>false</write>
						<execute>false</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Toolbar</name>
					<permission>
						<read>false</read>
						<write>false</write>
						<execute>false</execute>
						<setPolicy>false</setPolicy>
						<traverse>false</traverse>
						<members>
							<role>QSWorklistUser</role>
							<role>WorklistUser</role>
						</members>
					</permission>
				</feature>
			</features>
		</capability>

		<capability>
			<name>Administration</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>

		<capability>
			<name>Administration</name>
			<permission>
				<read>true</read>
				<write>true</write>
				<execute>true</execute>
				<setPolicy>true</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>System Administrators</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Analysis Studio</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>

		<capability>
			<name>Event Studio</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Metric Studio</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Controller Studio</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Planning Contributor</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Specification Execution</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Scheduling</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Detailed Errors</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
		
		<capability>
			<name>Administration</name>
			<permission>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>true</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>System Administrators (ReadOnly)</role>
				</members>
			</permission>
			<features>
				<feature>
					<name>Adaptive Analytics Administration</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Administration tasks</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Configure and manage the system</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Controller Administration</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Data Source Connections</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Distribution Lists and Contacts</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Metric Studio Administration</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Planning Administration</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>PowerPlay Servers</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Printers</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Run activities and schedules</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Set capabilities and manage UI profiles</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Styles and portlets</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>
				<feature>
					<name>Users\, Groups\, and Roles</name>
					<permission>
						<read>true</read>
						<write>false</write>
						<execute>true</execute>
						<setPolicy>false</setPolicy>
						<traverse>true</traverse>
						<members>
							<role>System Administrators (ReadOnly)</role>
						</members>
					</permission>
				</feature>			
			</features>
		</capability>
		<capability>
			<name>Hide Entries</name>
			<permission>
				<read>false</read>
				<write>false</write>
				<execute>false</execute>
				<setPolicy>false</setPolicy>
				<traverse>false</traverse>
				<members>
					<role>QSWorklistUser</role>
					<role>WorklistUser</role>
				</members>
			</permission>
			<features>
			</features>
		</capability>
	</capabilities>

	<restrictedContent>
				<unrestricted>
					<role>System Administrators</role>
				</unrestricted>
				<visible>
					<searchPath>/content/folder[@name='MIF_QueryStudio']</searchPath>
				</visible>
	</restrictedContent>

</CoCoMa>
