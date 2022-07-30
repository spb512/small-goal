package com.spb512.small.goal.service.impl;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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

	private String data = "data";
	private String sCode = "sCode";
	private String code = "code";
	/**
	 * 产品id
	 */
	private String instId = "ETH-USDT-SWAP";
	/**
	 * 杠杆倍数
	 */
	private String lever = "10";
	/**
	 * 保证金模式 isolated：逐仓 cross：全仓
	 */
	private String mode = "isolated";
	/**
	 * 产品类型 SPOT：币币 MARGIN：币币杠杆 SWAP：永续合约 FUTURES：交割合约 OPTION：期权
	 */
	private String instType = "SWAP";
	/**
	 * 币种
	 */
	private String ccy = "USDT";
	/**
	 * 时间粒度，默认值1m 如 [1m/3m/5m/15m/30m/1H/2H/4H]
	 * 香港时间开盘价k线：[6H/12H/1D/2D/3D/1W/1M/3M/6M/1Y]
	 * UTC时间开盘价k线：[/6Hutc/12Hutc/1Dutc/2Dutc/3Dutc/1Wutc/1Mutc/3Mutc/6Mutc/1Yutc]
	 */
	private int intBar = 1;
	/**
	 * 时间粒度+单位
	 */
	private String bar = intBar + "m";
	/**
	 * 分页返回的结果集数量，最大为100，不填默认返回100条(市价是300)
	 */
	private String limit = "100";
	/**
	 * 是否有持仓
	 */
	private boolean isPosition = false;
	/**
	 * 最高盈利率
	 */
	private BigDecimal highestUplRatio = BigDecimal.ZERO;
//	/**
//	 * 最高做空点
//	 */
//	private double highestHighRsi = 0;
//	/**
//	 * 最低做空点
//	 */
//	private double lowestLowRsi = 100;
	private double[] dHigh = new double[Integer.parseInt(limit)];
	private double[] dLow = new double[Integer.parseInt(limit)];
	private double[] dClose = new double[Integer.parseInt(limit)];
	/**
	 * 最小开仓资金
	 */
	private int minStartup = 15;
	/**
	 * 市价交易单次最大买和卖的数量
	 */
	private long maxBuyOrSell = 4500;
	/**
	 * 收益率激活
	 */
	private double activateRatio = 0.03;
	/**
	 * 回调收益率
	 */
	private double pullbackRatio = 0.01;
	/**
	 * 强制止损线
	 */
	private double stopLossLine = -0.10;
	/**
	 * rsi12做空激活点
	 */
	private int activateHighRsi12 = 65;
	/**
	 * rsi12做多激活点
	 */
	private int activateLowRsi12 = 35;
