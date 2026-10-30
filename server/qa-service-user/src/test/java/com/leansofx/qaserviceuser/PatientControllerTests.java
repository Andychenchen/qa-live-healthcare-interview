package com.leansofx.qaserviceuser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 患者认证接口集成测试（PRD T-03）。
 *
 * <p>通过 MockMvc 验证 REST 层契约：HTTP 状态码、统一响应结构、错误码、认证头处理。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("患者认证接口测试")
class PatientControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    // ==================== 注册接口 ====================

    @Test
    @DisplayName("POST /api/patients/register：注册成功返回 201 与统一响应结构")
    void registerSuccess() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", "qa123456",
                                "name", "张三",
                                "birthday", "1990-01-01",
                                "phone", "13800138000",
                                "gender", "男"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.patient.username").value(username))
                .andExpect(jsonPath("$.data.patient.name").value("张三"))
                .andExpect(jsonPath("$.data.patient.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/patients/register：用户名重复返回 409")
    void registerDuplicate() throws Exception {
        String username = uniqueUsername();
        String body = json(Map.of("username", username, "password", "qa123456", "name", "张三"));

        mockMvc.perform(post("/api/patients/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_USERNAME_TAKEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/patients/register：入参不合法返回 400 及对应错误码")
    void registerValidationErrors() throws Exception {
        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "ab", "password", "qa123456", "name", "张三"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PATIENT_USERNAME_INVALID"));

        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", uniqueUsername(), "password", "12345678", "name", "张三"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PATIENT_PASSWORD_INVALID"));

        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", uniqueUsername(), "password", "qa123456", "name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PATIENT_NAME_INVALID"));
    }

    // ==================== 登录接口 ====================

    @Test
    @DisplayName("POST /api/patients/login：登录成功返回 200 与令牌")
    void loginSuccess() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "qa123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.patient.username").value(username))
                .andExpect(jsonPath("$.data.patient.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/patients/login：密码错误返回 401 PATIENT_LOGIN_FAILED")
    void loginWrongPassword() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "wrongpass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PATIENT_LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/patients/login：用户不存在与密码错误的响应体完全一致")
    void loginNonExistentUserIdenticalResponse() throws Exception {
        String username = uniqueUsername();
        register(username);

        MvcResult wrongPassword = mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "wrongpass"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult noSuchUser = mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", uniqueUsername(), "password", "qa123456"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertEquals(wrongPassword.getResponse().getContentAsString(),
                noSuchUser.getResponse().getContentAsString(),
                "两个场景的响应体应逐字节一致，避免账号枚举");
    }

    // ==================== /me 与 /logout ====================

    @Test
    @DisplayName("GET /api/patients/me：携带有效 Bearer 令牌返回当前患者")
    void meWithValidToken() throws Exception {
        String username = uniqueUsername();
        register(username);

        String token = login(username);

        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/patients/me：未携带令牌返回 401 PATIENT_UNAUTHORIZED")
    void meWithoutToken() throws Exception {
        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PATIENT_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/patients/me：非法令牌返回 401 PATIENT_TOKEN_INVALID")
    void meWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PATIENT_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("POST /api/patients/logout：登出后原令牌立即失效")
    void logoutInvalidatesToken() throws Exception {
        String username = uniqueUsername();
        register(username);
        String token = login(username);

        mockMvc.perform(post("/api/patients/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/patients/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PATIENT_TOKEN_INVALID"));
    }

    // ==================== 响应体安全 ====================

    @Test
    @DisplayName("响应体：注册与登录响应中均不含明文密码与哈希")
    void responsesNeverLeakPassword() throws Exception {
        String username = uniqueUsername();

        MvcResult registerResult = mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "qa123456", "name", "张三"))))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult loginResult = mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "qa123456"))))
                .andExpect(status().isOk())
                .andReturn();

        for (MvcResult result : new MvcResult[]{registerResult, loginResult}) {
            String body = result.getResponse().getContentAsString();
            assertFalse(body.contains("qa123456"), "响应中不应出现明文密码");
            assertFalse(body.contains("$2a$"), "响应中不应出现密码哈希");
            assertFalse(body.toLowerCase().contains("\"password\""), "响应中不应包含 password 字段");
        }
    }

    @Test
    @DisplayName("回归：既有医生接口不受影响，且不返回 password 字段")
    void doctorsApiStillWorks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"id\""), "医生接口应正常返回数据");
        assertFalse(body.contains("\"password\""), "医生接口不应返回 password 字段");
    }

    // ==================== 工具方法 ====================

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "qa123456", "name", "张三"))))
                .andExpect(status().isCreated());
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "qa123456"))))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(body);
        String token = node.path("data").path("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
        return token;
    }

    private String json(Map<String, String> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private String uniqueUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
