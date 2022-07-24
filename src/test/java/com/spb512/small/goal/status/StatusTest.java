package com.spb512.small.goal.status;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.spb512.small.goal.service.StatusService;

/**
 * @author spb512
 * @date 2022年6月6日 下午11:01:53
 * 
 */

@RunWith(SpringRunner.class)
@SpringBootTest
//由于是Web项目，Junit需要模拟ServletContext，因此我们需要给我们的测试类加上@WebAppConfiguration。
@WebAppConfiguration
public class StatusTest {

	@Autowired
	private StatusService statusServic;

	@Test
	public void testGetStatus() {
		statusServic.getStatus("completed");
	}
}
