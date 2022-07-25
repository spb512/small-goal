package com.spb512.small.goal.utils.talib;

import org.springframework.stereotype.Component;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * @Description:
 * @author spb512
 * @date 2022年6月28日
 */
@Component
public class FinStratModel {
	/**
	 * Talib核心
	 */
	private Core finLib = new Core();

	/**
	 * 计算停损点转向指标（SAR）
	 * 
	 * @param inHigh   最高价
	 * @param inLow    最低价
	 * @param optAf    加速因子AF（因子 0.02）
	 * @param optMaxAf 加速因子AF最大值（因子 0.2）
	 * @return SAR计算结果数据
	 */
	public FinStratEntity calSar(double inHigh[], double inLow[], double optAf, double optMaxAf) {

		// 指标计算结果
		FinStratEntity fiResult = new FinStratEntity();

		int startIdx = 0;
		int endIdx = inHigh.length - 1;
		double[] sarReal = new double[inHigh.length - 1];

		RetCode retCode = this.finLib.sar(startIdx, endIdx, inHigh, inLow, optAf, optMaxAf, new MInteger(),
				new MInteger(), sarReal);
		if (retCode == RetCode.Success) {
			fiResult.setRetCode(0);
			fiResult.setSarReal(sarReal);
		}
		return fiResult;
	}

	/**
	 * 
	 * @param inClose      收盘价
	 * @param inTimePeriod 时间周期
	 * @return SRI计算结果数据
	 */
	public FinStratEntity calRsi(double inClose[], int inTimePeriod) {

		// 指标计算结果
		FinStratEntity fiResult = new FinStratEntity();

		int startIdx = 0;
		int endIdx = inClose.length - 1;
		double[] sriReal = new double[inClose.length - inTimePeriod];

		RetCode retCode = this.finLib.rsi(startIdx, endIdx, inClose, inTimePeriod, new MInteger(), new MInteger(),
				sriReal);
		if (retCode == RetCode.Success) {
			fiResult.setRetCode(0);
			fiResult.setSarReal(sriReal);
		}
		return fiResult;
	}

	/**
	 * 计算平滑异同移动平均线(MACD)
	 * 
	 * @param inReal          收盘价
	 * @param optFastPeriod   快速移动平均线（因子 12日EMA）
	 * @param optSlowPeriod   慢速移动平均线（因子 26日EMA）
	 * @param optSignalPeriod DEA移动平均线(因子 9日EMA)
	 * @return RSI数据
	 */
	public FinStratEntity calMacd(double inReal[], int optFastPeriod, int optSlowPeriod, int optSignalPeriod) {
		// 指标计算结果
		FinStratEntity fiResult = new FinStratEntity();

		// 基础计算库
		int startIdx = 0;
		int endIdx = inReal.length - 1;
		double[] macd = new double[inReal.length - 33];
		double[] macdSignal = new double[inReal.length - 33];
		double[] macdHist = new double[inReal.length - 33];
		RetCode retCode = this.finLib.macd(startIdx, endIdx, inReal, optFastPeriod, optSlowPeriod, optSignalPeriod,
				new MInteger(), new MInteger(), macd, macdSignal, macdHist);

		if (retCode == RetCode.Success) {
			fiResult.setRetCode(0);
			fiResult.setDifMacd(macd);
			fiResult.setDeaMacd(macdSignal);
			fiResult.setHistMacd(macdHist);
		}
		return fiResult;
	}
}
