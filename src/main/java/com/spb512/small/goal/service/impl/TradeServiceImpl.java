package com.spb512.small.goal.service.impl;

import java.math.BigDecimal;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okx.open.api.bean.account.param.SetLeverage;
import com.okx.open.api.bean.trade.param.ClosePositions;
import com.okx.open.api.bean.trade.param.PlaceOrder;
import com.okx.open.api.client.ApiClient;
import com.okx.open.api.service.account.impl.AccountApi;
import com.okx.open.api.service.market.data.impl.MarketDataApi;
import com.okx.open.api.service.publics.data.impl.PublicDataApi;
import com.okx.open.api.service.trade.impl.TradeApi;
import com.spb512.small.goal.dto.IndicatorDto;
import com.spb512.small.goal.service.TradeService;
import com.spb512.small.goal.utils.PrivateClient;
import com.spb512.small.goal.utils.PublicClient;
import com.spb512.small.goal.utils.talib.FinStratEntity;
import com.spb512.small.goal.utils.talib.FinStratModel;

/**
 * @author spb512
 * @date 2022年6月19日 下午9:46:37
 * 
 */
@Service
public class TradeServiceImpl implements TradeService {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Resource
	private PublicClient publicClient;

	@Resource
	private PrivateClient privateClient;

	@Resource
	private FinStratModel finModel;

	private ApiClient pvClient;

	private ApiClient pbClient;

	private AccountApi accountApi;

	private TradeApi tradeApi;

	private MarketDataApi marketDataApi;

	private PublicDataApi publicDataApi;

	private String instId = "ETH-USDT-SWAP";
	private String lever = "10";
	private String mode = "isolated";
	private String instType = "SWAP";
	private String ccy = "USDT";
	private String bar = "5m";
	private String limit = "300";
	private String data = "data";
	private int skipCount = 0;
	private double[] dHigh = new double[Integer.parseInt(limit)];
	private double[] dLow = new double[Integer.parseInt(limit)];
	private double[] dClose = new double[Integer.parseInt(limit)];

