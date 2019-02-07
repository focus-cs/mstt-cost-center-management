package com.schneider.cost_center_mngt.business;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class UserLog {
	
	private static UserLog classInstance;
	private static String fileName;
	public static final int ERROR_STATUS = 2;
	public static final int WARNING_STATUS = 1;
	public static final int OK_STATUS = 0;
	private transient int status = 0;
	private transient final List<String> traces;

	/**
	 * Logger instance.
	 */
	private static final Logger LOG = Logger.getLogger(CostCenterManager.class);

	/**
	 * 
	 * @return The unique instance of UserLog.
	 */
	public static UserLog getInstance() {
		return getClassInstance();
	}

	public static UserLog getInstance(String logFileName) {
		
		if (!logFileName.equals(fileName)) {
			classInstance = new UserLog(logFileName);
		}
		
		return getClassInstance();
	}

	public UserLog(String csvFileName) {
		LOG.debug("New UserLog " + csvFileName);
		fileName = csvFileName;
		this.traces = new ArrayList<String>();
	}

	/**
	 * 
	 * @param msg
	 */
	public int warning(final String row_id, final String msg, final int error_code) {
		this.traces.add(WARNING_STATUS + ";" + row_id + ";" + msg + ";" + error_code);

		if (this.status == OK_STATUS) {
			this.status = WARNING_STATUS;
		}

		return WARNING_STATUS;
	}

	/**
	 * 
	 * @param msg
	 */
	public void error(final String row_id, final String msg, final int error_code) {
		this.traces.add(ERROR_STATUS + ";" + row_id + ";" + msg + ";" + error_code);

		this.status = ERROR_STATUS;
	}

	/**
	 * 
	 * @param ro
	 */
	public void info(final String row_id, final String msg, final int error_code) {
		this.traces.add(OK_STATUS + ";" + row_id + ";" + msg + ";" + error_code);
	}

	/**
	 * 
	 * @return On of values {ERROR_STATUS,WARNING_STATUS,OK_STATUS}.
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * @return the classIsnatnce
	 */
	private static UserLog getClassInstance() {
		return classInstance;
	}

	/**
	 * @return the traces
	 */
	public List<String> getTraces() {
		return this.traces;
	}

	public String generateLogFile(final String directory, final String file) {
		// calcultate log file name NameofCSVfile_YYYYMMDD-HHMMSS_OK/KO.log
		String finalDir = (this.status > 0 ? "error" : "success") + File.separator;
		String finalName = directory + finalDir + file + "_" + (this.status > 0 ? "KO" : "OK") + ".log";
		
		try {
			final FileWriter logfile = new FileWriter(finalName);
			
			for (int i = 0; i < UserLog.getInstance().getTraces().size(); i++) {

				logfile.write(UserLog.getInstance().getTraces().get(i));

				if (i != UserLog.getInstance().getTraces().size() - 1) {
					logfile.write("\n");
				}
			}
			
			logfile.close();
		} catch (IOException e) {
			LOG.warn("could not write log file", e);
		}
		
		return finalDir;
	}

	public String getFileName() {
		return fileName;
	}
	
}
