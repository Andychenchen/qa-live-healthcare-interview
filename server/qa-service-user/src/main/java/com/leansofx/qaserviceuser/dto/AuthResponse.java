package com.leansofx.qaserviceuser.dto;

/**
 * 登录成功后的响应体（PRD 5.3.1）。
 *
 * @param token    会话令牌，前端持久化到 localStorage
 * @param patient  当前患者信息（不含 password）
 */
public record AuthResponse(String token, PatientResponse patient) {
}
