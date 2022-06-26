package com.spb512.small.goal.service.impl;

import java.math.BigDecimal;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okex.open.api.bean.account.param.SetLeverage;
import com.okex.open.api.bean.trade.param.ClosePositions;
import com.okex.open.api.bean.trade.param.PlaceOrder;
import com.okex.open.api.client.ApiClient;
import com.okex.open.api.service.account.impl.AccountApi;
import com.okex.open.api.service.market.data.impl.MarketDataApi;
import com.okex.open.api.service.trade.impl.TradeApi;
import com.spb512.small.goal.service.TradeService;
import com.spb512.small.goal.utils.PrivateClient;
import com.spb512.small.goal.utils.PublicClient;

/**
 * @author spb512
 * @date 2022年6月19日 下午9:46:37
 * 
 */
@Service
public class TradeServiceImpl implements TradeService {

	private static final Logger LOG = LoggerFactory.getLogger(TradeServiceImpl.class);

	@Resource
	private PublicClient publicClient;

	@Resource
	private PrivateClient privateClient;

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
	private String limit = "6";
	private String data = "data";

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
		LOG.info("是否有持仓：" + !jsonArray.isEmpty());
		// 查询账户余额
		JSONObject balanceObject = pvClient.executeSync(accountApi.getBalance(ccy));
		JSONArray balanceArray = balanceObject.getJSONArray(data);
		JSONObject jsonObject = balanceArray.getJSONObject(0);
		JSONArray detailsArray = jsonObject.getJSONArray("details");
		JSONObject usdtBalance = detailsArray.getJSONObject(0);
		BigDecimal usdtCashBal = usdtBalance.getBigDecimal("cashBal");
		if (usdtCashBal.compareTo(new BigDecimal(15)) == -1) {
			return;
		}
//		LOG.info("账号余额：" + usdtCashBal);
		// 查询市场数据，寻找下单时机
		// 1、查询k线数据
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 2、计算rsi参数
		double rsiResult = getRsi(candlesticksArray);
		LOG.info("RSI指数：" + rsiResult);

		// 查询杠杆倍数
		JSONObject leverAgeSync = pvClient.executeSync(accountApi.getLeverage(instId, mode));
		String leverString = leverAgeSync.getJSONArray(data).getJSONObject(0).getString("lever");
//		LOG.info("当前杠杆倍数：" + leverString);
		if (!lever.equals(leverString)) {
			// 设置杠杆倍速
			SetLeverage setLeverage = new SetLeverage();
			setLeverage.setInstId(instId);
			setLeverage.setLever(lever);
			setLeverage.setMgnMode(mode);
			pvClient.executeSync(accountApi.setLeverage(JSONObject.parseObject(JSON.toJSONString(setLeverage))));
			LOG.info("设置杠杆倍速为" + lever + "成功");
		}

