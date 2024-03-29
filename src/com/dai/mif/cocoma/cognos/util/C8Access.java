/**
 * $Id: C8Access.java 163 2010-10-12 09:16:13Z rroeber $
 */

package com.dai.mif.cocoma.cognos.util;

import java.rmi.RemoteException;

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.log4j.Logger;

import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerServiceStub;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.Dispatcher_PortType;
import com.cognos.developer.schemas.bibus._3.Dispatcher_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.EventManagementService_PortType;
import com.cognos.developer.schemas.bibus._3.EventManagementService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.RoutingInfo;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class provides basic access functionality for a cognos8 system. It
 * prepares the connection, encodes the credentials in the proper way and offers
 * means to get a ContentManagerService instance to start the actual work.
 *
 * @author riedchr
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since 02.02.2010
 * @version $Revision: 163 $ ($Date:: 2010-10-12 11:16:13 +0200#$)
 */
public class C8Access {

	private Logger log;

	private boolean isConnected;

	private String url;
	private String username;
	private String namespace;
	private String password;

	private ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
	private ContentManagerServiceStub cmService;

	private ReportService_ServiceLocator reportServiceLocator = new ReportService_ServiceLocator();
	private ReportService_PortType repService;

	private Dispatcher_ServiceLocator dispatcherServiceLocator = new Dispatcher_ServiceLocator();
	private Dispatcher_PortType dispatcherService = null;

	EventManagementService_ServiceLocator eventManagerServiceLocator = null;
	EventManagementService_PortType eventManagerService = null;

	public Dispatcher_PortType getDispatcherService() {
		return dispatcherService;
	}

	public void setDispatcherService(Dispatcher_PortType dispatcherService) {
		this.dispatcherService = dispatcherService;
	}

	public Dispatcher_ServiceLocator getDispatcherServiceLocator() {
		return dispatcherServiceLocator;
	}

	public void setDispatcherServiceLocator(Dispatcher_ServiceLocator dispatcherServiceLocator) {
		this.dispatcherServiceLocator = dispatcherServiceLocator;
	}

	private C8Utility c8Utiliy;

	private MonitorService_ServiceLocator monitorServiceLocator;

	public MonitorService_ServiceLocator getMonitorServiceLocator() {
		return monitorServiceLocator;
	}

	public void setMonitorServiceLocator(MonitorService_ServiceLocator monitorServiceLocator) {
		this.monitorServiceLocator = monitorServiceLocator;
	}

	private MonitorService_PortType monitorService;

	/**
	 * Constructor for this class. It initializes all attributes needed to build a
	 * connection to the cognos8 content store.
	 *
	 * @param url       The URL for the dispatcher of the cognos8 system
	 * @param namespace The namespace to be used during authentication
	 * @param username  The username to connect with
	 * @param password  The password to be used
	 */
	public C8Access(String url, String namespace, String username, String password) {
		this.log = Logging.getInstance().getLog(this.getClass());
		this.log.debug("C8Access starts");
		this.setUrl(url);
		this.namespace = namespace;
		this.username = username;
		this.password = password;

		this.cmServiceLocator = new ContentManagerService_ServiceLocator();
		this.monitorServiceLocator = new MonitorService_ServiceLocator();
		this.eventManagerServiceLocator = new EventManagementService_ServiceLocator();

		this.c8Utiliy = new C8Utility(this);
	}

	public ContentManagerService_ServiceLocator getCmServiceLocator() {
		return cmServiceLocator;
	}

	public void setCmServiceLocator(ContentManagerService_ServiceLocator cmServiceLocator) {
		this.cmServiceLocator = cmServiceLocator;
	}

	public boolean hasSecondaryRequest(AsynchReply response, String secondaryRequest) {
		AsynchSecondaryRequest[] secondaryRequests = response.getSecondaryRequests();
		for (int i = 0; i < secondaryRequests.length; i++) {

			if (secondaryRequests[i].getName().compareTo(secondaryRequest) == 0) {
				return true;
			}
		}
		System.out.println(secondaryRequest + " is false");
		return false;
	}

	public AsynchReply getAsyncReply(AsynchReply asReply) {

		int maxWaitRetries = 0;
		int cntWaitRetries = 0;
		int sleepTimeMs = 10000;

		try {
			log.debug("Entering while-loop to wait for conversation status ...");
			while (!(asReply.getStatus().equals(AsynchReplyStatusEnum.complete))
					&& !(asReply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) {

				if (hasSecondaryRequest(asReply, "wait")) {
					log.debug("Waiting for converstation to finish.");

					asReply = this.getMonitorService().wait(asReply.getPrimaryRequest(), new ParameterValue[] {},
							new Option[] {});
					log.debug("asReply received ... ");
					log.debug("Status: " + asReply.getStatus());
				} else {
					log.error("Error: Wait method not available as expected.");
				}
			}
			return asReply;
		} catch (Exception e1) {
			log.debug("!!! Received reportError while waiting for AsyncReply");
			log.debug(e1);
			log.debug("Will ask server again ... just a moment ... ");
			while (cntWaitRetries < maxWaitRetries && !(asReply.getStatus().equals(AsynchReplyStatusEnum.complete))
					&& !(asReply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) {
				if (hasSecondaryRequest(asReply, "wait")) {
					log.debug("Waiting for converstation to finish.");
					try {
						asReply = this.getMonitorService().wait(asReply.getPrimaryRequest(), new ParameterValue[] {},
								new Option[] {});
					} catch (RemoteException e) {
						cntWaitRetries++;
						e.printStackTrace();
						log.debug("!!! Received reportError while waiting for AsyncReply. Will keep trying.");
						log.debug("Counter: " + cntWaitRetries + " -> " + maxWaitRetries);
						log.debug(e1);
						try {
							log.debug("Sleeping " + sleepTimeMs + " milliseconds.");
							Thread.sleep(sleepTimeMs);
						} catch (InterruptedException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
				} else {
					log.error("Error: Wait method not available as expected.");
				}
			}
			return asReply;
		}
	}

	public boolean ConnectToCognosServer(String serverURL) throws Exception {

		log = Logger.getLogger(this.getClass());
		log.debug("--> ConnectToCognosServer10()");
		log.debug("Server: " + serverURL);
		try {
			cmService = new ContentManagerServiceStub(new java.net.URL(serverURL), cmServiceLocator);
			dispatcherService = dispatcherServiceLocator.getdispatcher(new java.net.URL(serverURL));
			monitorService = monitorServiceLocator.getmonitorService(new java.net.URL(serverURL));
			eventManagerService = eventManagerServiceLocator.geteventManagementService(new java.net.URL(serverURL));

			String timeoutValueConfig = "0";
			int timeoutValue = Integer.parseInt(timeoutValueConfig);

			// Set the Axis request timeout
			// in milliseconds, 0 turns the timeout off
			((Stub) cmService).setTimeout(timeoutValue);
			((Stub) monitorService).setTimeout(timeoutValue);
			((Stub) eventManagerService).setTimeout(timeoutValue);

		} catch (Exception e) {
			log.error(e);
		}
		log.debug("** cmService connect done ");

		repService = reportServiceLocator.getreportService(new java.net.URL(serverURL));
		log.debug("** repService connect done");

		//
		// Logon if UserID and namepsaceID is filled
		//

		if (!this.namespace.equals("") && !this.username.equals("")) {
			log.debug("namespace or userID not empty ... will logon now.");
			log.debug("namespace: " + this.namespace);
			log.debug("userID: " + this.username);
			StringBuffer credentialXML = new StringBuffer();
			credentialXML.append("<credential>");
			credentialXML.append("<namespace>");
			credentialXML.append(this.namespace);
			credentialXML.append("</namespace>");
			credentialXML.append("<username>");
			credentialXML.append(this.username);
			credentialXML.append("</username>");
			credentialXML.append("<password>");
			credentialXML.append(this.password);
			credentialXML.append("</password>");
			credentialXML.append("</credential>");
			try {
				cmService.logon(new XmlEncodedXML(credentialXML.toString()), null);
			} catch (RemoteException e) {
				log.error("Login to Cognos failed - exception follows:\n\n" + e);
				log.error(e.detail);
				log.error("No further execution possible - see cogserver.log for error details!");
				log.error("Check user credentials as first step.");
				System.exit(-1);
			}
			log.debug("** logon done ");
		}

		log.debug("** setBiBusHeader ");
		SetBiBusHeader();
		SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/",
				"biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader) temp
				.getValueAsType(new QName("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"));
		((Stub) monitorService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
		((Stub) dispatcherService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader",
				cmBiBusHeader);
		((Stub) eventManagerService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader",
				cmBiBusHeader);
		((Stub) repService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
		log.debug("** setBiBusHeader done ");
		log.debug("<-- ConnectToCognosServer()");
		this.isConnected = true;

		return true;
	}

	/**
	 * initReportService() setzt den BiBusHeader passend zum reportService benoetigt
	 * "import com.cognos.org.apache.axis.client.Stub"
	 */
	public void SetBiBusHeader() {
		this.log.debug(" --> ConnectinitReportService() ");

		String BiBus_NS = "http://developer.cognos.com/schemas/bibus/3/";
		String BiBus_H = "biBusHeader";

		BiBusHeader CMbibus = null;

		SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(BiBus_NS, BiBus_H);

		try {
			CMbibus = (BiBusHeader) temp.getValueAsType(new QName(BiBus_NS, BiBus_H));
		} catch (Exception e) {
			this.log.error(" !!! Found BiBus Header Exception:" + e.getMessage());
			this.log.error("Will setHeader to cmService now");
		}

		if (CMbibus != null) {
			((Stub) cmService).setHeader(BiBus_NS, BiBus_H, CMbibus);
			log.debug("Header was set.");
		}

		this.log.debug(" <-- initReportService() ");
	}

	/**
	 * Disconnect from the server
	 */
	public void disconnect() {
		try {
			if (!this.isConnected) {
				cmService.logoff();
				log.debug("Disconected from Cognos server.");
			}
		} catch (RemoteException e) {
			String msg = "Error disconnecting from server: " + e.getMessage();
			CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
			log.error(msg);
		}
		this.isConnected = false;
	}

	/**
	 * Encode the credentials to XML
	 *
	 * @param namespace The namespace to be encoded
	 * @param username  The username to be encoded
	 * @param password  The password to be encoded
	 * @return An XmlEncodedXML object holding all the credential data
	 */
	private XmlEncodedXML buildCredentialsXML(String namespace, String username, String password) {
		StringBuffer cred = new StringBuffer();
		cred.append("<credential>");
		cred.append("<namespace>").append(namespace).append("</namespace>");
		cred.append("<username>").append(username).append("</username>");
		cred.append("<password>").append(password).append("</password>");
		cred.append("</credential>");

		XmlEncodedXML credentials = new XmlEncodedXML(cred.toString());
		return credentials;
	}

	/**
	 * This getter returns an instance to the ContentManagerService_Port that
	 * provides the interface to actually access the Cognos8 system. The connection
	 * has to be established before, preferably via the connect() mtehod of this
	 * class.
	 *
	 * @see #connect()
	 *
	 * @return An instance of the CognosManagerService_Port or NULL if the
	 *         connection was not etsablished before.
	 */
	public ContentManagerService_PortType getCmService() {
		return this.cmService;
	}

	public static BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader, boolean isNewConversation,
			String RSGroup) {
		if (SourceHeader == null)
			return null;

		BiBusHeader bibus = null;
		try {
			bibus = (BiBusHeader) SourceHeader
					.getValueAsType(new QName("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"));

			// If the header will be used for a new conversation, clear
			// tracking information, and set routing if supplied (clear if not)
			if (isNewConversation) {

				bibus.setTracking(null);

				// If a Routing Server Group is specified, direct requests to it
				if (RSGroup.length() > 0) {
					RoutingInfo routing = new RoutingInfo(RSGroup);
					bibus.setRouting(routing);
				} else {
					bibus.setRouting(null);
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		return bibus;
	}

	/**
	 * Getter for the MonitorService
	 *
	 * @return The currently set monitor service instance
	 */
	public MonitorService_PortType getMonitorService(boolean isNewConversation, String RSGroup) {
		BiBusHeader bibus = null;
		bibus = getHeaderObject(
				((Stub) monitorService).getHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"),
				isNewConversation, RSGroup);

		if (bibus == null) {
			BiBusHeader CMbibus = null;
			CMbibus = getHeaderObject(
					((Stub) cmService).getHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), true,
					RSGroup);

			((Stub) monitorService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", CMbibus);
		} else {
			((Stub) monitorService).clearHeaders();
			((Stub) monitorService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", bibus);

		}

		return this.monitorService;
	}

	/**
	 * Getter to check whether a connection is open via this instance
	 *
	 * @return TRUE if a connection is open, FALSE if no connection
	 */
	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * Getter for the {@link C8Utility} instance that holds convenience methods to
	 * work on the content store.
	 *
	 * @return Instance of the {@link C8Utility}
	 */
	public C8Utility getC8Utility() {
		return this.c8Utiliy;
	}

	/**
	 *
	 * @return
	 */
	public XmlEncodedXML getCurrentCredentialsXML() {
		XmlEncodedXML creds = buildCredentialsXML(this.namespace, this.username, this.password);
		return creds;
	}

	/**
	 * Return the name of the security name space to be used for this connection
	 *
	 * @return The namespace as it is configured in the configuration.
	 */
	public String getNameSpaceName() {
		return this.namespace;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	public ReportService_PortType getRepService() {
		return repService;
	}

	public void setRepService(ReportService_PortType repService) {
		this.repService = repService;
	}

	public MonitorService_PortType getMonitorService() {
		return monitorService;
	}

	public void setMonitorService(MonitorService_PortType monitorService) {
		this.monitorService = monitorService;
	}

	/**
	 * Returns the username used to connect to Cognos Server
	 * 
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	public EventManagementService_PortType getEventManagerService() {
		return eventManagerService;
	}

	public void setEventManagerService(EventManagementService_PortType eventManagerService) {
		this.eventManagerService = eventManagerService;
	}
}
