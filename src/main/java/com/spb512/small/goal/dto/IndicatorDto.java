package com.spb512.small.goal.dto;

/**
 * @Description:
 * @author spb512
 * @date 2022年6月28日
 */
public class IndicatorDto {

	private double sar;
	private double rsi;
	private double diff;
	private double dea;
	private double stick;
	private int sarTurningPoint;
	private int macdTurningPoint;

	public double getSar() {
		return sar;
	}

	public void setSar(double sar) {
		this.sar = sar;
	}

	public double getRsi() {
		return rsi;
	}

	public void setRsi(double rsi) {
		this.rsi = rsi;
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

}
