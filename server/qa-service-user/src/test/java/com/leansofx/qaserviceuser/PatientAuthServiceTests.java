package com.leansofx.qaserviceuser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansofx.qaserviceuser.dto.AuthResponse;
import com.leansofx.qaserviceuser.dto.LoginRequest;
import com.leansofx.qaserviceuser.dto.PatientResponse;
import com.leansofx.qaserviceuser.dto.RegisterRequest;
import com.leansofx.qaserviceuser.entity.Patient;
import com.leansofx.qaserviceuser.exception.BusinessException;
import com.leansofx.qaserviceuser.exception.ErrorCodes;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import com.leansofx.qaserviceuser.service.LoginAttemptService;
import com.leansofx.qaserviceuser.service.PatientAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 患者认证服务测试（PRD T-03）。
 *
 * <p>覆盖 PRD 第 4.1 节校验规则与第 5 节接口契约要求的核心场景：
 * 注册成功、用户名重复、密码哈希存储、登录成功、密码错误、用户不存在（响应一致）、
 * 限流触发与解除、登出后 token 失效。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("患者认证服务测试")
class PatientAuthServiceTests {

    @Autowired
    private PatientAuthService patientAuthService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        patientRepository.deleteAll();
        loginAttemptService.reset("__reset__");
    }

    // ==================== 注册 ====================

    @Test
    @DisplayName("注册成功：返回令牌与患者信息，且密码以 BCrypt 哈希存储")
    void registerSuccess() {
        String username = uniqueUsername();
        RegisterRequest request = registerRequest(username, "qa123456", "张三");

        AuthResponse response = patientAuthService.register(request);

        // 返回令牌与患者信息
        assertNotNull(response.token());
        assertFalse(response.token().isBlank());
        assertNotNull(response.patient());
        assertEquals(username, response.patient().getUsername());
        assertEquals("张三", response.patient().getName());

        // 密码以 BCrypt 哈希存储，绝不是明文
        Patient saved = patientRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertNotEquals("qa123456", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2a$"), "密码应为 BCrypt 哈希");
        assertEquals(60, saved.getPassword().length());
        assertTrue(passwordEncoder.matches("qa123456", saved.getPassword()));

        // 响应中不含密码字段
        assertFalse(response.patient().toString().contains("qa123456"));
    }

    @Test
    @DisplayName("注册成功：密码哈希具有随机盐，同一密码两次注册得到不同哈希")
    void registerUsesRandomSalt() {
        String username1 = uniqueUsername();
        String username2 = uniqueUsername();

        patientAuthService.register(registerRequest(username1, "qa123456", "李四"));
        patientAuthService.register(registerRequest(username2, "qa123456", "王五"));

        String hash1 = patientRepository.findByUsernameIgnoreCase(username1).orElseThrow().getPassword();
        String hash2 = patientRepository.findByUsernameIgnoreCase(username2).orElseThrow().getPassword();

        assertNotEquals(hash1, hash2, "BCrypt 应使用随机盐，两次哈希不应相同");
    }

    @Test
    @DisplayName("用户名重复：抛出 409 PATIENT_USERNAME_TAKEN")
    void registerDuplicateUsername() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "赵六"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest(username, "qa123456", "赵六")));

        assertEquals(ErrorCodes.PATIENT_USERNAME_TAKEN, e.getCode());
        assertEquals(409, e.getStatus());
        assertEquals(1, patientRepository.count());
    }

    @Test
    @DisplayName("用户名重复：大小写不敏感，Patient001 与 patient001 视为同一账号")
    void registerDuplicateUsernameCaseInsensitive() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "钱七"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(
                        registerRequest(username.toUpperCase(), "qa123456", "钱七")));

        assertEquals(ErrorCodes.PATIENT_USERNAME_TAKEN, e.getCode());
    }

    // ==================== 校验规则 ====================

    @Test
    @DisplayName("注册校验：用户名过短返回 PATIENT_USERNAME_INVALID")
    void registerInvalidUsername() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest("ab", "qa123456", "张三")));

        assertEquals(ErrorCodes.PATIENT_USERNAME_INVALID, e.getCode());
        assertEquals(400, e.getStatus());
    }

    @Test
    @DisplayName("注册校验：用户名含非法字符返回 PATIENT_USERNAME_INVALID")
    void registerUsernameWithIllegalChars() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest("张 三", "qa123456", "张三")));

        assertEquals(ErrorCodes.PATIENT_USERNAME_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：密码不含字母返回 PATIENT_PASSWORD_INVALID")
    void registerPasswordWithoutLetter() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest(uniqueUsername(), "12345678", "张三")));

        assertEquals(ErrorCodes.PATIENT_PASSWORD_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：密码不含数字返回 PATIENT_PASSWORD_INVALID")
    void registerPasswordWithoutDigit() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest(uniqueUsername(), "abcdefgh", "张三")));

        assertEquals(ErrorCodes.PATIENT_PASSWORD_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：密码过短返回 PATIENT_PASSWORD_INVALID")
    void registerPasswordTooShort() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest(uniqueUsername(), "a1b2", "张三")));

        assertEquals(ErrorCodes.PATIENT_PASSWORD_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：姓名为空返回 PATIENT_NAME_INVALID")
    void registerEmptyName() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(registerRequest(uniqueUsername(), "qa123456", "")));

        assertEquals(ErrorCodes.PATIENT_NAME_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：手机号非法返回 PATIENT_PHONE_INVALID")
    void registerInvalidPhone() {
        RegisterRequest request = registerRequest(uniqueUsername(), "qa123456", "张三");
        request.setPhone("123");

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(request));

        assertEquals(ErrorCodes.PATIENT_PHONE_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：生日格式非法返回 PATIENT_BIRTHDAY_INVALID")
    void registerInvalidBirthday() {
        RegisterRequest request = registerRequest(uniqueUsername(), "qa123456", "张三");
        request.setBirthday("1990/01/01");

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(request));

        assertEquals(ErrorCodes.PATIENT_BIRTHDAY_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：性别取值非法返回 PATIENT_GENDER_INVALID")
    void registerInvalidGender() {
        RegisterRequest request = registerRequest(uniqueUsername(), "qa123456", "张三");
        request.setGender("X");

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.register(request));

        assertEquals(ErrorCodes.PATIENT_GENDER_INVALID, e.getCode());
    }

    @Test
    @DisplayName("注册校验：选填字段留空时可正常注册")
    void registerWithOptionalFieldsOmitted() {
        String username = uniqueUsername();
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("qa123456");
        request.setName("张三");

        AuthResponse response = patientAuthService.register(request);

        assertNotNull(response.token());
        Patient saved = patientRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertNull(saved.getBirthday());
        assertNull(saved.getPhone());
        assertNull(saved.getGender());
    }

    // ==================== 登录 ====================

    @Test
    @DisplayName("登录成功：返回令牌与患者信息")
    void loginSuccess() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        AuthResponse response = patientAuthService.login(loginRequest(username, "qa123456"));

        assertNotNull(response.token());
        assertFalse(response.token().isBlank());
        assertEquals(username, response.patient().getUsername());
        assertEquals("张三", response.patient().getName());
    }

    @Test
    @DisplayName("登录成功：用户名大小写不敏感")
    void loginCaseInsensitiveUsername() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        AuthResponse response = patientAuthService.login(loginRequest(username.toUpperCase(), "qa123456"));

        assertEquals(username, response.patient().getUsername());
    }

    @Test
    @DisplayName("登录失败：密码错误返回 PATIENT_LOGIN_FAILED")
    void loginWithWrongPassword() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest(username, "wrongpass")));

        assertEquals(ErrorCodes.PATIENT_LOGIN_FAILED, e.getCode());
        assertEquals(401, e.getStatus());
        assertEquals(ErrorCodes.MSG_LOGIN_FAILED, e.getMessage());
    }

    @Test
    @DisplayName("登录失败：用户不存在与密码错误返回完全一致的错误码与文案（防账号枚举）")
    void loginNonExistentUserRespondsIdentically() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest(username, "wrongpass")));
        BusinessException noSuchUser = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest(uniqueUsername(), "qa123456")));

        assertEquals(wrongPassword.getCode(), noSuchUser.getCode());
        assertEquals(wrongPassword.getMessage(), noSuchUser.getMessage());
        assertEquals(wrongPassword.getStatus(), noSuchUser.getStatus());
    }

    @Test
    @DisplayName("登录失败：用户名为空返回 PATIENT_LOGIN_FAILED")
    void loginWithEmptyUsername() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest("", "qa123456")));

        assertEquals(ErrorCodes.PATIENT_LOGIN_FAILED, e.getCode());
    }

    // ==================== 限流 ====================

    @Test
    @DisplayName("限流：连续失败达到阈值后返回 429 PATIENT_LOGIN_LOCKED")
    void loginLockedAfterRepeatedFailures() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        // 前 5 次为普通的凭证错误
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> patientAuthService.login(loginRequest(username, "wrongpass")));
            assertEquals(ErrorCodes.PATIENT_LOGIN_FAILED, e.getCode());
        }

        // 第 6 次触发锁定
        BusinessException locked = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest(username, "wrongpass")));
        assertEquals(ErrorCodes.PATIENT_LOGIN_LOCKED, locked.getCode());
        assertEquals(429, locked.getStatus());
    }

    @Test
    @DisplayName("限流：锁定期间即使密码正确也被拒绝")
    void loginLockedRejectsCorrectPassword() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertThrows(BusinessException.class,
                    () -> patientAuthService.login(loginRequest(username, "wrongpass")));
        }

        BusinessException locked = assertThrows(BusinessException.class,
                () -> patientAuthService.login(loginRequest(username, "qa123456")));
        assertEquals(ErrorCodes.PATIENT_LOGIN_LOCKED, locked.getCode());
    }

    @Test
    @DisplayName("限流：登录成功后失败计数被清空")
    void loginSuccessResetsFailureCounter() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        // 先失败 3 次
        for (int i = 0; i < 3; i++) {
            assertThrows(BusinessException.class,
                    () -> patientAuthService.login(loginRequest(username, "wrongpass")));
        }

        // 一次成功登录后计数清零
        patientAuthService.login(loginRequest(username, "qa123456"));

        // 再失败 5 次不应触发锁定（第 6 次才锁定）
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> patientAuthService.login(loginRequest(username, "wrongpass")));
            assertEquals(ErrorCodes.PATIENT_LOGIN_FAILED, e.getCode());
        }
    }

    @Test
    @DisplayName("限流：锁定过期后自动解除")
    void loginLockExpires() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertThrows(BusinessException.class,
                    () -> patientAuthService.login(loginRequest(username, "wrongpass")));
        }
        assertTrue(loginAttemptService.isLocked(username));

        // 模拟锁定过期：重置计数后应能正常登录
        loginAttemptService.reset(username);
        assertFalse(loginAttemptService.isLocked(username));

        AuthResponse response = patientAuthService.login(loginRequest(username, "qa123456"));
        assertNotNull(response.token());
    }

    // ==================== 会话 ====================

    @Test
    @DisplayName("登出后 token 立即失效，/me 返回 PATIENT_TOKEN_INVALID")
    void logoutInvalidatesToken() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        AuthResponse login = patientAuthService.login(loginRequest(username, "qa123456"));
        String token = login.token();

        // 登出前 /me 可用
        PatientResponse before = patientAuthService.currentPatient(token);
        assertEquals(username, before.getUsername());

        // 登出
        patientAuthService.logout(token);

        // 登出后 /me 失效
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.currentPatient(token));
        assertEquals(ErrorCodes.PATIENT_TOKEN_INVALID, e.getCode());
        assertEquals(401, e.getStatus());
    }

    @Test
    @DisplayName("会话：每次登录生成不同的随机令牌")
    void loginGeneratesUniqueToken() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        String token1 = patientAuthService.login(loginRequest(username, "qa123456")).token();
        String token2 = patientAuthService.login(loginRequest(username, "qa123456")).token();

        assertNotNull(token1);
        assertNotNull(token2);
        assertFalse(token1.equals(token2), "每次登录应生成不同的令牌");
        assertEquals(64, token1.length(), "令牌应为 32 字节的十六进制字符串");
    }

    @Test
    @DisplayName("会话：无效令牌查询 /me 返回 PATIENT_TOKEN_INVALID")
    void meWithInvalidToken() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> patientAuthService.currentPatient("invalid-token"));

        assertEquals(ErrorCodes.PATIENT_TOKEN_INVALID, e.getCode());
        assertEquals(401, e.getStatus());
    }

    @Test
    @DisplayName("会话：me 返回的患者信息不含密码字段")
    void meResponseHasNoPassword() {
        String username = uniqueUsername();
        patientAuthService.register(registerRequest(username, "qa123456", "张三"));

        AuthResponse login = patientAuthService.login(loginRequest(username, "qa123456"));
        PatientResponse me = patientAuthService.currentPatient(login.token());

        String json = me.toString();
        assertFalse(json.contains("qa123456"), "响应中不应出现明文密码");
        assertFalse(json.contains("$2a$"), "响应中不应出现密码哈希");
        assertFalse(json.toLowerCase().contains("password"), "响应中不应包含 password 字段");
    }

    // ==================== 工具方法 ====================

    /**
     * 生成唯一用户名，避免用例之间相互干扰
     */
    private String uniqueUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private RegisterRequest registerRequest(String username, String password, String name) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setName(name);
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
