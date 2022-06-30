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
	// Talib核心
	private Core finLib = new Core();

	/**
	 * 计算停损点转向指标（SAR）
	 * 
	 * @param inHigh   最高价
	 * @param inLow    最低价
	 * @param optAF    加速因子AF（因子 0.02）
	 * @param optMaxAF 加速因子AF最大值（因子 0.2）
	 * @return SAR计算结果数据
	 */
	public FinStratEntity calSar(double inHigh[], double inLow[], double optAF, double optMaxAF) {

		// 指标计算结果
		FinStratEntity fiResult = new FinStratEntity();

		int startIdx = 0;
		int endIdx = inHigh.length - 1;
		double[] sarReal = new double[inHigh.length - 1];

		RetCode retCode = this.finLib.sar(startIdx, endIdx, inHigh, inLow, optAF, optMaxAF, new MInteger(),
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
}
