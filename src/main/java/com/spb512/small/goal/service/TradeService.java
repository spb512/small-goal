package com.spb512.small.goal.service;

/**
 * @author spb512
 * @date 2022年6月19日 下午9:44:58
 */
public interface TradeService {

    /**
     * 开仓
     */
    void openPosition();

    /**
     * 平仓
     */
    void closePosition();

    /**
     * 检查持仓
     */
    void checkPosition();

}
