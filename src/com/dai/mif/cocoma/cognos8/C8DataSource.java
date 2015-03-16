/**
 * $Id: C8DataSource.java 153 2010-06-22 08:36:36Z rroeber $
 */
package com.dai.mif.cocoma.cognos8;

import java.rmi.RemoteException;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DataSource;
import com.cognos.developer.schemas.bibus._3.DataSourceCommandBlock;
import com.cognos.developer.schemas.bibus._3.DataSourceCommandBlockProp;
import com.cognos.developer.schemas.bibus._3.DataSourceConnection;
import com.cognos.developer.schemas.bibus._3.DataSourceSignon;
import com.cognos.developer.schemas.bibus._3.Group;
import com.cognos.developer.schemas.bibus._3.NmtokenProp;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.dai.mif.cocoma.cognos.util.C8Access;
import com.dai.mif.cocoma.config.DataSourceData;
import com.dai.mif.cocoma.exception.CoCoMaC8Exception;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class encapsulates all data needed to create, delete or edit a
 * datasource in cognos8
 *
 * @author riedelc (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 153 $ ($Date:: 2010-06-22 10:36:36 +0200#$)
 */
public class C8DataSource {

	private DataSourceData dataSourceData;
	private C8Access c8Access;
	private Logger log;

	/**
	 * Constructor for the C8DataSource class. It takes two arguments: a data
	 * object, holding all necessary information about the dataSource and a
	 * C8Access object that provides access to the c8 system that the program is
	 * currently connected to.
	 *
	 * @param dsData
	 *            Data container for the DataSource
	 * @param c8ccess
	 *            C8Access object making the current connection available to
	 *            this class.
	 */
	public C8DataSource(DataSourceData dsData, C8Access c8Access) {
		this.log = Logging.getInstance().getLog(this.getClass());

		this.dataSourceData = dsData;
		this.c8Access = c8Access;
	}

	/**
	 * This method performs the steps needed to actually create the dataSource
	 * in the C8System.
	 *
	 * @param asynchronous
	 *            Boolean flag defining whether the data source shall be
	 *            configured for asynchronous (TRUE) or synchronous (FALSE)
	 *            access mode.
	 *
	 * @throws CoCoMaC8Exception
	 *             This exception is thrown, whenever a problem with the c8
	 *             operations occurs.
	 */
	public void create(boolean asynchronous) throws CoCoMaC8Exception {

		log.info("Creating data source " + dataSourceData.getName());

		// {0}: DSN, {1}: User, {2}: Password, {3}: asynchronous
		String connectionStringPattern = "^User ID:^?Password:;LOCAL;D2;DSN={0};UID=%s;PWD=%s;{0}@ASYNC={3}@0/0@COLSEQ=";

		String connectionString = MessageFormat.format(connectionStringPattern,
				this.dataSourceData.getDBAlias(),
				this.dataSourceData.getUserName(),
				this.dataSourceData.getPassword(), (asynchronous ? 1 : 0));

		log.debug("Connection string: " + connectionString);

		ContentManagerService_PortType cmService = this.c8Access.getCmService();

		// --- the dataSource itself

		DataSource dataSource = new DataSource();
		TokenProp tp = new TokenProp();
		tp.setValue(this.dataSourceData.getName());
		dataSource.setDefaultName(tp);

		// --- the dataSourceConnection

		DataSourceConnection dataSourceConnection = new DataSourceConnection();
		dataSourceConnection.setDefaultName(tp);
		StringProp strProp = new StringProp();

		strProp.setValue(connectionString);
		dataSourceConnection.setConnectionString(strProp);

		NmtokenProp isoLevel = new NmtokenProp();
		isoLevel.setValue(this.dataSourceData.getIsolationLevel());
		dataSourceConnection.setIsolationLevel(isoLevel);

		// --- connection commands

		// open connection commands

		DataSourceCommandBlockProp openConnectionCommands = null;
		String openConnectionCommandsValue = this.dataSourceData
				.getOpenConnectionCommands();
		if (openConnectionCommandsValue.length() > 0) {
			openConnectionCommands = new DataSourceCommandBlockProp();
			DataSourceCommandBlock commandBlock = new DataSourceCommandBlock();
			XmlEncodedXML commandBlockValue = new XmlEncodedXML(
					"<commandBlock><commands><sqlCommand><sql>"
							+ openConnectionCommandsValue
							+ "</sql></sqlCommand></commands></commandBlock>");
			commandBlock.set_value(commandBlockValue.toString());
			openConnectionCommands.setValue(commandBlock);
			log.debug("Setting open connection commands to "
					+ openConnectionCommandsValue);
		} else {
			log.debug("No open connection commands configured");
		}
		dataSourceConnection.setOpenConnectionCommands(openConnectionCommands);

		// close connection commands

		DataSourceCommandBlockProp closeConnectionCommands = null;
		String closeConnectionCommandsValue = this.dataSourceData
				.getCloseConnectionCommands();
		if (closeConnectionCommandsValue.length() > 0) {
			closeConnectionCommands = new DataSourceCommandBlockProp();
			DataSourceCommandBlock commandBlock = new DataSourceCommandBlock();
			XmlEncodedXML commandBlockValue = new XmlEncodedXML(
					"<commandBlock><commands><sqlCommand><sql>"
							+ closeConnectionCommandsValue
							+ "</sql></sqlCommand></commands></commandBlock>");
			commandBlock.set_value(commandBlockValue.toString());
			closeConnectionCommands.setValue(commandBlock);
			log.debug("Setting close connection commands to "
					+ closeConnectionCommandsValue);
		} else {
			log.debug("No close connection commands configured");
		}
		dataSourceConnection
				.setCloseConnectionCommands(closeConnectionCommands);

		// open session commands

		DataSourceCommandBlockProp openSessionCommands = null;
		String openSessionCommandsValue = this.dataSourceData
				.getOpenSessionCommands();
		if (openSessionCommandsValue.length() > 0) {
			openSessionCommands = new DataSourceCommandBlockProp();
			DataSourceCommandBlock commandBlock = new DataSourceCommandBlock();
			XmlEncodedXML commandBlockValue = new XmlEncodedXML(
					"<commandBlock><commands><sqlCommand><sql>"
							+ openSessionCommandsValue
							+ "</sql></sqlCommand></commands></commandBlock>");
			commandBlock.set_value(commandBlockValue.toString());
			openSessionCommands.setValue(commandBlock);
			log.debug("Setting open session commands to "
					+ openSessionCommandsValue);
		} else {
			log.debug("No open session commands configured");
		}
		dataSourceConnection.setOpenSessionCommands(openSessionCommands);

		// close session commands

		DataSourceCommandBlockProp closeSessionCommands = null;
		String closeSessionCommandsValue = this.dataSourceData
				.getCloseSessionCommands();
		if (closeSessionCommandsValue.length() > 0) {
			closeSessionCommands = new DataSourceCommandBlockProp();
			DataSourceCommandBlock commandBlock = new DataSourceCommandBlock();
			XmlEncodedXML commandBlockValue = new XmlEncodedXML(
					"<commandBlock><commands><sqlCommand><sql>"
							+ closeSessionCommandsValue
							+ "</sql></sqlCommand></commands></commandBlock>");
			commandBlock.set_value(commandBlockValue.toString());
			closeSessionCommands.setValue(commandBlock);
			log.debug("Setting close session commands to "
					+ closeSessionCommandsValue);
		} else {
			log.debug("No close session commands configured");
		}
		dataSourceConnection.setCloseSessionCommands(closeSessionCommands);

		// --- the credentials for the connection

		String dataSourceCredentials = buildDataSourceCredentials(
				dataSourceData.getUserName(), dataSourceData.getPassword());
		AnyTypeProp credProp = new AnyTypeProp();
		credProp.setValue(dataSourceCredentials);

		DataSourceSignon dataSourceSignon = new DataSourceSignon();
		dataSourceSignon.setCredentials(credProp);
		dataSourceSignon.setDefaultName(tp);

		log.debug("DataSource " + dataSourceData.getName() + " using user '"
				+ dataSourceData.getUserName() + "' for signon");

		AddOptions addOptions = new AddOptions();
		addOptions.setUpdateAction(UpdateActionEnum.replace);
		addOptions.setReturnProperties(new PropEnum[] { PropEnum.searchPath,
				PropEnum.defaultName });

		Group everyOneGroup = findCognosGroup("Everyone");
		if (everyOneGroup != null) {
			log.debug("Adding group "
					+ everyOneGroup.getSearchPath().getValue()
					+ " as consumer for dataSource (signon).");
			BaseClassArrayProp bcap = new BaseClassArrayProp();
			bcap.setValue(new BaseClass[] { everyOneGroup });

			dataSourceSignon.setConsumers(bcap);
		}

		try {

			// Create the data source

			BaseClass[] addedDataSources = cmService.add(
					new SearchPathSingleObject("CAMID(\":\")"),
					new BaseClass[] { dataSource }, addOptions);

			for (BaseClass addedDataSource : addedDataSources) {
				log.debug("Created dataSource "
						+ addedDataSource.getDefaultName().getValue() + " at "
						+ addedDataSource.getSearchPath().getValue());

				// Create the data source connection for the data source that
				// has just been added

				BaseClass[] addedDataSourceConnections = cmService.add(
						new SearchPathSingleObject(addedDataSource
								.getSearchPath().getValue()),
						new BaseClass[] { dataSourceConnection }, addOptions);

				for (BaseClass addedDataSourceConnection : addedDataSourceConnections) {
					log.debug("Created dataSourceConnection "
							+ addedDataSourceConnection.getDefaultName()
									.getValue()
							+ " at "
							+ addedDataSourceConnection.getSearchPath()
									.getValue());

					// create the signon for the connection

					BaseClass[] addedDataSourceSignons = cmService.add(
							new SearchPathSingleObject(
									addedDataSourceConnection.getSearchPath()
											.getValue()),
							new BaseClass[] { dataSourceSignon }, addOptions);

					for (BaseClass addedDataSourceSignon : addedDataSourceSignons) {
						log.debug("Created dataSourceSignon "
								+ addedDataSourceSignon.getDefaultName()
										.getValue()
								+ " at "
								+ addedDataSourceSignon.getSearchPath()
										.getValue());
					}

				}

			}

			// TODO add a test, if the created dataSource actually works

		} catch (RemoteException e) {

			throw new CoCoMaC8Exception("Error creating the dataSource", e);

		}
	}

	/**
	 * Encode the dataSource credentials to XML and return the properly encoded
	 * string.
	 *
	 * @param username
	 *            The username to be encoded
	 * @param password
	 *            The password to be encoded
	 * @return A string holding all the credential data in properly encoded XML
	 *         Format
	 */
	private String buildDataSourceCredentials(String username, String password) {
		StringBuffer cred = new StringBuffer();
		cred.append("<credential>");
		cred.append("<username>").append(username).append("</username>");
		cred.append("<password>").append(password).append("</password>");
		cred.append("</credential>");

		XmlEncodedXML credentials = new XmlEncodedXML(cred.toString());
		return credentials.get_value();
	}

	/**
	 *
	 * @param groupName
	 * @return
	 * @throws CoCoMaC8Exception
	 */
	private Group findCognosGroup(String groupName) throws CoCoMaC8Exception {

		Group resultGroup = null;

		log.debug("Searching group " + groupName);

		String queryPath = "CAMID(\":\")//group[@name=\"" + groupName + "\"]";

		ContentManagerService_PortType cmService = this.c8Access.getCmService();

		PropEnum[] props = new PropEnum[] { PropEnum.defaultName,
				PropEnum.searchPath };
		Sort[] sort = new Sort[] {};
		QueryOptions queryOptions = new QueryOptions();

		try {
			BaseClass[] groups = cmService.query(new SearchPathMultipleObject(
					queryPath), props, sort, queryOptions);

			if (groups.length == 0) {
				log.debug("Group not found");
				throw new CoCoMaC8Exception("No group found with the name "
						+ groupName);

			} else if (groups.length > 1) {
				log.debug("Group name is ambiguous, result returned "
						+ groups.length + " elements.");
				throw new CoCoMaC8Exception("Group name " + groupName
						+ " is ambiguous. Returned " + groups.length
						+ " results.");
			} else {
				resultGroup = (Group) groups[0];
				log.debug("Found group "
						+ resultGroup.getSearchPath().getValue());
			}
		} catch (RemoteException e) {
			throw new CoCoMaC8Exception("Error searching group: "
					+ e.getMessage());
		}

		return resultGroup;
	}

}
