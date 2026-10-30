package com.leansofx.qaserviceuser;

import com.leansofx.qaserviceuser.service.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录失败计数与账号锁定测试（PRD T-03 / REQ-005）。
 */
@DisplayName("登录限流测试")
class LoginAttemptServiceTests {

    /**
     * 可推进的测试时钟，用于模拟时间流逝
     */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    @DisplayName("未失败过时账号未被锁定")
    void notLockedInitially() {
        LoginAttemptService service = new LoginAttemptService(Clock.systemDefaultZone());
        assertFalse(service.isLocked("user001"));
        assertEquals(0, service.failureCount("user001"));
    }

    @Test
    @DisplayName("失败次数达到阈值后账号被锁定")
    void lockedAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService(Clock.systemDefaultZone());

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertFalse(service.isLocked("user001"), "第 " + (i + 1) + " 次失败后不应锁定");
            service.recordFailure("user001");
        }
        assertTrue(service.isLocked("user001"));
        assertEquals(LoginAttemptService.MAX_ATTEMPTS, service.failureCount("user001"));
    }

    @Test
    @DisplayName("登录成功后清空失败计数")
    void resetClearsCount() {
        LoginAttemptService service = new LoginAttemptService(Clock.systemDefaultZone());
        service.recordFailure("user001");
        service.recordFailure("user001");
        assertEquals(2, service.failureCount("user001"));

        service.reset("user001");

        assertEquals(0, service.failureCount("user001"));
        assertFalse(service.isLocked("user001"));
    }

    @Test
    @DisplayName("不同用户名之间互不影响")
    void countersAreIsolatedPerUsername() {
        LoginAttemptService service = new LoginAttemptService(Clock.systemDefaultZone());
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure("user001");
        }
        assertTrue(service.isLocked("user001"));
        assertFalse(service.isLocked("user002"));
    }

    @Test
    @DisplayName("用户名大小写不敏感地累计失败次数")
    void counterIsCaseInsensitive() {
        LoginAttemptService service = new LoginAttemptService(Clock.systemDefaultZone());
        service.recordFailure("User001");
        service.recordFailure("user001");

        assertEquals(2, service.failureCount("USER001"));
    }

    @Test
    @DisplayName("锁定过期后自动解除锁定，失败计数归零")
    void lockExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure("user001");
        }
        assertTrue(service.isLocked("user001"));

        // 推进到锁定期之后
        clock.advance(Duration.ofMinutes(LoginAttemptService.LOCK_MINUTES + 1));

        assertFalse(service.isLocked("user001"), "锁定期过后应自动解除锁定");
        assertEquals(0, service.failureCount("user001"), "锁定期过后失败计数应归零");
    }

    @Test
    @DisplayName("锁定过期后重新失败会从 1 重新计数")
    void failureCountRestartsAfterExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure("user001");
        }
        assertTrue(service.isLocked("user001"));

        clock.advance(Duration.ofMinutes(LoginAttemptService.LOCK_MINUTES + 1));
        service.recordFailure("user001");

        assertEquals(1, service.failureCount("user001"));
        assertFalse(service.isLocked("user001"));
    }
}
