package com.leansofx.qaserviceuser.controller;

import com.leansofx.qaserviceuser.dto.ApiResponse;
import com.leansofx.qaserviceuser.dto.AuthResponse;
import com.leansofx.qaserviceuser.dto.LoginRequest;
import com.leansofx.qaserviceuser.dto.PatientResponse;
import com.leansofx.qaserviceuser.dto.RegisterRequest;
import com.leansofx.qaserviceuser.exception.BusinessException;
import com.leansofx.qaserviceuser.exception.ErrorCodes;
import com.leansofx.qaserviceuser.service.PatientAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者认证接口（PRD 第 5 节）。
 *
 * <p>跨域由现有 CorsConfig 统一处理，无需额外配置。
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PatientAuthService patientAuthService;

    public PatientController(PatientAuthService patientAuthService) {
        this.patientAuthService = patientAuthService;
    }

    /**
     * POST /api/patients/register —— 注册患者账号并返回登录态
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody(required = false) RegisterRequest request) {
        AuthResponse response = patientAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("注册成功", response));
    }

    /**
     * POST /api/patients/login —— 校验凭证并返回登录态
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody(required = false) LoginRequest request) {
        AuthResponse response = patientAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("登录成功", response));
    }

    /**
     * POST /api/patients/logout —— 注销当前会话
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        patientAuthService.logout(extractToken(authorization));
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/patients/me —— 查询当前登录患者信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponse>> me(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        if (token == null) {
            throw new BusinessException(ErrorCodes.PATIENT_UNAUTHORIZED, ErrorCodes.MSG_UNAUTHORIZED, 401);
        }
        PatientResponse patient = patientAuthService.currentPatient(token);
        return ResponseEntity.ok(ApiResponse.ok(patient));
    }

    /**
     * 从 Authorization 头中提取 Bearer token
     */
    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
