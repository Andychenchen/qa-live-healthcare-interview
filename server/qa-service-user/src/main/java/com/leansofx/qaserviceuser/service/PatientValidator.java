package com.leansofx.qaserviceuser.service;

import com.leansofx.qaserviceuser.dto.LoginRequest;
import com.leansofx.qaserviceuser.dto.RegisterRequest;
import com.leansofx.qaserviceuser.exception.BusinessException;
import com.leansofx.qaserviceuser.exception.ErrorCodes;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 患者注册/登录入参校验（PRD 第 4.1 节 V-01 ~ V-10）。
 *
 * <p>服务端独立执行全部校验，不信任前端；前端校验仅为体验优化。
 */
@Component
public class PatientValidator {

    /** 用户名：4-32 位，字母/数字/下划线/连字符 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");

    /** 密码：8-32 位，必须同时包含字母和数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[\\S]{8,32}$");

    /** 手机号：中国大陆 11 位手机号 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 性别枚举 */
    private static final Set<String> GENDERS = Set.of("男", "女", "其他");

    /**
     * 校验注册请求，任一字段不合法即抛出 BusinessException（400）
     */
    public void validateRegister(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodes.PATIENT_BAD_REQUEST, ErrorCodes.MSG_BAD_REQUEST, 400);
        }

        // V-01 用户名
        if (isBlank(request.getUsername())) {
            throw new BusinessException(ErrorCodes.PATIENT_USERNAME_INVALID, "请输入用户名", 400);
        }
        if (!USERNAME_PATTERN.matcher(request.getUsername().trim()).matches()) {
            throw new BusinessException(ErrorCodes.PATIENT_USERNAME_INVALID, ErrorCodes.MSG_USERNAME_INVALID, 400);
        }

        // V-02 密码
        if (isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCodes.PATIENT_PASSWORD_INVALID, "请输入密码", 400);
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BusinessException(ErrorCodes.PATIENT_PASSWORD_INVALID, ErrorCodes.MSG_PASSWORD_INVALID, 400);
        }

        // V-03 姓名
        if (isBlank(request.getName())) {
            throw new BusinessException(ErrorCodes.PATIENT_NAME_INVALID, ErrorCodes.MSG_NAME_INVALID, 400);
        }
        if (request.getName().trim().length() > 32) {
            throw new BusinessException(ErrorCodes.PATIENT_NAME_INVALID, ErrorCodes.MSG_NAME_INVALID, 400);
        }

        // V-04 生日（选填）
        if (!isBlank(request.getBirthday())) {
            validateBirthday(request.getBirthday().trim());
        }

        // V-05 手机号（选填）
        if (!isBlank(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException(ErrorCodes.PATIENT_PHONE_INVALID, ErrorCodes.MSG_PHONE_INVALID, 400);
            }
        }

        // V-06 性别（选填）
        if (!isBlank(request.getGender())) {
            String gender = request.getGender().trim();
            if (!GENDERS.contains(gender)) {
                throw new BusinessException(ErrorCodes.PATIENT_GENDER_INVALID, ErrorCodes.MSG_GENDER_INVALID, 400);
            }
        }
    }

    /**
     * 校验登录请求。用户名为空或密码为空统一返回登录失败文案，
     * 避免泄露账号是否存在（PRD SEC-03）。
     */
    public void validateLogin(LoginRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCodes.PATIENT_LOGIN_FAILED, ErrorCodes.MSG_LOGIN_FAILED, 401);
        }
    }

    /**
     * 校验生日格式：YYYY-MM-DD 且不得晚于当天
     */
    private void validateBirthday(String birthday) {
        try {
            LocalDate parsed = LocalDate.parse(birthday);
            if (parsed.isAfter(LocalDate.now())) {
                throw new BusinessException(ErrorCodes.PATIENT_BIRTHDAY_INVALID, ErrorCodes.MSG_BIRTHDAY_INVALID, 400);
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCodes.PATIENT_BIRTHDAY_INVALID, ErrorCodes.MSG_BIRTHDAY_INVALID, 400);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
