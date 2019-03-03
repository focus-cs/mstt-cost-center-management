package com.schneider.cost_center_mngt.business;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.schneider.cost_center_mngt.api.Constants;
import com.schneider.cost_center_mngt.api.Errors;
import com.schneider.cost_center_mngt.api.SciformaManager;
import com.schneider.cost_center_mngt.data.FileImport;
import com.schneider.cost_center_mngt.dto.OrganisationDTO;
import com.sciforma.psnext.api.AccessException;
import com.sciforma.psnext.api.DatedData;
//import com.sciforma.psnext.api.CostData;
//import com.sciforma.psnext.api.CostDatedData;
import com.sciforma.psnext.api.DoubleDatedData;
import com.sciforma.psnext.api.Organization;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Session;

/**
 * 
 * @author
 * 
 */
public class CostCenterManager {

	/**
	 * SciformaManager instance.
	 */
	private transient final SciformaManager sciformaManager;

	/**
	 * separator to use in csv files
	 */
	private String csv_separator;

	/**
	 * separator to use for cost values ("," or ".")
	 */
	private String csv_numeric_separator;

	/**
	 * separator to use for cost values ("," or ".")
	 */
	private String psnext_numeric_separator;

	/**
	 * directory where searching for csv to load
	 */
	private String input_dir_requests;

	/**
	 * directory where generating log for a csv file + the csv itself (log file will
	 * have the name of the csv but with .log extension)
	 */
	private String input_dir_result;

	/**
	 * option to enforce the monthly labor rate on organizations not declared as RC
	 * or RC branches without any internal cost center ID
	 */
	private boolean enforce = false;

	private double enforce_value = 10000;

	/**
	 * UserLog instance.
	 */
	private static UserLog USER_LOG;

	/**
	 * Logger instance.
	 */
	private static final Logger LOG = Logger.getLogger(CostCenterManager.class);

	// -------------------------------------------------------------------------------------------------------------
	/**
	 * Constructor method.
	 */
	public CostCenterManager(final Session session) throws PSException {
		this.sciformaManager = new SciformaManager(session);
	}

	// -------------------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param properties_ps
	 * @param properties
	 * @param properties_ocp
	 * @throws PSException
	 * @throws AccessException
	 */
	public void execute(final Properties properties) throws AccessException, PSException {
		LOG.debug("START ProjectsManager.execute");
		this.psnext_numeric_separator = properties.getProperty(Constants.PSNEXT_NUMERIC_SEPARATOR, ".");
		this.csv_separator = properties.getProperty(Constants.CSV_SEPARATOR, ".");
		this.csv_numeric_separator = properties.getProperty(Constants.CSV_NUMERIC_SEPARATOR, ".");
		this.input_dir_requests = properties.getProperty(Constants.INPUT_DIR_REQUESTS);
		this.input_dir_result = properties.getProperty(Constants.INPUT_DIR_RESULT);
		this.enforce = "true".equals(properties.getProperty(Constants.ENFORCE, "false"));
		String tempEnforce_value = properties.getProperty(Constants.ENFORCE_VALUE, "1000");

		try {
			this.enforce_value = Double.valueOf(tempEnforce_value);
			LOG.debug("Enforce value " + Constants.ENFORCE_VALUE + " : " + tempEnforce_value);
		} catch (NumberFormatException ne) {
			LOG.debug("Wrong number format for " + Constants.ENFORCE_VALUE + " : " + tempEnforce_value);
		}

		LOG.debug("\n\tPROPERTIES DETAILS: " + "\n\tpsnext_numeric_separator=" + this.psnext_numeric_separator
				+ "\n\tcsv_separator=" + this.csv_separator + "\n\tcsv.numeric_separator=" + this.csv_numeric_separator
				+ "\n\tinput_dir_requests=" + this.input_dir_requests + "\n\tinput_dir_result=" + this.input_dir_result
				+ "\n\torga.enforce=" + this.enforce);

		if (!this.input_dir_requests.endsWith(File.separator)) {
			this.input_dir_requests = this.input_dir_requests + File.separator;
		}

		if (!this.input_dir_result.endsWith(File.separator)) {
			this.input_dir_result = this.input_dir_result + File.separator;
		}

		// treat all .csv file from inputDir/Requests
		File dir = new File(this.input_dir_requests);

		if (dir.exists() && dir.isDirectory()) {
			String[] list = dir.list();

			if (list.length == 0) {
				LOG.debug("No file found in " + dir.getAbsolutePath());
			} else {
				this.sciformaManager.lockOrganisation();

				for (int i = 0; i < list.length; i++) {
					String sourceFile = list[i];

					if (sourceFile.toLowerCase().endsWith(".csv")
							&& sourceFile.toLowerCase().startsWith("importccentermr")) {
						USER_LOG = UserLog.getInstance(sourceFile);
						final FileImport fileImport = new FileImport(this.input_dir_requests + sourceFile);
						LOG.debug("--------------------------------------------------------------------------");
						LOG.debug("--- Running input file = " + this.input_dir_requests + sourceFile);

						try {
							List<OrganisationDTO> orgaDTOList = fileImport.getData(this.csv_separator, USER_LOG);

							if (orgaDTOList == null) {
								LOG.info("Error on file " + this.input_dir_requests + sourceFile + " => FILE IGNORED");
								continue; // go to next file to treat
							}

							for (OrganisationDTO orgaDTO : orgaDTOList) {
								updateOrganisation(orgaDTO);
							}

						} catch (NumberFormatException e) {
							USER_LOG.error("0", e.getMessage(), Errors.DATA_FILE_ERRROR);
							this.archiveFile(sourceFile);
						}

						this.archiveFile(sourceFile);
					}

				}

				this.sciformaManager.unlockOrganisation();
			}

		} else {
			LOG.error("The API cannot reach the directory <" + this.input_dir_requests + ">");
		}

		// if optional task has been asked => for all organizations without internal
		// cost center id
		if (this.enforce) {
			updateOrganisations();
		}

		LOG.debug("END   ProjectsManager.execute");
	}