	@Override
	public void openPosition() {

		pvClient = privateClient.getClient();
		if (accountApi == null) {
			accountApi = pvClient.createService(AccountApi.class);
		}
		if (tradeApi == null) {
			tradeApi = pvClient.createService(TradeApi.class);
		}
		pbClient = publicClient.getClient();
		if (marketDataApi == null) {
			marketDataApi = pbClient.createService(MarketDataApi.class);
		}
		if (publicDataApi == null) {
			publicDataApi = pbClient.createService(PublicDataApi.class);
		}

		// 当前是否有持仓
		JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
		JSONArray jsonArray = positionsObject.getJSONArray(data);
		if (!jsonArray.isEmpty()) {
			return;
		}

		// 查询k线数据
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 计算sar和rsi指标
		IndicatorDto indicatorDto = getIndicators(candlesticksArray);
		double rsi = indicatorDto.getRsi();
		double sar = indicatorDto.getSar();
		Double highPrice = candlesticksArray.getJSONArray(0).getDouble(2);
		Double lowPrice = candlesticksArray.getJSONArray(0).getDouble(3);
		Double currentPrice = candlesticksArray.getJSONArray(0).getDouble(4);
		// 是否跳过
		if (skipCount > 0) {
			skipCount--;
			logger.info("本次开仓流程跳过，当前跳过数skipCount：" + skipCount + ";RSI指标:" + rsi);
			return;
		}
		// 查询账户余额
		JSONObject balanceObject = pvClient.executeSync(accountApi.getBalance(ccy));
		JSONArray balanceArray = balanceObject.getJSONArray(data);
		JSONObject jsonObject = balanceArray.getJSONObject(0);
		JSONArray detailsArray = jsonObject.getJSONArray("details");
		JSONObject usdtBalance = detailsArray.getJSONObject(0);
		BigDecimal usdtCashBal = usdtBalance.getBigDecimal("cashBal");
		if (usdtCashBal.compareTo(new BigDecimal(15)) == -1) {
			logger.info("账号余额：" + usdtCashBal + ",余额过低小于15;" + "RSI指标:" + rsi);
			return;
		}

		// 查询杠杆倍数
		JSONObject leverAgeSync = pvClient.executeSync(accountApi.getLeverage(instId, mode));
		String leverString = leverAgeSync.getJSONArray(data).getJSONObject(0).getString("lever");
		if (!lever.equals(leverString)) {
			// 设置杠杆倍速
			SetLeverage setLeverage = new SetLeverage();
			setLeverage.setInstId(instId);
			setLeverage.setLever(lever);
			setLeverage.setMgnMode(mode);
			pvClient.executeSync(accountApi.setLeverage(JSONObject.parseObject(JSON.toJSONString(setLeverage))));
			logger.info("设置杠杆倍速为" + lever + "成功");
		}

		// 开仓
		String trend = ";SAR指标:趋势不明";
		if (lowPrice >= sar) {
			trend = ";SAR指标:上升趋势" + sar;
		}
		if (highPrice <= sar) {
			trend = ";SAR指标:下降趋势" + sar;
		}
		if (sar > lowPrice && sar < highPrice) {
			logger.info("高:" + highPrice + ";低:" + lowPrice + ";SAR:" + sar);
		}
		logger.info("当前没有持仓;RSI指标：" + indicatorDto.getRsi() + trend);
		int turningPoint = indicatorDto.getMacdTurningPoint();
//		boolean upFlag = turningPoint == 0 && rsi > 55.0 && rsi < 70.0;
//		boolean lowFlag = turningPoint == 1 && rsi > 25.0 && rsi < 40.0;
		boolean upFlag = turningPoint == 0;
		boolean lowFlag = turningPoint == 1;
		if (upFlag || lowFlag) {
//			if (upFlag || lowFlag) {
			// 获取最大开仓数量
			JSONObject maxImun = pvClient.executeSync(
					accountApi.getMaximumTradableSizeForInstrument(instId, mode, null, currentPrice.toString()));
			JSONArray maxInumArray = maxImun.getJSONArray(data);
			JSONObject maxInumObject = maxInumArray.getJSONObject(0);
			long maxBuy = maxInumObject.getBigDecimal("maxBuy").multiply(new BigDecimal("0.90")).longValue();
			long maxSell = maxInumObject.getBigDecimal("maxSell").multiply(new BigDecimal("0.90")).longValue();
			if (maxBuy > 4500 || maxSell > 4500) {
				maxBuy = 4500;
				maxSell = 4500;
			}

			String side = "buy";
			String direction = "做多";
			long szNum = maxBuy;

			logger.info("最大购买数量" + maxBuy + "；最大可卖数量：" + maxSell);
			if (turningPoint == 1) {
				side = "sell";
				szNum = maxSell;
				direction = "做空";
			}
			PlaceOrder placeOrder = new PlaceOrder();
			placeOrder.setInstId(instId);
			placeOrder.setTdMode(mode);
			placeOrder.setSide(side);
			placeOrder.setOrdType("market");
			placeOrder.setSz(szNum + "");// 委托数量
			JSONObject orderSync = pvClient
					.executeSync(this.tradeApi.placeOrder(JSONObject.parseObject(JSON.toJSONString(placeOrder))));
			JSONArray orderArray = orderSync.getJSONArray(data);
			JSONObject order = orderArray.getJSONObject(0);
			logger.info("RSI指标：" + rsi + "；开" + direction + "仓成功，订单号ordId：" + order.getString("ordId") + "；执行结果sCode："
					+ order.getString("sCode") + "；执行信息sMsg：" + order.getString("sMsg") + "=======>当前余额：" + usdtCashBal
					+ trend);

		}
	}

