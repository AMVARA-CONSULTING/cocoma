/**
 * $Id: $
 */
package com.dai.mif.cocoma.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author Stefan Brauner (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: Stefan Brauner $
 *
 * @since Jul 25, 2014
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class MailserverData {

    private String username;
    private String password;
    private String host;
    private String port;
    private String strategy;

    private XMLConfiguration conf;
    private Logger log;
    private Session session;

    /**
     * @param conf
     */
    public MailserverData(XMLConfiguration conf) throws ConfigException {
        this.conf = conf;
        Cryptography crypt = Cryptography.getInstance();
        this.log = Logging.getInstance().getLog(this.getClass());

        log.debug("Parsing mailserver configuration");

        this.username = conf.getString("mailserver.username", null);
        this.host = conf.getString("mailserver.host", null);
        this.port = conf.getString("mailserver.port", "25");
        this.strategy = conf.getString("mailserver.strategy", null);

        try {
            String cryptedPass = conf.getString("mailserver.password", null);
            this.password = crypt.decrypt(cryptedPass);
        } catch (Exception e) {
            if (!CoCoMa.isInteractiveMode()) {
                throw new ConfigException(
                        "Error decrypting the mail password: " + e.getMessage(),
                        e);
            } else {
                this.password = null;
            }
        }

        if (this.username == null || this.host == null || this.port == null
                || this.password == null) {
            log.debug("Incomplete mail configuration. No mails will be sent.");
        } else {
            this.createMailSession();
        }
    }

    public void createMailSession() {
        log.debug("Creating mail session");
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", this.host);
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.port", this.port);
        if (this.strategy.compareToIgnoreCase("SSL") == 0) {
            props.setProperty("mail.smtp.socketFactory.port", this.port);
            props.setProperty("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        } else if (this.strategy.compareToIgnoreCase("TLS") == 0
                || this.strategy.compareToIgnoreCase("StartTLS") == 0) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        this.session = Session.getInstance(props, auth);
    }

    public void sendMail(String subject, String content, String sender,
            String recipient) {
        if (this.session != null) {
            try {
            	log.debug("Preparing message");
                Message message = new MimeMessage(this.session);

                message.addHeader("Content-type", "text/HTML; charset=UTF-8");

                message.setFrom(new InternetAddress(sender));
                message.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(recipient));
                message.setSubject(subject);
                message.setText(content);
                message.setSentDate(new Date());

                // Sending message
                Transport.send(message);
            	log.debug("Message sent.");
            } catch (AddressException e) {
                log.error("Error while converting sender or recipient mail adress.");
                log.debug(e.getMessage());
            } catch (MessagingException e) {
                log.error("Error while sending mail.");
                log.debug(e.getMessage());
            }
        } else {
        	log.debug("No mailserver session found. Not sending mails.");
        }
    }

    /**
     * @param password
     * @throws ConfigException
     */
    public void setPassword(String password) throws ConfigException {

        Cryptography crypt = Cryptography.getInstance();

        try {
            conf.setProperty("mailserver.password", crypt.encrypt(password));
            conf.save();
            conf.reload();
        } catch (ConfigurationException e) {
            throw new ConfigException("Error saving the password: "
                    + e.getMessage());
        }

    }

    /**
     * Getter for the username field
     *
     * @return the currently set value for username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Getter for the password field
     *
     * @return the currently set value for password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Getter for the host field
     *
     * @return the currently set value for host
     */
    public String getHost() {
        return host;
    }

    /**
     * Getter for the port field
     *
     * @return the currently set value for port
     */
    public String getPort() {
        return port;
    }

    /**
     * Getter for the strategy field
     *
     * @return the currently set value for strategy
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Fills the placeholders in mail subject and text Available placeholders
     * (surrounced by {}): HOST, STATUS, ARCHIVE, DATE, TIME, VERSION
     *
     * TODO: {OPTIONS} fehlt noch. nur was fuer options?
     * @return
     */
    public String fillPlaceholders(String text, DeploymentData deployment,
            String versionString) {
        text = text.replace("{ARCHIVE}", deployment.getArchive());
        text = text.replace("{DATE}", new SimpleDateFormat("yyyy-MM-dd")
                .format(Calendar.getInstance().getTime()));
        text = text.replace("{TIME}", new SimpleDateFormat("HH:mm:ss").format(Calendar
                .getInstance().getTime()));
        text = text.replace("{VERSION}", versionString);
        if (deployment.getStatus() == DeploymentData.DEPLOYMENT_STATUS_SUCCESS) {
            text = text.replace("{STATUS}", "Success");
        } else if (deployment.getStatus() == DeploymentData.DEPLOYMENT_STATUS_ERROR) {
            text = text.replace("{STATUS}", "Error");
        } else {
            text = text.replace("{STATUS}", "Unkown ("+deployment.getStatus()+")");
        }

        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            if(hostname == null || hostname.length() == 0){
                hostname = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
            hostname = "Unknown";
        }
        text = text.replace("{HOST}", hostname);

        return text;
    }

}
