package com.spb512.small.goal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.okex.open.api.bean.account.param.SetLeverage;
import com.okex.open.api.bean.account.param.SetPositionMode;
import com.okex.open.api.bean.trade.param.ClosePositions;
import com.okex.open.api.bean.trade.param.PlaceOrder;
import com.okex.open.api.client.APIClient;
import com.okex.open.api.service.account.impl.AccountAPI;
import com.okex.open.api.service.marketData.impl.MarketDataAPI;
import com.okex.open.api.service.publicData.impl.PublicDataAPI;
import com.okex.open.api.service.trade.impl.TradeAPI;
import com.spb512.small.goal.dto.IndicatorDto;
import com.spb512.small.goal.service.TradeService;
import com.spb512.small.goal.utils.PrivateClient;
import com.spb512.small.goal.utils.PublicClient;
import com.spb512.small.goal.utils.talib.FinStratEntity;
import com.spb512.small.goal.utils.talib.FinStratModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author spb512
 * @date 2022年6月19日 下午9:46:37
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

    private APIClient pvClient;

    private APIClient pbClient;

    private AccountAPI accountApi;

    private TradeAPI tradeApi;

    private MarketDataAPI marketDataApi;

    private PublicDataAPI publicDataApi;

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
    private int intBar = 3;
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
    /**
     * 最高做空点
     */
    private double highestHighRsi = 0;
    /**
     * 最低做空点
     */
    private double lowestLowRsi = 100;
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
    private double activateRatio = 0.05;
    /**
     * 回调收益率
     */
    private double pullbackRatio = 0.001;
    /**
     * 强制止损线
     */
    private double stopLossLine = -0.08;
    /**
     * rsi12做空激活点
     */
    private double activateHighRsi12 = 80;
    /**
     * rsi12做多激活点
     */
    private double activateLowRsi12 = 20;
    /**
     * 回调开仓点
     */
    private double pullbackRsi = 0.01;
    /**
     * 做多
     */
    private boolean doBuy = false;
    /**
     * 做空
     */
    private boolean doSell = false;

    /**
     * 需要减少(最大交易数量大于实际数量，需要减1)
     */
    private boolean needReduce = false;
    /**
     * 持仓方式
     * long_short_mode：双向持仓 net_mode：单向持仓
     * 仅适用交割/永续
     */
    private String posMode = "net_mode";

    @PostConstruct
    public void init() {
        pvClient = privateClient.getClient();
        pbClient = publicClient.getClient();
        if (accountApi == null) {
            accountApi = pvClient.createService(AccountAPI.class);
            // 初始化持仓模式
            SetPositionMode setPositionMode = new SetPositionMode();
            setPositionMode.setPosMode(posMode);
            pvClient.executeSync(accountApi.setPositionMode(JSONObject.parseObject(JSON.toJSONString(setPositionMode))));
            logger.info("初始化持仓模式为单向持仓成功");
            // 初始化杠杆倍速
            SetLeverage setLeverage = new SetLeverage();
            setLeverage.setInstId(instId);
            setLeverage.setLever(lever);
            setLeverage.setMgnMode(mode);
            pvClient.executeSync(accountApi.setLeverage(JSONObject.parseObject(JSON.toJSONString(setLeverage))));
            logger.info("初始化杠杆倍速为{}成功", lever);
            //初始化持仓标记
            JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
            JSONArray jsonArray = positionsObject.getJSONArray(data);
            if (!jsonArray.isEmpty()) {
                isPosition = true;
                logger.info("当前有持仓，初始化持仓标记isPosition为true成功");
            }
        }
        if (tradeApi == null) {
            tradeApi = pvClient.createService(TradeAPI.class);
        }

        if (marketDataApi == null) {
            marketDataApi = pbClient.createService(MarketDataAPI.class);
        }
        if (publicDataApi == null) {
            publicDataApi = pbClient.createService(PublicDataAPI.class);
            //初始化市价交易单次最大买和卖的数量
            JSONObject instrumentsSync = pbClient.executeSync(publicDataApi.getInstruments(instType, null, instId));
            maxBuyOrSell = instrumentsSync.getJSONArray(data).getJSONObject(0).getLongValue("maxMktSz");
            logger.info("初始化市价交易单次最大买和卖的数量为{}成功", maxBuyOrSell);
        }
    }

    @Override
    public synchronized void openPosition() {
        // 当前是否有持仓
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
//		double rsi24 = indicatorDto.getRsi24();
//		double[] rsi12Arr = indicatorDto.getRsi12Arr();

        if ((rsi12 > activateHighRsi12) && (rsi12 > highestHighRsi)) {
            highestHighRsi = rsi12;
            logger.info("highestHighRsi更新，当前为:{}", highestHighRsi);
        }
        if ((highestHighRsi > activateHighRsi12) && (highestHighRsi - rsi12 > pullbackRsi)) {
            doSell = true;
        }

        if ((rsi12 < activateLowRsi12) && (rsi12 < lowestLowRsi)) {
            lowestLowRsi = rsi12;
            logger.info("lowestLowRsi更新，当前为:{}", lowestLowRsi);
        }
        if ((lowestLowRsi < activateLowRsi12) && (rsi12 - lowestLowRsi > pullbackRsi)) {
            doBuy = true;
        }
        if (doBuy || doSell) {
            //再次确认是否有持仓
            JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
            JSONArray jsonArray = positionsObject.getJSONArray(data);
            if (!jsonArray.isEmpty()) {
                return;
            }
            // 查询账户余额
            JSONObject balanceObject = pvClient.executeSync(accountApi.getBalance(ccy));
            JSONArray balanceArray = balanceObject.getJSONArray(data);
            JSONObject jsonObject = balanceArray.getJSONObject(0);
            JSONArray detailsArray = jsonObject.getJSONArray("details");
            JSONObject usdtBalance = detailsArray.getJSONObject(0);
            BigDecimal usdtCashBal = usdtBalance.getBigDecimal("cashBal");
            if (usdtCashBal.compareTo(new BigDecimal(minStartup)) < 0) {
                doBuy = false;
                doSell = false;
                logger.info("账号余额:{},余额过低小于{}", usdtCashBal, minStartup);
                return;
            }
            // 获取最大开仓数量
            JSONObject maxImun = pvClient
                    .executeSync(accountApi.getMaximumTradableSizeForInstrument(instId, mode, null, null, null, null));
            JSONArray maxInumArray = maxImun.getJSONArray(data);
            JSONObject maxInumObject = maxInumArray.getJSONObject(0);
            long maxBuy = maxInumObject.getLongValue("maxBuy");
            long maxSell = maxInumObject.getLongValue("maxSell");

            if (maxBuy > maxBuyOrSell || maxSell > maxBuyOrSell) {
                maxBuy = maxBuyOrSell;
                maxSell = maxBuyOrSell;
            }
            if (needReduce) {
                maxBuy--;
                maxSell--;
            }
            String side = "buy";
            String direction = "做多";
            String szNum = maxBuy + "";
            logger.info("最大购买数量{};最大可卖数量:{}", maxBuy, maxSell);
            if (doSell) {
                side = "sell";
                szNum = maxSell + "";
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
            int resultCode = order.getIntValue(sCode);
            if (resultCode == 0) {
                isPosition = true;
                lowestLowRsi = 100;
                highestHighRsi = 0;
                doBuy = false;
                doSell = false;
                needReduce = false;
            } else if (resultCode == 51008) {
                needReduce = true;
            }
            logger.info("开{}仓,订单号ordId:{};执行结果sCode:{};执行信息sMsg:{}=======>当前余额:{}", direction, order.getString("ordId"),
                    order.getString(sCode), order.getString("sMsg"), usdtCashBal);
        }
    }

    private IndicatorDto getIndicators(JSONArray candlesticksArray) {
        int j = Integer.parseInt(limit) - 1;
        for (int i = 0; i < candlesticksArray.size(); i++) {
            JSONArray jsonArray = candlesticksArray.getJSONArray(i);
//			dHigh[j] = jsonArray.getDouble(2);
//			dLow[j] = jsonArray.getDouble(3);
            dClose[j] = jsonArray.getDouble(4);
            j--;
        }
//		FinStratEntity finEntity = finModel.calSar(dHigh, dLow, 0.02, 0.2);
//		double dSar[] = finEntity.getSarReal();

//		FinStratEntity rsi6FinEntity = finModel.calRsi(dClose, 6);
//		double dRsi6[] = rsi6FinEntity.getSarReal();
        FinStratEntity rsi12FinEntity = finModel.calRsi(dClose, 12);
        double[] dRsi12 = rsi12FinEntity.getRsiReal();
//		FinStratEntity rsi24FinEntity = finModel.calRsi(dClose, 24);
//		double dRsi24[] = rsi24FinEntity.getSarReal();

//		FinStratEntity calMacd = finModel.calMacd(dClose, 12, 26, 9);
//		double[] difMacd = calMacd.getDifMacd();
//		double[] deaMacd = calMacd.getDeaMacd();
//		double[] histMacd = calMacd.getHistMacd();

        IndicatorDto indicatorDto = new IndicatorDto();
//		indicatorDto.setSar(dSar[dSar.length - 1]);
//		indicatorDto.setRsi6(dRsi6[dRsi6.length - 1]);
        indicatorDto.setRsi12(dRsi12[dRsi12.length - 1]);
//		indicatorDto.setRsi24(dRsi24[dRsi24.length - 1]);
//		indicatorDto.setDiff(difMacd[difMacd.length - 1]);
//		indicatorDto.setDea(deaMacd[deaMacd.length - 1]);
//		indicatorDto.setStick(histMacd[histMacd.length - 1]);
//		indicatorDto.setSarTurningPoint(-1);
//		indicatorDto.setMacdTurningPoint(-1);
//		indicatorDto.setRsi6Arr(new double[] { dRsi6[dRsi6.length - 5], dRsi6[dRsi6.length - 4],
//				dRsi6[dRsi6.length - 3], dRsi6[dRsi6.length - 2], dRsi6[dRsi6.length - 1] });
//		indicatorDto.setRsi12Arr(
//				new double[] { dRsi12[dRsi12.length - 3], dRsi12[dRsi12.length - 2], dRsi12[dRsi12.length - 1] });
        return indicatorDto;
    }

    /**
     * 平仓
     */
    @Override
    public void closePosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
        JSONArray jsonArray = positionsObject.getJSONArray(data);
        if (jsonArray.isEmpty()) {
            return;
        }
        JSONObject uplRatioObject = jsonArray.getJSONObject(0);
        BigDecimal uplRatio = uplRatioObject.getBigDecimal("uplRatio");

        // 判断是否达到止盈止损条件
        if ((uplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio) > 0)) {
            highestUplRatio = uplRatio;
            logger.info("highestUplRatio更新，当前为:{}", highestUplRatio);
        }
        if ((highestUplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio.subtract(BigDecimal.valueOf(pullbackRatio))) < 1)) {
            sell();
            logger.info("平仓收益率为:{}", uplRatio);
            logger.info("<=====================分隔符=======================>");
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
        logger.info("平仓操作code:{};msg:{};当前余额:{}", closePosition.getString(code),
                closePosition.getString("msg"), usdtCashBal);
    }

    @Override
    public synchronized void checkPosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        JSONObject positionsObject = pvClient.executeSync(accountApi.getPositions(instType, null, null));
        JSONArray jsonArray = positionsObject.getJSONArray(data);
        if (jsonArray.isEmpty()) {
            return;
        }
        JSONObject uplRatioObject = jsonArray.getJSONObject(0);
        BigDecimal uplRatio = uplRatioObject.getBigDecimal("uplRatio");
        if (uplRatio.compareTo(BigDecimal.valueOf(stopLossLine)) < 0) {
            logger.info("达到强制止损线{}%", stopLossLine * 100);
            sell();
            logger.info("当前收益率{}", uplRatio);
            logger.info("<=====================分隔符=======================>");
        }
    }
}