	private IndicatorDto getIndicators(JSONArray candlesticksArray) {
		int j = Integer.parseInt(limit) - 1;
		for (int i = 0; i < candlesticksArray.size(); i++) {
			JSONArray jsonArray = candlesticksArray.getJSONArray(i);
			dHigh[j] = jsonArray.getDouble(2);
			dLow[j] = jsonArray.getDouble(3);
			dClose[j] = jsonArray.getDouble(4);
			j--;
		}
		FinStratEntity finEntity = finModel.calSar(dHigh, dLow, 0.02, 0.2);
		double dSar[] = finEntity.getSarReal();

		FinStratEntity rsiFinEntity = finModel.calRsi(dClose, 12);
		double dRsi[] = rsiFinEntity.getSarReal();

		FinStratEntity calMacd = finModel.calMacd(dClose, 12, 26, 9);
		double[] difMacd = calMacd.getDifMacd();
		double[] deaMacd = calMacd.getDeaMacd();
		double[] histMacd = calMacd.getHistMacd();
//		logger.info("difMacd:" + Arrays.toString(difMacd));
//		logger.info("deaMacd:" + Arrays.toString(deaMacd));
//		logger.info("histMacd:" + Arrays.toString(histMacd));
//		logger.info("dHigh:" + Arrays.toString(dHigh));
//		logger.info("dLow:" + Arrays.toString(dLow));
//		logger.info("dSar:" + Arrays.toString(dSar));
//
//		logger.info("dClose:" + Arrays.toString(dClose));
//		logger.info("dRsi:" + Arrays.toString(dRsi));

		IndicatorDto indicatorDto = new IndicatorDto();
		indicatorDto.setSar(dSar[dSar.length - 1]);
		indicatorDto.setRsi(dRsi[dRsi.length - 1]);
		indicatorDto.setDiff(difMacd[difMacd.length - 1]);
		indicatorDto.setDea(deaMacd[deaMacd.length - 1]);
		indicatorDto.setStick(histMacd[histMacd.length - 1]);
		indicatorDto.setSarTurningPoint(-1);
		indicatorDto.setMacdTurningPoint(-1);

		// 判断当前是否是拐点
		if (histMacd[histMacd.length - 2] < 0 && histMacd[histMacd.length - 1] > 0) {// 由下降转上升
			indicatorDto.setMacdTurningPoint(0);
			logger.info("macd拐点:下降--->上升");
		}
		if (histMacd[histMacd.length - 2] > 0 && histMacd[histMacd.length - 1] < 0) {// 由上升转下降
			indicatorDto.setMacdTurningPoint(1);
			logger.info("macd拐点:上升--->下降");
		}

		double curHigh = dHigh[dHigh.length - 1];
		double preHigh = dHigh[dHigh.length - 2];
		double curLow = dLow[dLow.length - 1];
		double preLow = dLow[dLow.length - 2];
		double curSar = dSar[dSar.length - 1];
		double preSar = dSar[dSar.length - 2];
		if (preHigh < preSar && curLow > curSar) {// 由下降转上升
			indicatorDto.setSarTurningPoint(0);
			logger.info("sar拐点:下降--->上升");
		}
		if (preLow > preSar && curHigh < curSar) {// 由上升转下降
			indicatorDto.setSarTurningPoint(1);
			logger.info("sar拐点:上升--->下降");
		}

//		int u = 0;
//		int d = 0;
//		for (int k = 8; k > 0; k--) {
//			double dh = dHigh[dHigh.length - k];
//			double dl = dLow[dLow.length - k];
//			double ds = dSar[dSar.length - k];
//			if (k > 1) {
//				if (dl > ds) {
//					u++;
//				}
//				if (dh < ds) {
//					d++;
//				}
//				logger.info(">>>>>dl" + dl + " > ds" + ds + ":" + (dl > ds) + "u:" + u);
//				logger.info("<<<<<dh" + dh + " < ds" + ds + ":" + (dh < ds) + "u:" + d);
//			} else {
//				if (dl > ds && d == 7) {
//					indicatorDto.setTurningPoint(0);
//				}
//				if (dh < ds && u == 7) {
//					indicatorDto.setTurningPoint(1);
//				}
//				logger.info(
//						"dl" + dl + " > ds" + ds + ":" + (dl > ds) + "&d("+d+")==7:" + (d == 7) + ":" + (dl > ds && d == 7));
//				logger.info(
//						"dh" + dh + " < ds" + ds + ":" + (dh < ds) + "&u("+u+")==7:" + (u == 7) + ":" + (dh < ds && u == 7));
//			}
//		}
		return indicatorDto;
	}

