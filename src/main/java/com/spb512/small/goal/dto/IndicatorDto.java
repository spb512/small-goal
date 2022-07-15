package com.spb512.small.goal.dto;

/**
 * @Description:
 * @author spb512
 * @date 2022年6月28日
 */
public class IndicatorDto {

	private double sar;
	private double rsi6;
	private double rsi12;
	private double rsi24;
	private double diff;
	private double dea;
	private double stick;
	private int sarTurningPoint;
	private int macdTurningPoint;
	private int singleMacdTurningPoint;

	public double getSar() {
		return sar;
	}

	public void setSar(double sar) {
		this.sar = sar;
	}

	public double getRsi12() {
		return rsi12;
	}

	public double getRsi6() {
		return rsi6;
	}

	public void setRsi6(double rsi6) {
		this.rsi6 = rsi6;
	}

	public void setRsi12(double rsi12) {
		this.rsi12 = rsi12;
	}

	public double getRsi24() {
		return rsi24;
	}

	public void setRsi24(double rsi24) {
		this.rsi24 = rsi24;
	}

	public double getDiff() {
		return diff;
	}

	public void setDiff(double diff) {
		this.diff = diff;
	}

	public double getDea() {
		return dea;
	}

	public void setDea(double dea) {
		this.dea = dea;
	}

	public double getStick() {
		return stick;
	}

	public void setStick(double stick) {
		this.stick = stick;
	}

	public int getSarTurningPoint() {
		return sarTurningPoint;
	}

	public void setSarTurningPoint(int sarTurningPoint) {
		this.sarTurningPoint = sarTurningPoint;
	}

	public int getMacdTurningPoint() {
		return macdTurningPoint;
	}

	public void setMacdTurningPoint(int macdTurningPoint) {
		this.macdTurningPoint = macdTurningPoint;
	}

	public int getSingleMacdTurningPoint() {
		return singleMacdTurningPoint;
	}

	public void setSingleMacdTurningPoint(int singleMacdTurningPoint) {
		this.singleMacdTurningPoint = singleMacdTurningPoint;
	}

//	public int getMacdTwoTurningPoint() {
//		return macdTwoTurningPoint;
//	}
//
//	public void setMacdTwoTurningPoint(int macdTwoTurningPoint) {
//		this.macdTwoTurningPoint = macdTwoTurningPoint;
//	}

}
