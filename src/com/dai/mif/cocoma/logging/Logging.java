/**
 * $Id: Logging.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.logging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class provides basic logging functionality. It offers methods for
 * retrieving a log file for a class, set the log level, change the filename
 * used for the log files etc.
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since 02.02.2010
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 */
public class Logging {

	public static final Level DEBUG = Level.DEBUG;
	public static final Level INFO = Level.INFO;
	public static final Level WARN = Level.WARN;
	public static final Level ERROR = Level.ERROR;
	public static final Level FATAL = Level.FATAL;

	private Map<Class<?>, Logger> logMap;

	// private Level logLevel;

	private static Logging loggingInstance;
	private Logger log;

	private String logFilePrefix;
	private String logFileDir;

	/**
	 * Constructor for the Logging class. This constructor is private - the
	 * factory pattern method getInstance has to be used to get a Logging
	 * instance.
	 */
	private Logging() {
		Logging.loggingInstance = this;
		this.logMap = new HashMap<Class<?>, Logger>();
		this.log = getLog(this.getClass());
		this.logFileDir = ".";
	}

	/**
	 * Factory pattern method to get a logging instance. This method makes sure
	 * that exactly one Logging instance exists and that it is used by all
	 * classes for logging.
	 *
	 * @return Instance to the Logging object to be used for logging.
	 */
	public static Logging getInstance() {
		if (Logging.loggingInstance == null) {
			Logging.loggingInstance = new Logging();
		}
		return Logging.loggingInstance;
	}

	/**
	 * Method to get a Logger instance for a given class. This Logger instance
	 * offers all methods needed to issue any log message for different log
	 * levels. All Logger instances are held in a map so that it is made sure
	 * that there exists only one Logger instance for each class.
	 *
	 * @param clazz
	 *            The class that requests a Logger instance
	 *
	 * @return The Logger instance to be used by the given class
	 *
	 * @see {@link org.apache.log4j.Logger}
	 */
	public Logger getLog(Class<?> clazz) {
		Logger log;

		log = this.logMap.get(clazz);
		if (log == null) {
			log = Logger.getLogger(clazz);

			this.logMap.put(clazz, log);
		}

		return log;
	}

	// /**
	// * Set a new log level to be used by all currently active and all future
	// * Logger instances.
	// *
	// * @param level
	// * The new leg level to be used
	// *
	// * @see {@link org.apache.log4j.Level}
	// */
	// public void setLogLevel(Level level) {
	// this.logLevel = level;
	// for (Logger log : this.logMap.values()) {
	// log.setLevel(this.logLevel);
	// }
	// }

	// /**
	// * Getter for the logLevel property
	// *
	// * @return Reference to the currently set LogLevel object.
	// */
	// public Level getLogLevel() {
	// return this.logLevel;
	// }

	/**
	 * Set a new prefix for the log files.
	 *
	 * @param prefix
	 *            The new prefix to be used for logfiles
	 */
	public void setLogFilePrefix(String prefix) {
		this.logFilePrefix = prefix;
		applyLogFileData();
	}

	/**
	 * Set the new directory where the log files will be stored.
	 *
	 * @param dir
	 *            The new directory to be used for saving the log files
	 */
	public void setLogDir(String dir) {
		File logDir;

		this.logFileDir = dir;
		logDir = new File(dir);
		if (logDir.exists() && logDir.isDirectory()) {
			applyLogFileData();
		} else {
			this.logFileDir = System.getProperty("user.dir");
			applyLogFileData();
			this.log.warn("The log file folder \"" + dir
					+ "\" as specified in the INI file does not exist. "
					+ "Log files are stored in \"" + this.logFileDir
					+ "\" instead.");
		}
	}

	/**
	 * Convenience method for printing an Exception or Error stack trace
	 *
	 * @param clazz
	 *            The class for which the message should be logged
	 * @param error
	 *            The Throwable instance to be logged (Error or Exception)
	 */
	public static void logError(Class<?> clazz, Throwable error) {
		Logging logging;
		Logger log;

		if (Logging.loggingInstance != null) {
			logging = Logging.loggingInstance;

			// try to find the logger registered for the given class
			log = logging.logMap.get(clazz);
			if (log == null) {
				log = logging.getLog(clazz);
			}

			// print the stack trace elements
			for (StackTraceElement ste : error.getStackTrace()) {
				log.error(ste.toString());
			}
		}
	}

	/**
	 * Apply the log file data by setting the filenames for the logs and
	 * settings the log directory as defined in the attributes in this class.
	 * Any possibly previously created log files created under the old name and
	 * the old location are deleted if the logs are still empty.
	 *
	 * @see #setLogDir(String)
	 * @see #setLogFilePrefix(String)
	 */
	private void applyLogFileData() {
		File allRunsFile;
		File lastRunFile;
		FileAppender allRunsAppender;
		FileAppender lastRunAppender;
		Logger root;
		String allRunsLog;
		String lastRunLog;

		// set the pattern to be appended to the prefix
		allRunsLog = this.logFileDir + File.separator + this.logFilePrefix
				+ ".log";
		lastRunLog = this.logFileDir + File.separator + this.logFilePrefix
				+ "_LASTRUN.log";

		root = Logger.getRootLogger();

		// get the all runs appender
		allRunsAppender = (FileAppender) root.getAppender("allruns");
		// get the old filename
		allRunsFile = new File(allRunsAppender.getFile());
		// only set the new log file name if that file path has actually changed
		if (!allRunsFile.getAbsolutePath().equalsIgnoreCase(allRunsLog)) {
			// set the new filename
			allRunsAppender.setFile(allRunsLog);
			allRunsAppender.activateOptions();
			// delete the old files if existent and empty
			if (allRunsFile.exists() && (allRunsFile.length() == 0)) {
				allRunsFile.delete();
			}
		}

		// get the last run appender
		lastRunAppender = (FileAppender) root.getAppender("lastrun");
		// get the old filename
		lastRunFile = new File(lastRunAppender.getFile());
		// only set the new log file name if that file path has actually changed
		if (!lastRunFile.getAbsolutePath().equalsIgnoreCase(lastRunLog)) {
			// set the new filename
			lastRunAppender.setFile(lastRunLog);
			lastRunAppender.activateOptions();
			// delete the old files if existent and empty
			if (lastRunFile.exists() && (lastRunFile.length() == 0)) {
				lastRunFile.delete();
			}
		}
	}

	/**
	 * Sets the logging mode for a logging appender. The appender found under
	 * the given name is added or removed to the root logger instance depending
	 * on the boolean flag provided. By this means logging via this appender can
	 * easily be toggled.
	 *
	 * @param appenderName
	 * @param appenderEnabled
	 */
	public void setAppenderMode(String appenderName, boolean appenderEnabled) {
		Logger rootLogger;
		Appender appender;

		rootLogger = Logger.getRootLogger();
		appender = rootLogger.getAppender(appenderName);

		if (appender != null) {
			if (appenderEnabled) {
				rootLogger.addAppender(appender);
			} else {
				rootLogger.removeAppender(appender);
			}
		}
	}

	/**
	 * Convenience method to easily switch logging to the console on or off.
	 * This method assumes the console appender to be configured with the name
	 * "stdout".
	 *
	 * @param consoleLogging
	 *            Boolean flag whether loggin to the console shall be activated.
	 */
	public void setConsoleLogging(boolean consoleLogging) {
		// setAppenderMode("stdout", consoleLogging);
		setAppenderMode("console", consoleLogging);
	}

}
