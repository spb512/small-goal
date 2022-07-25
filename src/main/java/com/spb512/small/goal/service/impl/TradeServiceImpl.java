package com.spb512.small.goal.service.impl;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;
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
	private double activateYield = 0.07;
	/**
	 * 回调收益率
	 */
	private double pullbackYield = 0.01;
	/**
	 * 强制止损线
	 */
	private double stopLossLine = -0.13;
	/**
	 * rsi12做空点
	 */
	private int highRsi = 81;
	/**
	 * rsi12做多点
	 */
	private int lowRsi = 19;

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
//		double rsi6 = indicatorDto.getRsi6();
		double rsi12 = indicatorDto.getRsi12();
		double rsi24 = indicatorDto.getRsi24();
//		double sar = indicatorDto.getSar();
//		double highPrice = candlesticksArray.getJSONArray(0).getDouble(2);
//		double lowPrice = candlesticksArray.getJSONArray(0).getDouble(3);
		String currentPrice = candlesticksArray.getJSONArray(0).getString(4);
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
		if (rsi12 < lowRsi || rsi12 > highRsi) {
			// 获取最大开仓数量
			JSONObject maxImun = pvClient
					.executeSync(accountApi.getMaximumTradableSizeForInstrument(instId, mode, null, currentPrice));
			JSONArray maxInumArray = maxImun.getJSONArray(data);
			JSONObject maxInumObject = maxInumArray.getJSONObject(0);
			long maxBuy = maxInumObject.getBigDecimal("maxBuy").multiply(new BigDecimal("0.90")).longValue();
			long maxSell = maxInumObject.getBigDecimal("maxSell").multiply(new BigDecimal("0.90")).longValue();
			if (maxBuy > maxBuyOrSell || maxSell > maxBuyOrSell) {
				maxBuy = 4500;
				maxSell = 4500;
			}
			String side = "buy";
			String direction = "做多";
			long szNum = maxBuy;
			logger.info("最大购买数量{};最大可卖数量:{}", maxBuy, maxSell);
			if (rsi12 > highRsi) {
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
			placeOrder.setSz(szNum + "");
			JSONObject orderSync = pvClient
					.executeSync(this.tradeApi.placeOrder(JSONObject.parseObject(JSON.toJSONString(placeOrder))));
			JSONArray orderArray = orderSync.getJSONArray(data);
			JSONObject order = orderArray.getJSONObject(0);
			if (order.getIntValue(sCode) == 0) {
				isPosition = true;
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
//		logger.info("difMacd:{}", Arrays.toString(difMacd));
//		logger.info("deaMacd:{}", Arrays.toString(deaMacd));
//		logger.info("histMacd:{}", Arrays.toString(histMacd));
//		logger.info("dHigh:{}", Arrays.toString(dHigh));
//		logger.info("dLow:{}", Arrays.toString(dLow));
//		logger.info("dSar:{}", Arrays.toString(dSar));
//
//		logger.info("dClose:{}", Arrays.toString(dClose));
//		logger.info("dRsi6:{}", Arrays.toString(dRsi6));
//		logger.info("dRsi12:{}", Arrays.toString(dRsi12));
//		logger.info("dRsi24:{}", Arrays.toString(dRsi24));

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

//		// 判断当前是否是拐点
//		if (histMacd[histMacd.length - 6] < 0 && histMacd[histMacd.length - 5] < 0 && histMacd[histMacd.length - 4] < 0
//				&& histMacd[histMacd.length - 3] < 0 && histMacd[histMacd.length - 2] < 0
//				&& histMacd[histMacd.length - 1] > 0) {
//			// 由下降转上升
//			indicatorDto.setMacdTurningPoint(0);
////			logger.info("macd拐点:下降--->上升");
//		}
//		if (histMacd[histMacd.length - 6] > 0 && histMacd[histMacd.length - 5] > 0 && histMacd[histMacd.length - 4] > 0
//				&& histMacd[histMacd.length - 3] > 0 && histMacd[histMacd.length - 2] > 0
//				&& histMacd[histMacd.length - 1] < 0) {
//			// 由上升转下降
//			indicatorDto.setMacdTurningPoint(1);
////			logger.info("macd拐点:上升--->下降");
//		}
//		if (histMacd[histMacd.length - 2] < 0 && histMacd[histMacd.length - 1] > 0) {
//			// 由下降转上升
//			indicatorDto.setSingleMacdTurningPoint(0);
////			logger.info("macd拐点:下降--->上升");
//		}
//		if (histMacd[histMacd.length - 2] > 0 && histMacd[histMacd.length - 1] < 0) {
//			// 由上升转下降
//			indicatorDto.setSingleMacdTurningPoint(1);
////			logger.info("macd拐点:上升--->下降");
//		}
//		double curHigh = dHigh[dHigh.length - 1];
//		double preHigh = dHigh[dHigh.length - 2];
//		double curLow = dLow[dLow.length - 1];
//		double preLow = dLow[dLow.length - 2];
//		double curSar = dSar[dSar.length - 1];
//		double preSar = dSar[dSar.length - 2];
//		if (preHigh < preSar && curLow > curSar) {
//			indicatorDto.setSarTurningPoint(0);
////			logger.info("sar拐点:下降--->上升");
//		}
//		if (preLow > preSar && curHigh < curSar) {
//			indicatorDto.setSarTurningPoint(1);
////			logger.info("sar拐点:上升--->下降");
//		}
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
//		long cTime = uplRatioObject.getLongValue("cTime");

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
//		double pos = uplRatioObject.getDouble("pos");
//		double sar = indicatorDto.getSar();
//		double highPrice = candlesticksArray.getJSONArray(0).getDouble(2);
//		double lowPrice = candlesticksArray.getJSONArray(0).getDouble(3);

//		String trend = ";SAR指标:默认状态";
//		if (lowPrice >= sar) {
//			trend = ";SAR指标:上升趋势" + sar;
//		}
//		if (highPrice <= sar) {
//			trend = ";SAR指标:下降趋势" + sar;
//		}

//		int turningPoint = indicatorDto.getSingleMacdTurningPoint();
//		boolean macdUpFlag = turningPoint == 0;
//		boolean macdLowFlag = turningPoint == 1;

//		JSONObject executeSync = pbClient.executeSync(publicDataApi.getSystemTime());
//		JSONArray sytmTimeData = executeSync.getJSONArray(data);
//		long systmTime = sytmTimeData.getJSONObject(0).getLongValue("ts");
//		boolean timeFlag = (systmTime - cTime) > intBar * 5 * 60 * 1000;// 平仓前至少需要持仓5个周期

		if (uplRatio.compareTo(new BigDecimal(activateYield)) > -1) {
			if (uplRatio.compareTo(highestUplRatio) == 1) {
				highestUplRatio = uplRatio;
				logger.info("highestUplRatio更新，当前为:{};rsi6指数为:{};rsi12指数为:{};rsi24指数为:{}", highestUplRatio, rsi6, rsi12,
						rsi24);
			}
		}
		if (highestUplRatio.compareTo(new BigDecimal(activateYield)) > -1) {
			if (uplRatio.compareTo(highestUplRatio.subtract(new BigDecimal(pullbackYield))) < 1) {
				logger.info("highestUplRatio当前为:{}", highestUplRatio);
				sell();
				logger.info("常规平仓收益率为:{};rsi6指数为:{};rsi12指数为:{};rsi24指数为:{}", uplRatio, rsi6, rsi12, rsi24);
				return;
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
