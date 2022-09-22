package com.spb512.small.goal.task;

import com.okex.open.api.exception.APIException;
import com.spb512.small.goal.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author spb512
 * @date 2022年6月22日 上午9:57:48
 */
@Component
@EnableScheduling
@EnableAsync
@Async(value = "applicationTaskExecutor")
public class TradTask {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private TradeService tradeService;

    /**
     * 每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void openPositionTask() {
//		logger.info("开仓");
//		long timeMillis = System.currentTimeMillis();
//		logger.info("开仓方法开始:{}", timeMillis);
        try {
            tradeService.openPosition();
        } catch (APIException e) {
            logger.info("开仓任务捕获api异常:{}", e.getMessage());
        }
//		logger.info("开仓方法结束:{},耗时:{}毫秒", System.currentTimeMillis(), System.currentTimeMillis() - timeMillis);
    }

    /**
     * 每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void closePositionTask() {
//		logger.info("平仓");
//		long timeMillis = System.currentTimeMillis();
//		logger.info("平仓方法开始:{}", timeMillis);
        try {
            tradeService.closePosition();
        } catch (APIException e) {
            logger.info("平仓任务捕获api异常:{}", e.getMessage());
        }
//		logger.info("平仓方法结束:{},耗时:{}毫秒", System.currentTimeMillis(), System.currentTimeMillis() - timeMillis);
    }

    /**
     * 每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void checkPositionTask() {
//		logger.info("检查持仓");
//		long timeMillis = System.currentTimeMillis();
//		logger.info("检查方法开始:{}", timeMillis);
        try {
            tradeService.checkPosition();
        } catch (APIException e) {
            logger.info("止损任务捕获api异常:{}", e.getMessage());
        }
//		logger.info("检查方法结束:{},耗时:{}毫秒", System.currentTimeMillis(), System.currentTimeMillis() - timeMillis);
    }

}