	private void archiveFile(String sourceFile) {
		// calcultate log file name NameofCSVfile_YYYYMMDD-HHMMSS_OK/KO.log
		final Calendar cal = Calendar.getInstance();
		String dateTime = cal.get(Calendar.YEAR) + String.format("%02d", (cal.get(Calendar.MONTH) + 1))
				+ String.format("%02d", cal.get(Calendar.DATE)) + "-"
				+ String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)) + String.format("%02d", cal.get(Calendar.MINUTE))
				+ String.format("%02d", cal.get(Calendar.SECOND));
		File sourceCSV = new File(this.input_dir_requests + sourceFile);
		String logFile = sourceFile.substring(0, sourceFile.lastIndexOf(".")) + "_" + dateTime;
		String directory = USER_LOG.generateLogFile(this.input_dir_result, logFile);
		String path = this.input_dir_result + directory + logFile + ".csv";

		try {
			File copyCSV = new File(path);

			if (copyCSV.createNewFile()) {
				Files.move(sourceCSV.toPath(), copyCSV.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

		} catch (IOException e) {
			LOG.error("The API cannot move the csv file <" + path + ">");
			e.printStackTrace();
		}

	}

	private void updateOrganisation(OrganisationDTO orgaDTO) {
		// Update every organisation where cost center id is found
		try {
			// retrieve every Real Organisation linked to this Cost center
			List<Organization> orgaList = this.sciformaManager.getOrganisation(orgaDTO.getInternalCostCenterId(),
					Constants.COST_CENTER_ID);
			// For each Organisation update fields Monthly labor rate, growth index and
			// Yearly projects hours
			for (Organization orga : orgaList) {

				if (isUpdatable(orga)) {
					updateOrganisation(orgaDTO, orga);
				} else {
					USER_LOG.warning(orgaDTO.getInternalCostCenterId(),
							orga.getParent() + "." + orga.getStringField("Name"), Errors.COST_CENTER_NOT_RC);
				}

			}

			if (orgaList.isEmpty()) {
				USER_LOG.warning(orgaDTO.getInternalCostCenterId(), "", Errors.COST_CENTER_NOT_EXIST);
			}

		} catch (PSException e) {
			LOG.error(e);
			USER_LOG.error(orgaDTO.getInternalCostCenterId(), "", Errors.ORGANISATION_SAVE);
			e.printStackTrace();
		}

	}

	private boolean isUpdatable(Organization orga) throws PSException {
		// The monthly labor rate synchronized with the cost center workflow tool is
		// only updated on RC branches
		// - Children branches in the tree (Parent = 0)
		// - Or Parent branch used for Requests (RC Check = 0)
		// If the organization is not a RC, the update is rejected even if there is an
		// internal cost center ID
		if (orga.getIntField(Constants.PARENT) == 0 || orga.getIntField(Constants.RC_CHECK) == 0) {
			return true;
		}
		return false;
	}

	private void updateOrganisation(OrganisationDTO orgaDTO, Organization orga) throws PSException {

		try {
			LOG.debug("updateOrganisation " + orga.getParent() + "." + orga.getStringField("Name"));
			// update Monthly labor rate
			updateDatedFieldNewVersion(Constants.MONTHLY_LABOR_RATE, orgaDTO, orga);
			// update growth index
			LOG.debug(Constants.GROWTH_INDEX + " = " + orgaDTO.getGrowthIndex());
			orga.setDoubleField(Constants.GROWTH_INDEX, orgaDTO.getGrowthIndex());
			// update yearly project hours
			LOG.debug(Constants.YEARLY_PROJECT_HOURS + " = " + orgaDTO.getYearlyProjectHours());
			orga.setDoubleField(Constants.YEARLY_PROJECT_HOURS, orgaDTO.getYearlyProjectHours());
			USER_LOG.info(orgaDTO.getInternalCostCenterId(), orga.getParent() + "." + orga.getStringField("Name"),
					Errors.SUCCESS);
		} catch (PSException e) {
			e.printStackTrace();
			LOG.error(e);
			USER_LOG.error(orgaDTO.getInternalCostCenterId(), orga.getParent() + "." + orga.getStringField("Name"),
					Errors.ORGANISATION_SAVE);
		}

	}

	private void updateOrganisations() {
		LOG.info(
				"== Option has been asked to enforce the monthly labor rate on organizations not declared as RC or RC branches without any internal cost center ID. ==");

		try {
			this.sciformaManager.lockOrganisation();
			// Get all organizations without internal cost center id
			List<Organization> orgaList = this.sciformaManager.getOrganisation("", Constants.COST_CENTER_ID);

			for (Organization orga : orgaList) {

				if (isUpdatable(orga)) {
					updateOrganisation(orga, 0, false);
				} else {
					updateOrganisation(orga, this.enforce_value, true);
				}

				LOG.info(orga.getParent() + "." + orga.getStringField("Name")
						+ " has been updated with default values.");
			}

			this.sciformaManager.unlockOrganisation();
		} catch (PSException e) {
			LOG.error(e);
			e.printStackTrace();
		}

	}

	private void updateOrganisation(Organization orga, double value, boolean defaultOrga) throws PSException {
		LOG.debug("updateOrganisation " + orga.getParent() + "." + orga.getStringField("Name"));
		// update Monthly labor rate

		if (defaultOrga) {
			setDatedField(Constants.MONTHLY_LABOR_RATE, orga, value);
		} else {
			updateDatedField(Constants.MONTHLY_LABOR_RATE, orga, value);
		}

		// update growth index
		LOG.debug(Constants.GROWTH_INDEX + " = 0");
		orga.setDoubleField(Constants.GROWTH_INDEX, 0d);
		// update yearly project hours
		LOG.debug(Constants.YEARLY_PROJECT_HOURS + " = 0");
		orga.setDoubleField(Constants.YEARLY_PROJECT_HOURS, 0d);
	}

	private void setDatedField(String fieldName, Organization orga, double value) throws PSException {
		List<DoubleDatedData> newCostList = new ArrayList<DoubleDatedData>();
		DoubleDatedData newCost = new DoubleDatedData(value, new Date(0), new Date(Long.MAX_VALUE));
		newCostList.add(newCost);
		orga.setDatedData(Constants.MONTHLY_LABOR_RATE, newCostList);
	}

	private void updateDatedField(String fieldName, Organization orga, double value) throws PSException {
		
		if (!this.isLastPeriodAtZero(orga)) {
			List<DoubleDatedData> data = new ArrayList<DoubleDatedData>();
			Calendar cal_deb = this.getStartDate();
			String currency = orga.getStringField(Constants.MONTHLY_LABOR_RATE_CURRENCY);
			String strFinalValue = value + " " + currency;
			strFinalValue = strFinalValue.replace(this.csv_numeric_separator.charAt(0), this.psnext_numeric_separator.charAt(0));
			DoubleDatedData newCost = new DoubleDatedData(orga, fieldName, strFinalValue, cal_deb.getTime(), new Date(Long.MAX_VALUE));
			data.add(newCost);
			orga.updateDatedData(Constants.MONTHLY_LABOR_RATE, data);
		}
		
	}

	private void updateDatedFieldNewVersion(String fieldName, OrganisationDTO orgaDTO, Organization orga)
			throws PSException {
		List<DoubleDatedData> data = new ArrayList<DoubleDatedData>();
		Calendar cal_deb = this.getStartDate();
		int currentYear = orgaDTO.getHeaderYear();
		int currentMonth = orgaDTO.getHeaderMonth();
		cal_deb.set(Calendar.YEAR, currentYear);
		cal_deb.set(Calendar.MONTH, currentMonth);
		data.add(updateDatedField(fieldName, orgaDTO.getCurrentYear(), cal_deb, null, orgaDTO.getCurrency(), orga));
		cal_deb.add(Calendar.YEAR, 1);
		cal_deb.set(Calendar.MONTH, 0);
		data.add(updateDatedField(fieldName, orgaDTO.getYearPlus1(), cal_deb, null, orgaDTO.getCurrency(), orga));
		cal_deb.add(Calendar.YEAR, 1);
		data.add(updateDatedField(fieldName, orgaDTO.getYearPlus2(), cal_deb, null, orgaDTO.getCurrency(), orga));
		cal_deb.add(Calendar.YEAR, 1);
		data.add(updateDatedField(fieldName, orgaDTO.getYearPlus3(), cal_deb, null, orgaDTO.getCurrency(), orga));
		cal_deb.add(Calendar.YEAR, 1);
		data.add(updateDatedField(fieldName, orgaDTO.getYearPlus4(), cal_deb, null, orgaDTO.getCurrency(), orga));
		cal_deb.add(Calendar.YEAR, 1);
		data.add(updateDatedField(fieldName, orgaDTO.getYearPlus5(), cal_deb, new Date(Long.MAX_VALUE), orgaDTO.getCurrency(), orga));
		orga.updateDatedData(Constants.MONTHLY_LABOR_RATE, data);
	}

	private boolean isLastPeriodAtZero(Organization orga) throws PSException {
		boolean result = false;
		@SuppressWarnings("unchecked")
		List<DoubleDatedData> oldList = orga.getDatedData(Constants.MONTHLY_LABOR_RATE, DatedData.NONE);
		Date start = null;
		Double value = null;
		
		if (!oldList.isEmpty()) {
			start = oldList.get(0).getStart();
			value = (double) oldList.get(0).getValue();
			
			for (DoubleDatedData data : oldList) {
				
				if (data.getStart().after(start)) {
					start = data.getStart();
					value = (double) data.getValue();
				}
				
			}
			
			if (value == 0) {
				result = true;
			}
			
		}
		
		return result;
	}
	
	private Calendar getStartDate() {
		Calendar cal_deb = Calendar.getInstance();
		cal_deb.set(Calendar.DAY_OF_MONTH, 1);
		cal_deb.set(Calendar.HOUR_OF_DAY, 0);
		cal_deb.set(Calendar.MINUTE, 0);
		cal_deb.set(Calendar.SECOND, 0);
		cal_deb.set(Calendar.MILLISECOND, 0);
		return cal_deb;
	}

	private DoubleDatedData updateDatedField(String fieldName, double value, Calendar cal_deb, Date endDate,
			String currency, Organization orga) throws PSException {
		Calendar cal_fin = null;

		if (endDate == null) {
			cal_fin = (Calendar) cal_deb.clone();
			cal_fin.add(Calendar.YEAR, 1);
			LOG.debug(cal_deb.getTime() + " = " + value);
		} else {
			cal_fin = Calendar.getInstance();
			cal_fin.setTime(endDate);
		}

		String strFinalValue = value + " " + currency;
		strFinalValue = strFinalValue.replace(this.csv_numeric_separator.charAt(0), this.psnext_numeric_separator.charAt(0));
		DoubleDatedData newCost = new DoubleDatedData(orga, fieldName, strFinalValue, cal_deb.getTime(), cal_fin.getTime());
		return newCost;
	}
	
}
