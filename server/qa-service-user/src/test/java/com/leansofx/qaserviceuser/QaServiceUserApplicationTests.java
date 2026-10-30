package com.leansofx.qaserviceuser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用上下文加载测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("应用上下文测试")
class QaServiceUserApplicationTests {

	@Test
	void contextLoads() {
	}

}