//	/**
//	 * 回调开仓点
//	 */
//	private double pullbackRsi = 1;
//	/**
//	 * 做多
//	 */
//	private boolean doBuy = false;
//	/**
//	 * 做空
//	 */
//	private boolean doSell = false;

	@PostConstruct
	public void init() {
		pvClient = privateClient.getClient();
		pbClient = publicClient.getClient();
		if (accountApi == null) {
			accountApi = pvClient.createService(AccountApi.class);
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
				logger.info("设置杠杆倍速为{}成功", lever);
			}
		}
		if (tradeApi == null) {
			tradeApi = pvClient.createService(TradeApi.class);
		}

		if (marketDataApi == null) {
			marketDataApi = pbClient.createService(MarketDataApi.class);
		}
		if (publicDataApi == null) {
			publicDataApi = pbClient.createService(PublicDataApi.class);
		}
	}

	@Override
	public synchronized void openPosition() {
		if (isPosition) {
			return;
		}
		// 查询k线数据(标记价格)
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getMarkPriceCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 计算指标
		IndicatorDto indicatorDto = getIndicators(candlesticksArray);
		double rsi12 = indicatorDto.getRsi12();
		double rsi24 = indicatorDto.getRsi24();
		double[] rsi12Arr = indicatorDto.getRsi12Arr();
		// 查询账户余额
		JSONObject balanceObject = pvClient.executeSync(accountApi.getBalance(ccy));
		JSONArray balanceArray = balanceObject.getJSONArray(data);
		JSONObject jsonObject = balanceArray.getJSONObject(0);
		JSONArray detailsArray = jsonObject.getJSONArray("details");
		JSONObject usdtBalance = detailsArray.getJSONObject(0);
		BigDecimal usdtCashBal = usdtBalance.getBigDecimal("cashBal");
		if (usdtCashBal.compareTo(new BigDecimal(minStartup)) == -1) {
			logger.info("账号余额:{},余额过低小于{};rsi12指标:{}rsi24指标:{}", usdtCashBal, minStartup, rsi12, rsi24);
			return;
		}
//		if (rsi12 > activateHighRsi12) {
//			if (rsi12 > highestHighRsi) {
//				highestHighRsi = rsi12;
//				logger.info("highestHighRsi更新，当前为:{}", highestHighRsi);
//			}
//		}
//		if (highestHighRsi > activateHighRsi12) {
//			if (highestHighRsi - rsi12 > pullbackRsi) {
//				doSell = true;
//			}
//		}
//
//		if (rsi12 < activateLowRsi12) {
//			if (rsi12 < lowestLowRsi) {
//				lowestLowRsi = rsi12;
//				logger.info("lowestLowRsi更新，当前为:{}", lowestLowRsi);
//			}
//		}
//		if (lowestLowRsi < activateLowRsi12) {
//			if (rsi12 - lowestLowRsi > pullbackRsi) {
//				doBuy = true;
//			}
//		}
		boolean buyFlag = rsi12Arr[0] < activateHighRsi12 && rsi12Arr[1] < activateHighRsi12
				&& rsi12Arr[2] < activateHighRsi12 && rsi12Arr[3] < activateHighRsi12 && rsi12Arr[4] < activateHighRsi12
				&& rsi12Arr[5] > activateHighRsi12;
		boolean sellFlag = rsi12Arr[0] > activateLowRsi12 && rsi12Arr[1] > activateLowRsi12
				&& rsi12Arr[2] > activateLowRsi12 && rsi12Arr[3] > activateLowRsi12 && rsi12Arr[4] > activateLowRsi12
				&& rsi12Arr[5] < activateLowRsi12;
		if (buyFlag || sellFlag) {
			// 获取最大开仓数量
			JSONObject maxImun = pvClient
					.executeSync(accountApi.getMaximumTradableSizeForInstrument(instId, mode, null, null));
			JSONArray maxInumArray = maxImun.getJSONArray(data);
			JSONObject maxInumObject = maxInumArray.getJSONObject(0);
			String maxBuy = maxInumObject.getString("maxBuy");
			String maxSell = maxInumObject.getString("maxSell");

			if (Long.parseLong(maxBuy) > maxBuyOrSell || Long.parseLong(maxSell) > maxBuyOrSell) {
				maxBuy = "4500";
				maxSell = "4500";
			}
			String side = "buy";
			String direction = "做多";
			String szNum = maxBuy;
			logger.info("最大购买数量{};最大可卖数量:{}", maxBuy, maxSell);
			if (sellFlag) {
				side = "sell";
				szNum = maxSell;
				direction = "做空";
			}
			PlaceOrder placeOrder = new PlaceOrder();
			placeOrder.setInstId(instId);
			placeOrder.setTdMode(mode);
			placeOrder.setSide(side);
			placeOrder.setOrdType("market");
			// 委托数量
			placeOrder.setSz(szNum);
			JSONObject orderSync = pvClient
					.executeSync(this.tradeApi.placeOrder(JSONObject.parseObject(JSON.toJSONString(placeOrder))));
			JSONArray orderArray = orderSync.getJSONArray(data);
			JSONObject order = orderArray.getJSONObject(0);
			if (order.getIntValue(sCode) == 0) {
				isPosition = true;
//				lowestLowRsi = 100;
//				highestHighRsi = 0;
//				doBuy = false;
//				doSell = false;
			}
			logger.info("RSI指标:{}:开{}仓成功，订单号ordId:{};执行结果sCode:{};执行信息sMsg:{}=======>当前余额:{}", rsi12, direction,
					order.getString("ordId"), order.getString(sCode), order.getString("sMsg"), usdtCashBal);
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

		FinStratEntity rsi6FinEntity = finModel.calRsi(dClose, 6);
		double dRsi6[] = rsi6FinEntity.getSarReal();
		FinStratEntity rsi12FinEntity = finModel.calRsi(dClose, 12);
		double dRsi12[] = rsi12FinEntity.getSarReal();
		FinStratEntity rsi24FinEntity = finModel.calRsi(dClose, 24);
		double dRsi24[] = rsi24FinEntity.getSarReal();

		FinStratEntity calMacd = finModel.calMacd(dClose, 12, 26, 9);
		double[] difMacd = calMacd.getDifMacd();
		double[] deaMacd = calMacd.getDeaMacd();
		double[] histMacd = calMacd.getHistMacd();

		IndicatorDto indicatorDto = new IndicatorDto();
		indicatorDto.setSar(dSar[dSar.length - 1]);
		indicatorDto.setRsi6(dRsi6[dRsi6.length - 1]);
		indicatorDto.setRsi12(dRsi12[dRsi12.length - 1]);
		indicatorDto.setRsi24(dRsi24[dRsi24.length - 1]);
		indicatorDto.setDiff(difMacd[difMacd.length - 1]);
		indicatorDto.setDea(deaMacd[deaMacd.length - 1]);
		indicatorDto.setStick(histMacd[histMacd.length - 1]);
		indicatorDto.setSarTurningPoint(-1);
		indicatorDto.setMacdTurningPoint(-1);
		indicatorDto.setRsi6Arr(new double[] { dRsi6[dRsi6.length - 5], dRsi6[dRsi6.length - 4],
				dRsi6[dRsi6.length - 3], dRsi6[dRsi6.length - 2], dRsi6[dRsi6.length - 1] });
		indicatorDto.setRsi12Arr(
				new double[] { dRsi12[dRsi12.length - 6], dRsi12[dRsi12.length - 5], dRsi12[dRsi12.length - 4],
						dRsi12[dRsi12.length - 3], dRsi12[dRsi12.length - 2], dRsi12[dRsi12.length - 1] });
		return indicatorDto;
	}

	/**
	 * 平仓
	 */
	@Override
	public void closePosition() {

		if (accountApi == null) {
			accountApi = pvClient.createService(AccountApi.class);
		}
		if (tradeApi == null) {
			tradeApi = pvClient.createService(TradeApi.class);
		}

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
		// 1、查询k线数据(标记价格)
		JSONObject candlesticksSync = pbClient
				.executeSync(marketDataApi.getMarkPriceCandlesticks(instId, null, null, bar, limit));
		JSONArray candlesticksArray = candlesticksSync.getJSONArray(data);
		// 2、计算指标
		IndicatorDto indicatorDto = getIndicators(candlesticksArray);
		double rsi6 = indicatorDto.getRsi6();
		double rsi12 = indicatorDto.getRsi12();
		double rsi24 = indicatorDto.getRsi24();
		if (uplRatio.compareTo(new BigDecimal(activateRatio)) > -1) {
			if (uplRatio.compareTo(highestUplRatio) == 1) {
				highestUplRatio = uplRatio;
				logger.info("highestUplRatio更新，当前为:{};rsi6指数为:{};rsi12指数为:{};rsi24指数为:{}", highestUplRatio, rsi6, rsi12,
						rsi24);
			}
		}
		if (highestUplRatio.compareTo(new BigDecimal(activateRatio)) > -1) {
			if (uplRatio.compareTo(highestUplRatio.subtract(new BigDecimal(pullbackRatio))) < 1) {
				sell();
				logger.info("平仓收益率为:{};rsi6指数为:{};rsi12指数为:{};rsi24指数为:{}", uplRatio, rsi6, rsi12, rsi24);
			}
		}
	}

	private synchronized void sell() {
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
		highestUplRatio = BigDecimal.ZERO;
		if (closePosition.getIntValue(code) == 0) {
			isPosition = false;
		}
		logger.info("平仓操作code:{};msg:{};=========>当前余额:{}", closePosition.getString(code),
				closePosition.getString("msg"), usdtCashBal);
	}

	@Override
	public synchronized void checkPosition() {

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
		if (uplRatio.compareTo(new BigDecimal(stopLossLine)) == -1) {
			logger.info("当前收益率{}", uplRatio);
			logger.info("达到强制止损线{}%", stopLossLine * 100);
			sell();
		}
	}
}
