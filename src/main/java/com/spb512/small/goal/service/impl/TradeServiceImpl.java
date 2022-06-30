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

	private String instId = "ETH-USDT-SWAP";
	private String lever = "10";
	private String mode = "isolated";
	private String instType = "SWAP";
	private String ccy = "USDT";
	private String bar = "3m";
	private String limit = "100";
	private String data = "data";
	private int skipCount = 0;
	private double[] dHigh = new double[100];
	private double[] dLow = new double[100];
	private double[] dClose = new double[100];

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
		double lowRsi = 20.0;
		double highRsi = 80.0;
		String trend = ";SAR指标:趋势不明";
		if (lowPrice >= sar) {// 上升趋势
			lowRsi = 20.0;
			highRsi = 85.0;
			trend = ";SAR指标:上升趋势" + sar;
		}
		if (highPrice <= sar) {// 下降趋势
			lowRsi = 15.0;
			highRsi = 80.0;
			trend = ";SAR指标:下降趋势" + sar;
		}
		if (sar > lowPrice && sar < highPrice) {
			logger.info("高:" + highPrice + ";低:" + lowPrice + ";SAR:" + sar);
		}
		logger.info("当前没有持仓;RSI指标：" + indicatorDto.getRsi() + trend);
		if (rsi < lowRsi || rsi > highRsi) {
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
			if (rsi > lowRsi) {
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
		int j = 99;
		for (int i = 0; i < candlesticksArray.size(); i++) {
			JSONArray jsonArray = candlesticksArray.getJSONArray(i);
			dHigh[j] = jsonArray.getDouble(2);
			dLow[j] = jsonArray.getDouble(3);
			dClose[j] = jsonArray.getDouble(4);
			j--;
		}
		FinStratEntity finEntity = finModel.calSar(dHigh, dLow, 0.02, 0.2);
		double dSar[] = finEntity.getSarReal();

		FinStratEntity rsiFinEntity = finModel.calRsi(dClose, 6);
		double dRsi[] = rsiFinEntity.getSarReal();
//		logger.info("dHigh:" + Arrays.toString(dHigh));
//		logger.info("dLow:" + Arrays.toString(dLow));
//		logger.info("dSar:" + Arrays.toString(dSar));

//		logger.info("dClose:" + Arrays.toString(dClose));
//		logger.info("dRsi:" + Arrays.toString(dRsi));
		IndicatorDto indicatorDto = new IndicatorDto();
		indicatorDto.setSar(dSar[dSar.length - 1]);
		indicatorDto.setRsi(dRsi[dRsi.length - 1]);
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

		double rsiLow = 30.0;
		double rsiHigh = 70.0;
		String trend = ";SAR指标:默认状态";
		if (lowPrice >= sar) {// 上升趋势
			rsiLow = 30.0;
			rsiHigh = 75.0;
			trend = ";SAR指标:上升趋势" + sar;
		}
		if (highPrice <= sar) {// 下降趋势
			rsiLow = 25.0;
			rsiHigh = 70.0;
			trend = ";SAR指标:下降趋势" + sar;
		}
		if (pos > 0) {
			logger.info("当前持做多仓；RSI指标：" + rsi + ";当前收益率：" + uplRatio + trend);
		} else {
			logger.info("当前持做空仓；RSI指标：" + rsi + ";当前收益率：" + uplRatio + trend);
		}
		if (pos > 0 && rsi > rsiHigh) {
			logger.info("做多平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
			sell();
		}
		if (pos < 0 && rsi < rsiLow) {
			logger.info("做空平仓收益率为：" + uplRatio + ";RSI指数为：" + rsi + trend);
			sell();
		}

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
		if (uplRatio.compareTo(new BigDecimal("-0.0214")) == -1) {// 达到强制止损线
			logger.info("当前收益率：" + uplRatio);
			logger.info("达到强制止损线-2.14%");
			sell();
			skipCount = 5;
		}
	}
}
