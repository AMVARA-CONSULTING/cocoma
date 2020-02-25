<?xml version="1.0" encoding="UTF-8"?><CoCoMa>
<version>$Revision: 139 $</version>
	<server>
			<dispatcherURL>!DEPLURL!</dispatcherURL>
			<namespace>LDAP</namespace>
			<username>MIF_CRNAPI_tec_00</username>
                	<password>ONcnuYTUhFNJqcwKa6INbQqFoKrX1q79UnGY9eWyseo=</password>
			<version>10</version>
			<deploymentFolder></deploymentFolder>

	</server>
	
	<deployments>
		<deployment>
			<name>IBM_Cognos_QUERYSTUDIO</name>
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
			<archive>/cluster/mif/cognos/deployment/!DEPLENV!/MIF_QueryStudio_3.1_All_*</archive>
			<!--
			<delete_items>
				<item>/content/folder[@name='Samples_PowerCube']</item>
				<item>/content/folder[@name='Samples_PowerCube_Dummy']</item>
			</delete_items>
			-->
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
<!--
	        <mail_sender>amvara.roeber@extaccount.com</mail_sender>
	        <mail_recipient>amvara.roeber@extaccount.com</mail_recipient>
	        <mail_subject>[MIF] Cognos Autodeployment {HOST} {STATUS}</mail_subject>
	        <mail_text>Deployment-Archive: {ARCHIVE}
	Status: {STATUS}
	Date: {DATE}
	Time: {TIME}
 	...	
	Cognos Autodeployment using COCOMA Version {VERSION} on Host {HOST}</mail_text>
-->
	
		</deployment>
	</deployments>
	
</CoCoMa>
