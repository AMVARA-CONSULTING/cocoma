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

As of Version 3.1: Cognos 10.2.1/2019-01-24_1614/414
--------------------------------------------------------
- Cognos11 uses buildin Role "Tenant Administrators", which cannot be deleted.
  CoCoMa would encounter an error, when trying to delete this fixed object
- The logged in user must have language settings set to "en", otherwhise
  the matching configuration items from XML could not be found. Added function
  to set content and product locale to "en"
- changed the error output from debug to info + error at the end of CoCoMa run

As of Version 3.1: Cognos 10.2.1/2018-11-12_1653/382
--------------------------------------------------------
Modified code so Import and Export won't output error on SOAP Headers.

As of Version 3.1: Cognos 10.2.1/2018-11-07_1553/381
--------------------------------------------------------
added support for jdbc variables to xml config file, now we can add jdbc configuration content in xml configuration file, an example:
...
        ...
        <dataSources>
                <dataSource>
                        ...
                        <jdbc>
                                <dbserver>DBHOST</dbserver> <!-- this is the url to the jdbc server -->
                                <dbname>DBNAME</dbname> <!-- this is the database name, to which we want to connect -->
                                <dbport>DBPORT</dbport> <!-- this is the port to the jdbc server -->
                        </jdb>
                        ...
                </dataSource>
        </dataSources
        ...
...

As of Version 2.9: Cognos 10.2.1/2016-05-25_1825/153
--------------------------------------------------------
- received ticket with issue description: 0031052397
- Version counting changed to automated build counting; 
	future version will not contain version string "2.8" anymore
	version string will be replace by automated version-counting 
- checked, and switched to java 1.8.72, also running under 1.8.74
- option "--setpass" now check if mailhost and backup is configured. will only ask for password, if configured.
- datasource option "<openSession>SET CURRENT SCHEMA=MY_SCHEMA</openSession>" may be repeated to include several sql statements in datasource
  e.g.
				<openSession>SET CURRENT SCHEMA=MY_SCHEMA</openSession>
				<openSession>set current optimization profile=MY_SCHEMA.QS101WFILTER_STMTKEY</openSession>
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
					<role>USERROLE1</role>
					<role>USERROLE2</role>
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
		<deploymentFolder>DEPLOYMENTDIR</deploymentFolder>
		[...]
	</server> 

- Mailserver Parameters to connect to a mailserver for sending resumes
about deployments executed.

	<mailserver>
        <username>ralf.roeber@amvara.de</username>
        <host>mailhost</host>
        <port>25</port>
        <!-- Strategy: TLS  -->
        <strategy/>
		<password>PASSWORD==</password>
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
	   <password>PASSWORD==</password>
	   <use_datetimesuffix>false</use_datetimesuffix>
	</backup>


--------------------------------------------------------
Complete example configuration file:
--------------------------------------------------------
<?xml version="1.0" encoding="UTF-8"?>
<CoCoMa>
<version>$Revision: 140 $</version>

	<server>
			<dispatcherURL>http://SERVER:9080/p2pd/servlet/dispatch</dispatcherURL>
			<namespace>LDAP</namespace>
			<username>administrator</username>
			<password>PASSWORD==</password>
			<version>10</version>
			<!-- ================================================================ -->
			<!-- Using Windows netshares with Java Programm requires  -->
			<!-- drive letters to be replace with remote information from command "net use" -->
			<!-- Y:\temp_rrr\deployment > net use -->
			<!-- Neue Verbindungen werden nicht gespeichert. -->
			<!-- Status       Lokal     Remote                    Netzwerk -->
			<!-- ================================================================ -->
			<!-- OK           Y:        \\SAMBASHARE	      Microsoft Windows Network -->
			<!-- ================================================================ -->
			<!-- Y:\temp_rrr translates into \\SAMBASHARE\temp_rrr -->
			<deploymentFolder>\\SAMASHARE\temp_rrr</deploymentFolder>
	</server>
	
	<mailserver>
        <username>ralf.roeber@amvara.de</username>
        <host>mailhost</host>
        <port>25</port>
        <!-- Strategy: TLS  -->
        <strategy/>
		<password>hdb7fPCDpRl9uORM1zzYMw==</password>
	</mailserver>
	
	<backup>
	   <name>FullBackup</name>
	   <enabled>true</enabled>
	   <password>PASSWORD==</password>
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
			<password>PASSWORD==</password>
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
	        <mail_sender>ralf.roeber@amvara.de</mail_sender>
	        <mail_recipient>ralf.roeber@amvara.de</mail_recipient>
	        <mail_subject>[COCOMA] Cognos Autodeployment {HOST} {STATUS}</mail_subject>
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
				<name>Custom</name>
				<resourceLocation>Custom</resourceLocation>
				<preview>../skins/Custom/preview.htm</preview>
			</skin>
		</skins>
		
		<defaultProfile>
			<skin>Custom</skin>
			<linesPerPage>999</linesPerPage>
			<listSeparator>background</listSeparator>
		</defaultProfile>

	</ui>

	<dataSources>
		<dataSource>
			<name>NAME</name>
			<dbalias>ALIAS</dbalias>
			<username>USERNAME</username>
			<password>PASSWORD==</password>
			<isolationLevel>readUncommitted</isolationLevel>
			<connectionCommands>
				<openConnection/>
				<closeConnection/>
				<openSession>SET CURRENT SCHEMA=MY_SCHEMA</openSession>
				<closeSession/>
			</connectionCommands>
		</dataSource>

		<dataSource>
			<name>NAME2</name>
			<dbalias>ALIAS2</dbalias>
			<username>USERNAME</username>
			<password>PASSWORD==</password>
			<isolationLevel>readUncommitted</isolationLevel>
			<connectionCommands>
				<openConnection/>
				<closeConnection/>
				<openSession>SET CURRENT SCHEMA=MY_SCHEMA</openSession>
				<closeSession/>
			</connectionCommands>
		</dataSource>

		<dataSource>
			<name>NAME3</name>
			<dbalias>ALIAS3</dbalias>
			<username>USERNAME2</username>
			<password>PASSWORD2==</password>
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
				<name>USERROLE1</name>
				<members>
					<user>test111</user>
					<user>test2111</user>
					<user>DE_15</user>
				</members>
			</role>

			<role>
				<name>USERROLE2</name>
				<members>
					<user>test111</user>
					<user>test2111</user>
					<user>DE_15</user>
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
					<role>USERROLE1</role>
					<role>USERROLE2</role>
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
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>


			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>


			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE1</role>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
				</members>
			</permission>

			<permission>
				<target>/content/package[@name="DUMMY_FOLDER"]</target>
				<read>true</read>
				<write>false</write>
				<execute>true</execute>
				<setPolicy>false</setPolicy>
				<traverse>true</traverse>
				<members>
					<role>USERROLE2</role>
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
					<role>USERROLE2</role>
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
							<role>USERROLE2</role>
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
							<role>USERROLE2</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
							<role>USERROLE2</role>
							<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<role>USERROLE2</role>
					<role>USERROLE1</role>
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
					<searchPath>/content/folder[@name='DUMMY_FOLDER']</searchPath>
				</visible>
	</restrictedContent>

</CoCoMa>
