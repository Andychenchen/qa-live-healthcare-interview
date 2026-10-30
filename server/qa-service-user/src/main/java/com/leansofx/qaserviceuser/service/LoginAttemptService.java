package com.leansofx.qaserviceuser.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数与账号锁定（PRD REQ-005 / V-09）。
 *
 * <p>以「用户名」为维度记录连续失败次数与最后失败时间；达到阈值后锁定一段时间。
 * 登录成功或锁定过期后计数清零。
 */
@Component
public class LoginAttemptService {

    /** 连续失败次数阈值 */
    public static final int MAX_ATTEMPTS = 5;

    /** 锁定时长（分钟） */
    public static final int LOCK_MINUTES = 5;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 时钟，抽象便于测试中模拟时间流逝 */
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemDefaultZone());
    }

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 记录一次登录失败
     */
    public void recordFailure(String username) {
        String key = normalize(username);
        attempts.compute(key, (k, existing) -> {
            Instant now = clock.instant();
            if (existing == null || existing.isExpired(now)) {
                return new Attempt(1, now);
            }
            return new Attempt(existing.count() + 1, now);
        });
    }

    /**
     * 判断该用户名当前是否处于锁定状态
     */
    public boolean isLocked(String username) {
        Attempt attempt = attempts.get(normalize(username));
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired(clock.instant())) {
            attempts.remove(normalize(username));
            return false;
        }
        return attempt.count() >= MAX_ATTEMPTS;
    }

    /**
     * 登录成功后清空失败计数
     */
    public void reset(String username) {
        attempts.remove(normalize(username));
    }

    /**
     * 获取当前失败次数（供测试与日志使用）
     */
    public int failureCount(String username) {
        Attempt attempt = attempts.get(normalize(username));
        if (attempt == null || attempt.isExpired(clock.instant())) {
            return 0;
        }
        return attempt.count();
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /**
     * 一次失败记录
     */
    private static final class Attempt {

        private final int count;
        private final Instant lastFailureAt;

        Attempt(int count, Instant lastFailureAt) {
            this.count = count;
            this.lastFailureAt = lastFailureAt;
        }

        int count() {
            return count;
        }

        /** 距离上次失败是否已超过锁定期 */
        boolean isExpired(Instant now) {
            return Duration.between(lastFailureAt, now).toMinutes() >= LOCK_MINUTES;
        }
    }
}
