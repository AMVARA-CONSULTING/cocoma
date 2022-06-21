/**
 * $Id: DataSourceData.java 153 2010-06-22 08:36:36Z rroeber $
 */
package com.dai.mif.cocoma.config;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.crypt.Cryptography;
import com.dai.mif.cocoma.exception.ConfigException;
import com.dai.mif.cocoma.logging.Logging;

/**
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 3, 2010
 * @version $Revision: 153 $ ($Date:: 2010-06-22 10:36:36 +0200#$)
 */
public class DataSourceData {

	protected static final String RECORD_KEY_PREFIX = "dataSources.dataSource";

	private String recordKey;

	private String name;
	private String dbAlias;
	private String userName;
	private String password;
	private String isolationLevel;
	private String openConnectionCommands;
	private String closeConnectionCommands;
	private String openSessionCommands;
	private String closeSessionCommands;
	private String dbserver;
	private String dbname;
	private String dbport;
	private Boolean jdbc = false; // lets you create Connection String depending on if there is jdbc connection...
	
	private XMLConfiguration conf;

	private Logger log;

	private String my_recordKey_at;

	/**
     * @param conf
     * @param i
     */
    public DataSourceData(XMLConfiguration conf, int i)
            throws ConfigException {

		// Init logger
    	this.log = Logging.getInstance().getLog(this.getClass());

		this.conf = conf;

        this.recordKey = RECORD_KEY_PREFIX + "(" + i + ")";

        Cryptography crypt = Cryptography.getInstance();

        this.name = conf.getString(recordKey + ".name");
        log.debug("Reading: "+this.name);
        this.dbAlias = conf.getString(recordKey + ".dbalias");
        this.userName = conf.getString(recordKey + ".username");

        String cryptedPass = conf.getString(recordKey + ".password");
        try {
            this.password = crypt.decrypt(cryptedPass);
        } catch (Exception e) {
            if (CoCoMa.isInteractiveMode()) {
                this.password = "";
            }
        }

        this.isolationLevel = conf.getString(recordKey + ".isolationLevel");

        this.openConnectionCommands = this.getSessionCommands(recordKey
                + ".connectionCommands.openConnection");
        log.debug("openConnection Command:" + this.openConnectionCommands);

        this.closeConnectionCommands = this.getSessionCommands(recordKey
                + ".connectionCommands.closeConnection");
        log.debug("closeConnection Command:" + this.closeConnectionCommands);

        this.openSessionCommands = this.getSessionCommands(recordKey + ".connectionCommands.openSession");
        log.debug("OpenSession Command:" + this.openSessionCommands);
        
        this.closeSessionCommands = this.getSessionCommands(recordKey
                + ".connectionCommands.closeSession");
        log.debug("closeSession Command:" + this.closeSessionCommands);

        
        // Reading jdbc dbserver, dbname & dbport
        
    	this.dbserver = conf.getString(recordKey + ".jdbc.dbserver");
        this.dbname = conf.getString(recordKey + ".jdbc.dbname");
        this.dbport = conf.getString(recordKey + ".jdbc.dbport");

        log.debug("this.dbserver: " + this.dbserver);
        
        this.jdbc = (this.dbserver != null && this.dbname != null && this.dbport != null) ? true : false;

        if (this.name.length() == 0) {
            throw new ConfigException("DataSource name is empty.");
        }

        if (this.dbAlias.length() == 0) {
            throw new ConfigException("DataSource (" + this.name
                    + ") DB alias is empty.");
        }

        log.debug("looking at username");
        if (this.userName.length() == 0) {
            throw new ConfigException("DataSource (" + this.name
                    + ") username is empty.");
        }

        log.debug("looking at pw");
        if ((this.getPassword() == null || this.getPassword().isEmpty()) ) {
        	if ((!CoCoMa.isInteractiveMode())) {
	            throw new ConfigException("DataSource (" + this.name
	                    + ") password is empty or could not be decrypted. Check it now!");
            }
        }

        log.debug("looking at isolation level");
        if (this.isolationLevel.length() == 0) {
            throw new ConfigException("DataSource (" + this.name
                    + ") isolationLevel is empty.");
        } else if (!this.isolationLevel.equals("cursorStability")
                && !this.isolationLevel.equals("phantomProtection")
                && !this.isolationLevel.equals("readCommitted")
                && !this.isolationLevel.equals("readUncommitted")
                && !this.isolationLevel.equals("reproducibleRead")
                && !this.isolationLevel.equals("serializable")) {
            throw new ConfigException(
                    "DataSource isolationLevel may only be one of the following values: cursorStability , phantomProtection , readCommitted , readUncommitted , reproducibleRead, serializable");

        }

    }


    public String getSessionCommands(String recordKey_at) {
    	my_recordKey_at = recordKey_at;
    	String result="";
        log.debug("Investigating on :" + my_recordKey_at);
        @SuppressWarnings("unchecked")
		List<HierarchicalConfiguration> fields = conf.configurationsAt(my_recordKey_at);
        if (fields.size()>0) {
        	log.debug("found entries");
    		log.debug("Reading subentries at: " + my_recordKey_at);
    		@SuppressWarnings("unchecked")
			List<Object> sqlCommandList = conf.getList(my_recordKey_at);
    		// read over configured entries
    		for (int i1 = 0; i1 < sqlCommandList.size(); i1++) {
    			log.debug("Counter:"+ i1 );
    			String my_tempString = (String) sqlCommandList.get(i1);
    			if (my_tempString.length()>0) {
        			result += "<sqlCommand><sql>" + my_tempString + "</sql></sqlCommand>";
        			log.debug("String:"+ my_tempString);
    			}
    		}
    		// place the surrounding block
    		if (result.length()>0) result = 
					"<commandBlock><commands>"+
	    			result +
	    			"</commands></commandBlock>";
        } else {
        	log.debug("no Command found in configfile at "+my_recordKey_at);
        	log.debug("returning empty string");
        }
        return result;
    }
    
    /**
	 * @param password
	 * @throws ConfigException
	 */
	public void setPassword(String password) throws ConfigException {

		Cryptography crypt = Cryptography.getInstance();

		try {
			conf.setProperty(this.recordKey + ".password", crypt.encrypt(password));
			conf.save();
			conf.reload();
		} catch (ConfigurationException e) {
			throw new ConfigException("Error saving the password: " + e.getMessage());
		}

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the isolationLevel
	 */
	public String getIsolationLevel() {
		return isolationLevel;
	}

	/**
	 * @return the catalog
	 */
	public String getDBAlias() {
		return dbAlias;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the openConnectionCommands
	 */
	public String getOpenConnectionCommands() {
		return openConnectionCommands;
	}

	/**
	 * @return the closeConnectionCommands
	 */
	public String getCloseConnectionCommands() {
		return closeConnectionCommands;
	}

	/**
	 * @return the openSessionCommands
	 */
	public String getOpenSessionCommands() {
		return openSessionCommands;
	}

	/**
	 * @return the closeSessionCommands
	 */
	public String getCloseSessionCommands() {
		return closeSessionCommands;
	}
	
	/**
	 * @return the dbserver from jdbc
	 */
	public String getDBServer() {
		return dbserver;
	}
	
	/**
	 * @return the dbname from jdbc
	 */
	public String getDBName() {
		return dbname;
	}
	
	/**
	 * @return the dbport from jdbc
	 */
	public String getDBPort() {
		return dbport;
	}
	
	/**
	 * @return if there are values in jdbc
	 */
	public Boolean getJDBC() {
		return jdbc;
	}
}
