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
			<name>IBM_Cognos_Arbeitslisten</name>
			<name_set_datetime_suffix>
				true
			</name_set_datetime_suffix>
			<password>Ad63BGTKYKwoMqGMW3xGYw==</password>
			<prefix_archive_after_deployent>yyyyMMdd_HHmmss</prefix_archive_after_deployent>
			<recordingLevel>
				<!-- basic -->
				full
			</recordingLevel>
			<archive>/cluster/mif/cognos/deployment/!DEPLENV!/MIFReporting_revision_*.zip</archive>
			<delete_items>
				<item>/content/package[@name='MIF_Adapter']</item>
				<item>/content/package[@name='MIF_AS']</item>
				<item>/content/package[@name='MIF_CFO']</item>
				<item>/content/package[@name='MIF_CONS']</item>
				<item>/content/package[@name='MIF_DERO']</item>
				<item>/content/package[@name='MIF_IMI']</item>
				<item>/content/package[@name='MIF_MNTR']</item>
				<item>/content/package[@name='MIF_Schatten']</item>
			</delete_items>
			<runCureJar>false</runCureJar>
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
	
		</deployment>
	</deployments>
	
</CoCoMa>
