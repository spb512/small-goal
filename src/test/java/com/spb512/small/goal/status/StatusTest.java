package com.spb512.small.goal.status;

import com.spb512.small.goal.service.StatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;

/**
 * 由于是Web项目，Junit需要模拟ServletContext，因此我们需要给我们的测试类加上@WebAppConfiguration。
 *
 * @author spb512
 * @date 2022年6月6日 下午11:01:53
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class StatusTest {

    @Resource
    private StatusService statusServic;

    @Test
    void testGetStatus() {
        statusServic.getStatus("completed");
    }
}
