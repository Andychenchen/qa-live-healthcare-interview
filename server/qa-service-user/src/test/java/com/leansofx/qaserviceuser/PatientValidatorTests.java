package com.leansofx.qaserviceuser;

import com.leansofx.qaserviceuser.dto.LoginRequest;
import com.leansofx.qaserviceuser.dto.RegisterRequest;
import com.leansofx.qaserviceuser.exception.BusinessException;
import com.leansofx.qaserviceuser.exception.ErrorCodes;
import com.leansofx.qaserviceuser.service.PatientValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 患者入参校验规则测试（PRD T-03 / 第 4.1 节）。
 *
 * <p>纯单元测试，不启动 Spring 上下文，用于固化后端校验规则，
 * 防止前后端规则漂移（PRD R-2）。
 */
@DisplayName("患者入参校验测试")
class PatientValidatorTests {

    private final PatientValidator validator = new PatientValidator();

    // ==================== 用户名 ====================

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "user001", "user_name", "user-name", "a1b2c3d4"})
    @DisplayName("合法用户名通过校验")
    void validUsernames(String username) {
        assertDoesNotThrow(() -> validator.validateRegister(register(username, "qa123456", "张三")));
    }

    @Test
    @DisplayName("32 字符的用户名通过校验（边界值）")
    void usernameMaxLength() {
        assertDoesNotThrow(() -> validator.validateRegister(register("a".repeat(32), "qa123456", "张三")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "   ", "user name", "user@name", "用户名", "user.name"})
    @DisplayName("非法用户名返回 PATIENT_USERNAME_INVALID")
    void invalidUsernames(String username) {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register(username, "qa123456", "张三")));
        assertEquals(ErrorCodes.PATIENT_USERNAME_INVALID, e.getCode());
    }

    @Test
    @DisplayName("33 字符的用户名被拒绝（边界值）")
    void usernameTooLong() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register("a".repeat(33), "qa123456", "张三")));
        assertEquals(ErrorCodes.PATIENT_USERNAME_INVALID, e.getCode());
    }

    // ==================== 密码 ====================

    @ParameterizedTest
    @ValueSource(strings = {"qa123456", "abcdefg1", "12345678a", "Passw0rd", "qa12345!"})
    @DisplayName("合法密码通过校验")
    void validPasswords(String password) {
        assertDoesNotThrow(() -> validator.validateRegister(register("user001", password, "张三")));
    }

    @Test
    @DisplayName("32 字符的密码通过校验（边界值）")
    void passwordMaxLength() {
        assertDoesNotThrow(() -> validator.validateRegister(register("user001", "a1" + "b".repeat(30), "张三")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "abcdefgh", "short1A", "qa 123456"})
    @DisplayName("非法密码返回 PATIENT_PASSWORD_INVALID")
    void invalidPasswords(String password) {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register("user001", password, "张三")));
        assertEquals(ErrorCodes.PATIENT_PASSWORD_INVALID, e.getCode());
    }

    @Test
    @DisplayName("33 字符的密码被拒绝（边界值）")
    void passwordTooLong() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register("user001", "a".repeat(33), "张三")));
        assertEquals(ErrorCodes.PATIENT_PASSWORD_INVALID, e.getCode());
    }

    // ==================== 姓名 ====================

    @ParameterizedTest
    @ValueSource(strings = {"张三", "Tom", "欧阳明", "a"})
    @DisplayName("合法姓名通过校验")
    void validNames(String name) {
        assertDoesNotThrow(() -> validator.validateRegister(register("user001", "qa123456", name)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("非法姓名返回 PATIENT_NAME_INVALID")
    void invalidNames(String name) {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register("user001", "qa123456", name)));
        assertEquals(ErrorCodes.PATIENT_NAME_INVALID, e.getCode());
    }

    @Test
    @DisplayName("33 字符的姓名被拒绝（边界值）")
    void nameTooLong() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(register("user001", "qa123456", "a".repeat(33))));
        assertEquals(ErrorCodes.PATIENT_NAME_INVALID, e.getCode());
    }

    // ==================== 生日 ====================

    @ParameterizedTest
    @ValueSource(strings = {"1990-01-01", "2000-12-31", "1985-03-15"})
    @DisplayName("合法生日通过校验")
    void validBirthdays(String birthday) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setBirthday(birthday);
        assertDoesNotThrow(() -> validator.validateRegister(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1990/01/01", "19900101", "1990-1-1", "not-a-date", "2999-01-01"})
    @DisplayName("非法生日返回 PATIENT_BIRTHDAY_INVALID")
    void invalidBirthdays(String birthday) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setBirthday(birthday);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(request));
        assertEquals(ErrorCodes.PATIENT_BIRTHDAY_INVALID, e.getCode());
    }

    // ==================== 手机号 ====================

    @ParameterizedTest
    @ValueSource(strings = {"13800138000", "19912345678", "15012345678"})
    @DisplayName("合法手机号通过校验")
    void validPhones(String phone) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setPhone(phone);
        assertDoesNotThrow(() -> validator.validateRegister(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345678901", "2380013800", "abc", "1380013800a"})
    @DisplayName("非法手机号返回 PATIENT_PHONE_INVALID")
    void invalidPhones(String phone) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setPhone(phone);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(request));
        assertEquals(ErrorCodes.PATIENT_PHONE_INVALID, e.getCode());
    }

    // ==================== 性别 ====================

    @ParameterizedTest
    @ValueSource(strings = {"男", "女", "其他"})
    @DisplayName("合法性别通过校验")
    void validGenders(String gender) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setGender(gender);
        assertDoesNotThrow(() -> validator.validateRegister(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "male", "female", "1"})
    @DisplayName("非法性别返回 PATIENT_GENDER_INVALID")
    void invalidGenders(String gender) {
        RegisterRequest request = register("user001", "qa123456", "张三");
        request.setGender(gender);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateRegister(request));
        assertEquals(ErrorCodes.PATIENT_GENDER_INVALID, e.getCode());
    }

    // ==================== 登录校验 ====================

    @ParameterizedTest
    @CsvSource({"'',qa123456", "user001,''", "'',''", "'   ',qa123456"})
    @DisplayName("登录入参为空返回 PATIENT_LOGIN_FAILED（不区分具体原因）")
    void invalidLoginInput(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validateLogin(request));
        assertEquals(ErrorCodes.PATIENT_LOGIN_FAILED, e.getCode());
        assertEquals(401, e.getStatus());
    }

    @Test
    @DisplayName("登录入参合法时通过校验")
    void validLoginInput() {
        LoginRequest request = new LoginRequest();
        request.setUsername("user001");
        request.setPassword("qa123456");
        assertDoesNotThrow(() -> validator.validateLogin(request));
    }

    // ==================== 工具方法 ====================

    private RegisterRequest register(String username, String password, String name) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setName(name);
        return request;
    }
}
