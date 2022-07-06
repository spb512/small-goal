package com.spb512.small.goal.utils.talib;

/**
 * @Description:
 * @author spb512
 * @date 2022年6月28日
 */
public class FinStratEntity {
	private int retCode;
	private double[] sarReal;
	private double[] difMacd;
	private double[] deaMacd;
	private double[] histMacd;

	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public double[] getSarReal() {
		return sarReal;
	}

	public void setSarReal(double[] sarReal) {
		this.sarReal = sarReal;
	}

	public double[] getDifMacd() {
		return difMacd;
	}

	public void setDifMacd(double[] difMacd) {
		this.difMacd = difMacd;
	}

	public double[] getDeaMacd() {
		return deaMacd;
	}

	public void setDeaMacd(double[] deaMacd) {
		this.deaMacd = deaMacd;
	}

	public double[] getHistMacd() {
		return histMacd;
	}

	public void setHistMacd(double[] histMacd) {
		this.histMacd = histMacd;
	}

}
