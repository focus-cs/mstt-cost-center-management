package com.schneider.cost_center_mngt;

public class Test {

	public static void main(String[] args) {
		String tempEnforce_value = "1000";
		Double enforce_value = null;

		try {
			enforce_value = Double.valueOf(tempEnforce_value);
		} catch (Exception e) {
			System.out.println("oups");
		}

		System.out.println(enforce_value);
	}

}