	/**
	 * 平仓
	 */
	@Override
	public void closePosition() {
		pvClient = privateClient.getClient();
		if (accountApi == null) {
			accountApi = pvClient.createService(AccountApi.class);
		}
		if (tradeApi == null) {
			tradeApi = pvClient.createService(TradeApi.class);
		}
		pbClient = publicClient.getClient();
		if (marketDataApi == null) {
			marketDataApi = pbClient.createService(MarketDataApi.class);
		}
		// 当前是否有持仓
		JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
		JSONArray jsonArray = positionsObject.getJSONArray(data);
		if (jsonArray.isEmpty()) {
			return;
		}
		JSONObject uplRatioObject = jsonArray.getJSONObject(0);
		BigDecimal uplRatio = uplRatioObject.getBigDecimal("uplRatio");
//		long cTime = uplRatioObject.getLongValue("cTime");

		// 判断是否达到止盈止损条件
		// 1、查询k线数据
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 2、计算SAR指数和RSI指数
		IndicatorDto indicatorDto = getIndicators(candlesticksArray);
		double rsi = indicatorDto.getRsi();
		double pos = uplRatioObject.getDouble("pos").doubleValue();
		double sar = indicatorDto.getSar();
		Double highPrice = candlesticksArray.getJSONArray(0).getDouble(2);
		Double lowPrice = candlesticksArray.getJSONArray(0).getDouble(3);

		String trend = ";SAR指标:默认状态";
		if (lowPrice >= sar) {// 上升趋势
			trend = ";SAR指标:上升趋势" + sar;
		}
		if (highPrice <= sar) {// 下降趋势
			trend = ";SAR指标:下降趋势" + sar;
		}
		if (pos > 0) {
			logger.info("当前持做多仓；RSI指标：" + rsi + ";当前收益率：" + uplRatio + trend);
		} else {
			logger.info("当前持做空仓；RSI指标：" + rsi + ";当前收益率：" + uplRatio + trend);
		}
		int turningPoint = indicatorDto.getMacdTurningPoint();
		boolean macdUpFlag = turningPoint == 0;
		boolean macdLowFlag = turningPoint == 1;
		int sarTurningPoint = indicatorDto.getSarTurningPoint();
		boolean sarUpFlag = sarTurningPoint == 0;
		boolean sarLowFlag = sarTurningPoint == 1;

//		JSONObject executeSync = pbClient.executeSync(publicDataApi.getSystemTime());
//		JSONArray sytmTimeData = executeSync.getJSONArray(data);
//		long systmTime = sytmTimeData.getJSONObject(0).getLongValue("ts");
//		boolean timeFlag = (systmTime - cTime) > 90000;// 平仓前至少需要持仓一个半周期
//		logger.info("(systmTime" + systmTime + " - cTime" + cTime + ")" + (systmTime - cTime) + " > 90000结果:"
//				+ ((systmTime - cTime) > 90000));
		if (pos > 0 && (sarLowFlag || macdLowFlag || rsi > 90)) {
			logger.info("做多平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
			sell();
		}
		if (pos < 0 && (sarUpFlag || macdUpFlag || rsi < 10)) {
			logger.info("做空平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
			sell();
		}
//		if (pos > 0 && rsi > rsiHigh) {
//			logger.info("做多平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
//			sell();
//		}
//		if (pos < 0 && rsi < rsiLow) {
//			logger.info("做空平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
//			sell();
//		}

	}

	private void sell() {
		ClosePositions closePositions = new ClosePositions();
		closePositions.setInstId(instId);
		closePositions.setMgnMode(mode);
		JSONObject closePosition = pvClient.executeSync(tradeApi.closePositions(closePositions));
		JSONObject balanceObject = pvClient.executeSync(accountApi.getBalance(ccy));
		JSONArray balanceArray = balanceObject.getJSONArray(data);
		JSONObject jsonObject = balanceArray.getJSONObject(0);
		JSONArray detailsArray = jsonObject.getJSONArray("details");
		JSONObject usdtBalance = detailsArray.getJSONObject(0);
		BigDecimal usdtCashBal = usdtBalance.getBigDecimal("cashBal");
		logger.info("平仓操作code：" + closePosition.getString("code") + ";msg:" + closePosition.getString("msg")
				+ ";=========>当前余额：" + usdtCashBal);
	}

	@Override
	public void checkPosition() {
		pvClient = privateClient.getClient();
		if (accountApi == null) {
			accountApi = pvClient.createService(AccountApi.class);
		}
		// 当前是否有持仓
		JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
		JSONArray jsonArray = positionsObject.getJSONArray(data);
		if (jsonArray.isEmpty()) {
			return;
		}
		JSONObject uplRatioObject = jsonArray.getJSONObject(0);
		BigDecimal uplRatio = uplRatioObject.getBigDecimal("uplRatio");
		if (uplRatio.compareTo(new BigDecimal("-0.13")) == -1) {// 达到强制止损线
			logger.info("当前收益率：" + uplRatio);
			logger.info("达到强制止损线-13%");
			sell();
			skipCount = 1;
		}
	}
}
