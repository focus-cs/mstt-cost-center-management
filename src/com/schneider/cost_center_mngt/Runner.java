package com.schneider.cost_center_mngt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.schneider.cost_center_mngt.api.Constants;
import com.schneider.cost_center_mngt.business.CostCenterManager;
import com.sciforma.psnext.api.Session;

public class Runner {
	
	public static final String APP_INFO = "MSTT Cost Center Management v1.1 (2018/10/24)";

	/**
	 * Logger Class instance.
	 */
	private static final Logger LOG = Logger.getLogger(Runner.class);

	/**
	 * UserLog Class instance.
	 */

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		if (args.length == 2) {
			try {
				PropertyConfigurator.configure(args[1]);
			
				// display application informations.
				LOG.info("#############################################################################");
				LOG.info(APP_INFO);

				// load PSNext properties
				final Properties properties_ps = readPropertiesFromFile(args[0]);
				LOG.debug(Constants.PROP_PSCONNECT + " properties file loaded");

				// load api properties
				LOG.debug(Constants.PROP_API + " properties file loaded");

				try {
					// init session
					LOG.debug("Connecting to PSNext");
					final Session session = new Session(properties_ps.getProperty(Constants.PSNEXT_URL));
					LOG.debug("Connected to PSNext");
					session.login(properties_ps.getProperty(Constants.PSNEXT_LOGIN), properties_ps.getProperty(Constants.PSNEXT_PASSWORD).toCharArray());
					LOG.debug("Logged in to PSNext");

					// Launch process
					new CostCenterManager(session).execute(properties_ps);
				} catch (Exception e1) {
					// Exception to connect to PSNext
					LOG.error("Cannot connect to MSTT, check parameters or contact MSTT administrator");
					LOG.error(e1.getMessage());
					e1.printStackTrace();
				}
			} catch (Exception e) {
				// exception to load properties file.
				e.printStackTrace();
				LOG.error("Cannot load properties file");
				LOG.error(e);
			}
		} else {
			LOG.error("Bad usage : java %JAVA_ARGS% -jar ..\\lib\\mstt-cost-center-management.jar ..\\conf\\");
		}

		LOG.info("Exit main program");
		LOG.info("#############################################################################");
		Runtime.getRuntime().exit(0);
	}

	/**
	 * Read Properties file using the path in input parameter
	 * 
	 * @return Properties
	 * @throws IOException
	 */
	public static Properties readPropertiesFromFile(final String path) throws IOException {
		final Properties properties = new Properties();
		final File file = new File(path);
		final InputStream resourceAsStream = new FileInputStream(file);
		properties.load(resourceAsStream);
		return properties;
	}

}
