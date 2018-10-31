package com.dai.mif.cocoma.cognos.util;

import java.rmi.RemoteException;
import javax.xml.namespace.QName;

import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.*;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.AxisFault;

import org.apache.log4j.Logger;

public class c10access {

	private boolean isConnected = false;

	private Logger log;

	private String serverURL;
	private String namespaceID;
	private String userID;
	private String password;

	private ReportService_ServiceLocator reportServiceLocator = new ReportService_ServiceLocator();
	private ReportService_PortType repService;

	private ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
	public ContentManagerServiceStub cmService;

	public c10access(String url, String namespace, String username,
			String password) {
		log = Logger.getLogger(this.getClass());
		log.info("** C10Access");
		this.serverURL = url;
		this.namespaceID = namespace;
		this.userID = username;
		this.password = password;
	};

	public boolean ConnectToCognosServer(String serverURL) throws Exception {

		log = Logger.getLogger(this.getClass());
		log.info("--> ConnectToCognosServer()");
		log.info("Server: " + serverURL);
		try {
			cmService = new ContentManagerServiceStub(new java.net.URL(
					serverURL), cmServiceLocator);
			String timeoutValueConfig = "0";
			int timeoutValue = Integer.parseInt(timeoutValueConfig);

			// Set the Axis request timeout
			// in milliseconds, 0 turns the timeout off
			cmService.setTimeout(timeoutValue);

		} catch (Exception e) {
			log.error(e);
		}
		log.info("** cmService connect done ");

		reportServiceLocator = new ReportService_ServiceLocator();
		repService = reportServiceLocator.getreportService(new java.net.URL(
				serverURL));
		log.info("** repService connect done");

		//
		// Logon if UserID and namepsaceID is filled
		//
		if (!this.namespaceID.equals("") && !this.userID.equals("")) {
			log.info("namespace or userID not empty ... will logon now.");
			log.info("namespace: " + this.namespaceID);
			log.info("userID: " + this.userID);
			log.info("password: " + this.password);
			StringBuffer credentialXML = new StringBuffer();
			credentialXML.append("<credential>");
			credentialXML.append("<namespace>");
			credentialXML.append(this.namespaceID);
			credentialXML.append("</namespace>");
			credentialXML.append("<username>");
			credentialXML.append(this.userID);
			credentialXML.append("</username>");
			credentialXML.append("<password>");
			credentialXML.append(this.password);
			credentialXML.append("</password>");
			credentialXML.append("</credential>");
			try {
				cmService.logon(new XmlEncodedXML(credentialXML.toString()),
						null);
				// SOAPHeaderElement temp =
				// ((Stub)cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/",
				// "biBusHeader");
				// BiBusHeader cmBiBusHeader =
				// (BiBusHeader)temp.getValueAsType(new QName
				// ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
				// ((Stub)cmService).setHeader("http://developer.cognos.com/schemas/bibus/3/",
				// "biBusHeader", cmBiBusHeader);
			} catch (RemoteException e) {
				log.error("Login to Cognos failed - exception follows:\n\n" + e);
				log.error(e.detail);
				log.error("No further execution possible - see cogserver.log for error details!");
				System.exit(-1);
			}
			log.info("** logon done ");
		}

		log.info("** setBiBusHeader ");
		SetBiBusHeader();
		log.info("** setBiBusHeader done ");
		log.info("<-- ConnectToCognosServer()");
		this.isConnected = true;
		return true;
	}

	/**
	 * initReportService() setzt den BiBusHeader passend zum reportService
	 * benoetigt "import com.cognos.org.apache.axis.client.Stub"
	 */
	public void SetBiBusHeader() {
		this.log.debug(" --> ConnectinitReportService() ");
		this.log.debug("Class RepService:" + repService.getClass());

		String BiBus_NS = "http://developer.cognos.com/schemas/bibus/3/";
		String BiBus_H = "biBusHeader";

		BiBusHeader CMbibus = null;

		SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(BiBus_NS,
				BiBus_H);

		try {
			CMbibus = (BiBusHeader) temp.getValueAsType(new QName(BiBus_NS,
					BiBus_H));
		} catch (Exception e) {
			this.log.info("Getting BiBus Header Exception:" + e);
			this.log.info("Will setHeader to cmService now");
		}

		if (CMbibus != null) {
			((Stub) cmService).setHeader(BiBus_NS, BiBus_H, CMbibus);
			log.info("Header was set.");
		}

		this.log.debug(" <-- initReportService() ");
	}

	public void disconnect() {
		try {
			if (!this.isConnected) {
				log.debug("** now loging off cognosServer");
				cmService.logoff();
				log.debug("** logoff done");
			}
		} catch (RemoteException e) {
			log.error("** Error disconnecting from server: " + e.getMessage());
		}
		this.isConnected = false;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * @return
	 */
	public String parseAxisFault(AxisFault af) {
		String axisMessage = af.dumpToString();

		int start = axisMessage.indexOf("<messageString>");
		int end = axisMessage.indexOf("</messageString>");

		String message = axisMessage.substring(start + 15, end);

		return message;
	}
}
