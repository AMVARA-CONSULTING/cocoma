<?xml version="1.0" encoding="UTF-8"?>
<CoCoMa>
<log4jloglevel>INFO</log4jloglevel>
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
		<!-- 
			within the deployments tag, use deployment-tag to define one or more deployments.
		 -->
		<deployment>
			<!--   
			name: 	This defines the name of the import to be shown in Cognos Connection.
			-->
			<name>Reimport_FullBackup_before_after_autodeployment</name>
			<!-- 
			name_set_datetime_suffix:	true or false. The archive name may be suffixed by a timestamp
										in cognos connection. Settings this option to true will
										put a timestamp behinde the import archive name in
										cognos connection.
										This will not influence the archive name used on disc.
			-->
			<name_set_datetime_suffix>
				false
			</name_set_datetime_suffix>
			<!-- 
			password:	if the archive is crypted and secured via password, use this password
						to tell cognos when importing a secured deployment archive.
			-->
			<password>DRTuZlLmF6Y4KLV78IPZZQ==</password>
			<!-- 
			prefix_archive_after_deployment:	Use this option and CoCoMa will copy the imported
												arhive to <archive_name>+timestamp. In other
												words, CoCoMa adds the actual timestamp to the
												imported archive name.
			-->
			<prefix_archive_after_deployment>yyyyMMdd_HHmmss</prefix_archive_after_deployment>
			<!-- recordingLevel:	This is the recording level from Cognos Connection. 
									Could be basic as well. Use it  as you like. 
			-->
			<recordingLevel>
				full
			</recordingLevel>
			<!-- archive:			name of the archive to be read from disc.	
			-->
			<archive>/cluster/mif/cognos/deployment/!DEPLENV!/ContentStoreFullBackup.zip</archive>
			<!-- show_job_history_after_deployment:		true or false. No comment.  -->
			<show_job_history_after_deployment>true</show_job_history_after_deployment>
		</deployment>
	</deployments>

</CoCoMa>
