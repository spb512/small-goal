package com.spb512.small.goal.task;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.spb512.small.goal.service.TradeService;

/**
 * @author spb512
 * @date 2022年6月22日 上午9:57:48
 * 
 */
@Component
@EnableScheduling
@EnableAsync
@Async
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
		tradeService.openPosition();
	}

	/**
	 * 每1秒执行一次
	 */
	@Scheduled(cron = "0/1 * * * * ?")
	public void closePositionTask() {
//		logger.info("平仓");
		tradeService.closePosition();
	}

	/**
	 * 每1秒执行一次
	 */
	@Scheduled(cron = "0/1 * * * * ?")
	public void checkPositionTask() {
//		logger.info("检查持仓");
		tradeService.checkPosition();
	}

}
