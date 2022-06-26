package com.spb512.small.goal.task;

import javax.annotation.Resource;

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

//	private static final Logger LOG = LoggerFactory.getLogger(TradTask.class);

	@Resource
	private TradeService tradeService;

	/**
	 * 每43秒执行一次
	 */
	@Scheduled(cron = "0/43 * * * * ?")
	public void openPositionTask() {
//		LOG.info("开仓定时任务");
		tradeService.openPosition();
	}

	/**
	 * 每27秒执行一次
	 */
	@Scheduled(cron = "0/27 * * * * ?")
	public void closePositionTask() {
//		LOG.info("平仓定时任务");
		tradeService.closePosition();
	}

}
