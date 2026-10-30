package com.leansofx.qaserviceuser;

import com.leansofx.qaserviceuser.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会话令牌管理测试（PRD T-03 / 6.2 节）。
 */
@DisplayName("会话令牌测试")
class SessionServiceTests {

    @Test
    @DisplayName("创建会话后可由令牌解析出主体 ID")
    void createAndResolve() {
        SessionService service = new SessionService();
        String token = service.createSession("patient001");

        assertNotNull(token);
        assertEquals("patient001", service.resolvePatientId(token));
    }

    @Test
    @DisplayName("令牌为 64 位十六进制字符串（32 字节随机数）")
    void tokenFormat() {
        SessionService service = new SessionService();
        String token = service.createSession("patient001");

        assertEquals(64, token.length());
        assertTrue(token.matches("^[0-9a-f]{64}$"));
    }

    @Test
    @DisplayName("每次创建的令牌都不同")
    void tokensAreUnique() {
        SessionService service = new SessionService();
        String token1 = service.createSession("patient001");
        String token2 = service.createSession("patient001");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("注销会话后令牌立即失效")
    void invalidateRemovesSession() {
        SessionService service = new SessionService();
        String token = service.createSession("patient001");

        service.invalidate(token);

        assertNull(service.resolvePatientId(token));
    }

    @Test
    @DisplayName("非法令牌解析为 null")
    void invalidTokenResolvesToNull() {
        SessionService service = new SessionService();

        assertNull(service.resolvePatientId("not-a-real-token"));
        assertNull(service.resolvePatientId(null));
        assertNull(service.resolvePatientId(""));
        assertNull(service.resolvePatientId("   "));
    }

    @Test
    @DisplayName("注销不存在的令牌不抛异常")
    void invalidateUnknownTokenIsSafe() {
        SessionService service = new SessionService();

        service.invalidate(null);
        service.invalidate("unknown");
    }
}
