package com.schneider.cost_center_mngt.api;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.sciforma.psnext.api.LockException;
import com.sciforma.psnext.api.Organization;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Session;
import com.sciforma.psnext.api.SystemData;

public class SciformaManager {

	private static final String NAME_FIELD = "Name";

	/**
	 * Internal SciformaManager logger
	 */
	private static final Logger LOG = Logger.getLogger(SciformaManager.class);

	/**
	 * Session to manage
	 */
	private final transient Session session;

	/**
	 * List of User objects, each of which represents a Sciforma User
	 */
	private transient Hashtable<String, Organization> orgaList = new Hashtable<String, Organization>();

	/**
	 * SciformaManager constructor
	 * 
	 * @param session session to manage
	 * @param version version of projects to manage
	 * @throws PSException if unable to get user list from given session
	 */
	public SciformaManager(Session session) throws PSException {
		this.session = session;
		getOrganisations();
	}

	@SuppressWarnings("unchecked")
	public void getOrganisations() throws PSException {
		Organization rootOrga = (Organization) this.session.getSystemData(SystemData.ORGANIZATIONS);
		this.orgaList.put(rootOrga.getStringField(NAME_FIELD), rootOrga);
		List<Organization> orgaChildList = rootOrga.getChildren();

		for (Organization orga : orgaChildList) {
			this.orgaList.put(orga.getParent() + "." + orga.getStringField(NAME_FIELD), orga);
			getOrganisations(orga);
		}

	}

	@SuppressWarnings("unchecked")
	private void getOrganisations(Organization orga) throws PSException {
		LOG.debug(orga.getParent() + "." + orga.getStringField(NAME_FIELD));
		List<Organization> orgaChildList = orga.getChildren();

		for (Organization node : orgaChildList) {
			this.orgaList.put(node.getParent() + "." + node.getStringField(NAME_FIELD), node);
			getOrganisations(node);
		}

	}

	public List<Organization> getOrganisation(String value, String fieldName) throws PSException {
		LOG.debug("Get Organisations where " + fieldName + " = " + value);
		List<Organization> orgaMatchList = new ArrayList<Organization>();
		Enumeration<Organization> enumOrgaList = this.orgaList.elements();
		String valueToCompare = "";

		while (enumOrgaList.hasMoreElements()) {
			Organization orga = enumOrgaList.nextElement();
			valueToCompare = orga.getStringField(fieldName);

			if (value.equals(valueToCompare)) {
				orgaMatchList.add(orga);
			}

		}

		LOG.debug("Cost center: " + value + " has " + orgaMatchList.size() + " child(ren).");
		return orgaMatchList;
	}

	protected Properties getProperties(String path) throws FileNotFoundException {
		Properties dbProperties = new Properties();
		BufferedReader resourceAsStream = new BufferedReader(new FileReader(path));

		try {
			dbProperties.load(resourceAsStream);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("<" + path + "> can't be read.");
		}

		return dbProperties;
	}

	public void lockOrganisation() {

		try {
			Organization root = (Organization) this.session.getSystemData(SystemData.ORGANIZATIONS);
			root.lock();
			LOG.info("Organization locked");
		} catch (LockException e) {
			LOG.error("Locked by: " + e.getLockingUser());
			e.printStackTrace();
		} catch (PSException e) {
			LOG.error(e);
			e.printStackTrace();
		}

	}

	public void unlockOrganisation() {
		Organization root = null;

		try {
			root = (Organization) this.session.getSystemData(SystemData.ORGANIZATIONS);
			root.save(false);
			LOG.info("Organization unlocked");
		} catch (LockException e) {
			LOG.error("Locked by: " + e.getLockingUser());
			e.printStackTrace();
		} catch (PSException e) {
			LOG.error(e);
			e.printStackTrace();
		} finally {

			if (root != null) {

				try {
					root.unlock();
				} catch (PSException e) {
					LOG.error(e);
					e.printStackTrace();
				}

			}

		}

	}

}
