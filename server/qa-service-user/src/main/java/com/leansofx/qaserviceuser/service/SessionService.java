package com.leansofx.qaserviceuser.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话令牌管理（PRD 6.2 节）。
 *
 * <p>采用服务端会话方案：随机不可预测的 token -&gt; 患者 ID 映射。
 * 登出时移除映射，令牌立即失效（PRD SEC-05 / SEC-06）。
 */
@Component
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    /**
     * 为指定患者创建会话并返回令牌
     */
    public String createSession(String patientId) {
        String token = generateToken();
        sessions.put(token, patientId);
        return token;
    }

    /**
     * 根据令牌解析患者 ID，令牌无效时返回 null
     */
    public String resolvePatientId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return sessions.get(token);
    }

    /**
     * 注销会话
     */
    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * 生成 32 字节（64 位十六进制）的安全随机令牌
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
