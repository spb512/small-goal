package com.spb512.small.goal.trade;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.spb512.small.goal.service.TradeService;

/** 
* @author spb512
* @date 2022年6月21日 下午7:59:28 
* 
*/
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TradeTest {
	
	@Autowired
	private TradeService tradeService;
	
	@Test
	public void testOpenPosition() {
		tradeService.openPosition();
	}

	@Test
	public void testClosePosition() {
		tradeService.closePosition();
	}
}
