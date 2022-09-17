package com.spb512.small.goal.trade;

import com.spb512.small.goal.service.TradeService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;

/**
 * @author spb512
 * @date 2022年6月21日 下午7:59:28
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
class TradeTests {

    @Resource
    private TradeService tradeService;

    @Test
    void testOpenPosition() {
        tradeService.openPosition();
    }

    @Test
    void testClosePosition() {
        tradeService.closePosition();
    }
}
