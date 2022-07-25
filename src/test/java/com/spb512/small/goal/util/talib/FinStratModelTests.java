package com.spb512.small.goal.util.talib;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.spb512.small.goal.utils.talib.FinStratEntity;
import com.spb512.small.goal.utils.talib.FinStratModel;

/**
 * @Description:
 * @author spb512
 * @date 2022年6月29日
 */
@SpringBootTest
public class FinStratModelTests {
	@Test
	void testTalibSar() {
		double[] dHigh = new double[] { 1153.01, 1154.99, 1155.35, 1154.23, 1154.12, 1153.58, 1152.22, 1149.89, 1151.65,
				1151.97, 1151.42, 1151.5 };
		double[] dLow = new double[] { 1150.84, 1151.22, 1152.9, 1151.73, 1152.51, 1149.12, 1148.32, 1147.65, 1149.23,
				1150.01, 1150.21, 1146.58 };
		// 调用财经库指标函数计算SAR值
		FinStratModel finModel = new FinStratModel();
		FinStratEntity finEntity = finModel.calSar(dHigh, dLow, 0.02, 0.2);
		double dSarPrice[] = finEntity.getSarReal();
		for (double d : dSarPrice) {
			System.out.println("SAR结果：" + d);
		}
	}

	@Test
	void testTalibSri() {
		double[] dClose = new double[] { 1181.07, 1186.07, 1189.32, 1187.82, 1187.54, 1189.67, 1186.48, 1186.98,
				1189.58, 1192.13, 1190.56, 1192.05 };
		// 调用财经库指标函数计算SRI值
		FinStratModel finModel = new FinStratModel();
		FinStratEntity finEntity = finModel.calRsi(dClose, 6);
		double dSarPrice[] = finEntity.getSarReal();
		for (double d : dSarPrice) {
			System.out.println("SRI结果：" + d);
		}
	}
}