		if (rsiResult == 110) {
			return;
		}
		// 开仓
		double rsiLow = 20.0;
		double rsiHigh = 80.0;
		if (rsiResult < rsiLow || rsiResult > rsiHigh) {
			Double currentPrice = candlesticksArray.getJSONArray(candlesticksArray.size() - 1).getDouble(4);
			// 获取最大开仓数量
			JSONObject maxImun = pvClient.executeSync(
					accountApi.getMaximumTradableSizeForInstrument(instId, mode, null, currentPrice.toString()));
			JSONArray maxInumArray = maxImun.getJSONArray(data);
			JSONObject maxInumObject = maxInumArray.getJSONObject(0);
			BigDecimal maxBuy = maxInumObject.getBigDecimal("maxBuy").multiply(new BigDecimal("0.618"));
			BigDecimal maxSell = maxInumObject.getBigDecimal("maxSell").multiply(new BigDecimal("0.618"));
			LOG.info("最大购买数量" + maxBuy + "；最大可卖数量：" + maxSell);

			String side = "buy";
			String direction = "做多";
			BigDecimal szNum = maxBuy;
			if (rsiResult > rsiHigh) {
				side = "sell";
				szNum = maxSell;
				direction = "做空";
			}
			PlaceOrder placeOrder = new PlaceOrder();
			placeOrder.setInstId(instId);
			placeOrder.setTdMode(mode);
			placeOrder.setSide(side);
			placeOrder.setOrdType("market");
			placeOrder.setSz(szNum.longValue() + "");// 委托数量
			JSONObject orderSync = pvClient
					.executeSync(this.tradeApi.placeOrder(JSONObject.parseObject(JSON.toJSONString(placeOrder))));
			JSONArray orderArray = orderSync.getJSONArray(data);
			JSONObject order = orderArray.getJSONObject(0);
			TradeServiceImpl.LOG.info("RSI指标：" + rsiResult + "；开" + direction + "仓成功，订单号ordId："
					+ order.getString("ordId") + "；执行结果sCode：" + order.getString("sCode") + "；执行信息sMsg："
					+ order.getString("sMsg") + "=======>当前余额：" + usdtCashBal);

		}
	}

	/**
	 * UP_AVG = UP_AMOUNT/PERIODS (周期内上涨数量平均); DOWN_AVG =
	 * DOWN_AMOUNT/PERIODS（周期内下跌数量平均）; RS = UP_AVG/DOWN_AVG（相对平均）; RSI = 100 - 100 /
	 * (1 + RS) （相对强弱指数）;
	 * RS（相对强度）=N日内收盘价涨数[close-open]和之均值÷N日内收盘价跌数[open-close]和之均值;
	 * RSI（相对强弱指标）=100－100÷（1+RS）;
	 * 
	 * @param candlesticksArray
	 * @return
	 */
	private double getRsi(JSONArray candlesticksArray) {
		double upAvg = 0;
		double downAvg = 0;
		int periods = 6;

		for (int i = (candlesticksArray.size() - 6); i < candlesticksArray.size(); i++) {
			JSONArray jsonArray = candlesticksArray.getJSONArray(i);
			Double openPrice = jsonArray.getDouble(1);
			Double closePrice = jsonArray.getDouble(4);
			// 价格上涨;
			if (closePrice >= openPrice) {
				upAvg += closePrice - openPrice;
			} else {// 价格下跌;
				downAvg += openPrice - closePrice;
			}
		}
		upAvg = upAvg / periods;
		downAvg = downAvg / periods;
		if (downAvg == 0) {
			return 110;
		}
		double rs = upAvg / downAvg;
		double rsiResult = 100 - 100 / (1 + rs);
		return rsiResult;
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
		LOG.info("是否有持仓：" + !jsonArray.isEmpty());
		JSONObject uplRatioObject = jsonArray.getJSONObject(0);
		BigDecimal uplRatio = uplRatioObject.getBigDecimal("uplRatio");
		LOG.info("当前收益率：" + uplRatio);
		if (uplRatio.compareTo(new BigDecimal("-0.10")) == -1) {// 达到强制止损线
			LOG.info("达到强制止损线-10%");
			sell();
		}
		// 判断是否达到止盈止损条件
		// 1、查询k线数据
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 2、计算rsi参数
		double rsiResult = getRsi(candlesticksArray);
		LOG.info("RSI指数：" + rsiResult);
		double rsiLow = 35.0;
		double rsiHigh = 65.0;
		double pos = uplRatioObject.getDouble("pos").doubleValue();
		if (pos > 0 && (uplRatio.compareTo(new BigDecimal("0.0512")) == 1) && rsiResult > rsiHigh) {
			LOG.info("做多平仓收益率为：" + uplRatio + ";RSI指数为：" + rsiResult);
			sell();
		}
		if (pos < 0 && (uplRatio.compareTo(new BigDecimal("0.0512")) == 1) && rsiResult < rsiLow) {
			LOG.info("做空平仓收益率为：" + uplRatio + ";RSI指数为：" + rsiResult);
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
		LOG.info("平仓操作code：" + closePosition.getString("code") + ";msg:" + closePosition.getString("msg")
				+ ";=========>当前余额：" + usdtCashBal);
	}

}
