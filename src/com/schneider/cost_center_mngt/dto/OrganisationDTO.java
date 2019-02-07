package com.schneider.cost_center_mngt.dto;

public class OrganisationDTO {
	
	private String internalCostCenterId;

	private double currentYear;

	private int headerMonth;

	private int headerYear;

	private double yearPlus1;

	private double yearPlus2;

	private double yearPlus3;

	private double yearPlus4;

	private double yearPlus5;

	private double growthIndex;

	private double yearlyProjectHours;

	private String currency;

	public OrganisationDTO() {
		
	}

	public String getInternalCostCenterId() {
		return this.internalCostCenterId;
	}

	public void setInternalCostCenterId(String internalCostCenterId) {
		this.internalCostCenterId = internalCostCenterId;
	}

	public double getCurrentYear() {
		return this.currentYear;
	}

	public void setCurrentYear(double currentYear) {
		this.currentYear = currentYear;
	}

	public double getYearPlus1() {
		return this.yearPlus1;
	}

	public void setYearPlus1(double yearPlus1) {
		this.yearPlus1 = yearPlus1;
	}

	public double getYearPlus2() {
		return this.yearPlus2;
	}

	public void setYearPlus2(double yearPlus2) {
		this.yearPlus2 = yearPlus2;
	}

	public double getYearPlus3() {
		return this.yearPlus3;
	}

	public void setYearPlus3(double yearPlus3) {
		this.yearPlus3 = yearPlus3;
	}

	public double getYearPlus4() {
		return this.yearPlus4;
	}

	public void setYearPlus4(double yearPlus4) {
		this.yearPlus4 = yearPlus4;
	}

	public double getYearPlus5() {
		return this.yearPlus5;
	}

	public void setYearPlus5(double yearPlus5) {
		this.yearPlus5 = yearPlus5;
	}

	public double getGrowthIndex() {
		return this.growthIndex;
	}

	public void setGrowthIndex(double growthIndex) {
		this.growthIndex = growthIndex;
	}

	public double getYearlyProjectHours() {
		return this.yearlyProjectHours;
	}

	public void setYearlyProjectHours(double yearlyProjectHours) {
		this.yearlyProjectHours = yearlyProjectHours;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getHeaderMonth() {
		return this.headerMonth;
	}

	public void setHeaderMonth(int headerMonth) {
		this.headerMonth = headerMonth;
	}

	public int getHeaderYear() {
		return this.headerYear;
	}

	public void setHeaderYear(int headerYear) {
		this.headerYear = headerYear;
	}

}
