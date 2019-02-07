package com.schneider.cost_center_mngt.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.schneider.cost_center_mngt.api.Errors;
import com.schneider.cost_center_mngt.business.UserLog;
import com.schneider.cost_center_mngt.dto.OrganisationDTO;

public class FileImport {
	
	private static final Logger LOG = Logger.getLogger(FileImport.class);

	private final transient String sourceFile;

	public FileImport(final String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public List<OrganisationDTO> getData(String csv_separator, UserLog USER_LOG) throws NumberFormatException {
		LOG.debug("START FileImport.getData");
		InputStream ips = null;
		BufferedReader buff = null;
		boolean errorFile = false;
		List<OrganisationDTO> orgaDTOList = new ArrayList<OrganisationDTO>();
		
		try {
			// Loading file
			ips = new FileInputStream(this.sourceFile);
			final InputStreamReader ipsr = new InputStreamReader(ips);
			buff = new BufferedReader(ipsr);
			int lineNumber = 2;
			int currentMonth = 0;
			int currentYear = 0;
			
			Calendar actualYearCal = Calendar.getInstance();
			int systemYear = actualYearCal.get(Calendar.YEAR);
			
			try {
				String header = buff.readLine();
				LOG.debug("Header line :" + header);
				// verify that current year value is really the actual current year
				final String[] rowHeader = header.split(csv_separator);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				
				if (!rowHeader[1].isEmpty()) {
					
					try {
						Date date = sdf.parse(rowHeader[1]);
						actualYearCal.setTime(date);
						actualYearCal.set(Calendar.DAY_OF_MONTH, 1);
						currentYear = actualYearCal.get(Calendar.YEAR);
						currentMonth = actualYearCal.get(Calendar.MONTH);
					} catch (ParseException e) {
						e.printStackTrace();
						LOG.warn("Current year doesn't match with the expected format");
						USER_LOG.warning("", "", Errors.CURRENT_YEAR);
						throw new NumberFormatException(rowHeader[1]);
					}
					
				}
					
			} catch (IOException e) {
				e.printStackTrace();
				LOG.warn("Can't read file");
				USER_LOG.warning("", "", Errors.CURRENT_YEAR);
				return null;
			} 

			LOG.debug("actualYear :" + systemYear);
			LOG.debug("currentYear :" + currentYear);		
			
			if (currentYear != systemYear) {
				LOG.warn("Current year doesn't match with the real current year");
				throw new NumberFormatException("" + currentYear);
			}
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			// extract data and construct a set of OrganisationDTO
			String ligne;
			int nbColumns = 10;
			List<String> unicityKeyList = new ArrayList<String>();

			while ((ligne = buff.readLine()) != null) {
				LOG.debug("### trait line = " + ligne);
				// Split the line in the array
				final String[] row = ligne.split(csv_separator);
				
				// 9 columns or more in the csv file
				if (row.length == nbColumns) {
					// get internal cost center id
					String internal_cost_center_id = row[0].trim();
					// get data for year to year+5
					String year = row[1].trim();
					String year_plus_1 = row[2].trim();
					String year_plus_2 = row[3].trim();
					String year_plus_3 = row[4].trim();
					String year_plus_4 = row[5].trim();
					String year_plus_5 = row[6].trim();
					// get growth index.
					String growthIndex = row[7].trim();
					// get yearly project hours
					String yearlyProjectHours = row[8].trim();
					// get currency
					String currency = row[9].trim();
					// -------------------------
					// Control data consistency
					if (internal_cost_center_id.isEmpty() || year.isEmpty() || year_plus_1.isEmpty()
							|| year_plus_2.isEmpty() || year_plus_3.isEmpty() || year_plus_4.isEmpty()
							|| year_plus_5.isEmpty() || growthIndex.isEmpty() || yearlyProjectHours.isEmpty()
							|| currency.isEmpty()) {
						LOG.warn("One of the expected values is empty (ie Internal cost center Id or Current year or year+1 or year+2 or year+3 or year+4 or year+5 or growth index or yearly project hours or currency)");
						USER_LOG.warning(internal_cost_center_id, "", Errors.EMPTY_VALUE);
						lineNumber++; // next line
						continue;
					}

					if (unicityKeyList.contains(internal_cost_center_id)) {
						LOG.error("Inconsistant input data : at least 2 lines with same Internal Cost Center ID (<" + internal_cost_center_id + ">)");
						USER_LOG.error(internal_cost_center_id, "", Errors.DUPLICATE_COST_CENTER_ID);
						errorFile = true;
						break;
					} else
						unicityKeyList.add(internal_cost_center_id);

					// affect values
					try {
						OrganisationDTO orgaDTO = new OrganisationDTO();
						orgaDTO.setInternalCostCenterId(internal_cost_center_id);
						orgaDTO.setCurrentYear(Double.parseDouble(year.replaceFirst(",", ".")));
						orgaDTO.setHeaderYear(currentYear);
						orgaDTO.setHeaderMonth(currentMonth);
						orgaDTO.setYearPlus1(Double.parseDouble(year_plus_1.replaceFirst(",", ".")));
						orgaDTO.setYearPlus2(Double.parseDouble(year_plus_2.replaceFirst(",", ".")));
						orgaDTO.setYearPlus3(Double.parseDouble(year_plus_3.replaceFirst(",", ".")));
						orgaDTO.setYearPlus4(Double.parseDouble(year_plus_4.replaceFirst(",", ".")));
						orgaDTO.setYearPlus5(Double.parseDouble(year_plus_5.replaceFirst(",", ".")));
						orgaDTO.setGrowthIndex(Double.parseDouble(growthIndex.replaceFirst(",", ".")));
						orgaDTO.setYearlyProjectHours(Double.parseDouble(yearlyProjectHours.replaceFirst(",", ".")));
						orgaDTO.setCurrency(currency);
						orgaDTOList.add(orgaDTO);
					} catch (NumberFormatException e) {
						e.printStackTrace();
						LOG.warn("One of the reported Monthly Rate doesn't match with the expected format");
						USER_LOG.warning(internal_cost_center_id, "", Errors.MONTHLY_RATE);
					}
					
				} else {
					LOG.error("Inconsistent input data: wrong number of columns in csv file on line <" + lineNumber + ">");
					USER_LOG.error("", "", Errors.DATA_FILE_ERRROR);
					errorFile = true;
				}
				
				lineNumber++;
			}

			LOG.debug("end of the file processing " + dateFormat.format(date));
		} catch (IOException e) {
			e.printStackTrace();
			LOG.debug("ERROR loading file : " + this.sourceFile);
			return null;
		} finally {
			
			if (ips != null) {
				
				try {
					ips.close();
				} catch (IOException e) {
					LOG.error(e);
					e.printStackTrace();
				}
				
			}
			
			if (buff != null) {
				
				try {
					buff.close();
				} catch (IOException e) {
					LOG.error(e);
					e.printStackTrace();
				}
				
			}
			
		}

		if (errorFile) {
			LOG.debug(orgaDTOList.size() + " cost center found but not continue because of error on file");
			LOG.debug("END   FileImport.getData");
			return null;
		} else {
			LOG.debug(orgaDTOList.size() + " cost center found");
			LOG.debug("END   FileImport.getData");
			return orgaDTOList;
		}
		
	}
	
}
