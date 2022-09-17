package com.spb512.small.goal.dto;

/**
 * @author spb512
 * @Description:
 * @date 2022年6月28日
 */
public class IndicatorDto {

    private double sar;
    private double rsi6;
    private double rsi12;
    private double[] rsi12Arr;
    private double rsi24;
    private double diff;
    private double dea;
    private double stick;
    private double cci;
    private int sarTurningPoint;
    private int macdTurningPoint;
    private int singleMacdTurningPoint;
    private double[] rsi6Arr;

    public double getSar() {
        return sar;
    }

    public void setSar(double sar) {
        this.sar = sar;
    }

    public double getRsi12() {
        return rsi12;
    }

    public void setRsi12(double rsi12) {
        this.rsi12 = rsi12;
    }

    public double getRsi6() {
        return rsi6;
    }

    public void setRsi6(double rsi6) {
        this.rsi6 = rsi6;
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

    public double[] getRsi6Arr() {
        return rsi6Arr;
    }

    public void setRsi6Arr(double[] rsi6Arr) {
        this.rsi6Arr = rsi6Arr;
    }

    public double getCci() {
        return cci;
    }

    public void setCci(double cci) {
        this.cci = cci;
    }

    public double[] getRsi12Arr() {
        return rsi12Arr;
    }

    public void setRsi12Arr(double[] rsi12Arr) {
        this.rsi12Arr = rsi12Arr;
    }

}
