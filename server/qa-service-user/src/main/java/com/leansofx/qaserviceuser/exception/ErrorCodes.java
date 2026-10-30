package com.leansofx.qaserviceuser.exception;

/**
 * 业务错误码与用户可见文案（PRD 第 4.2 节）。
 *
 * <p>登录失败必须统一为「用户名或密码错误」，禁止区分「用户不存在」与「密码错误」，
 * 以避免账号枚举攻击（PRD SEC-03）。
 */
public final class ErrorCodes {

    // ==================== 校验类（400） ====================

    public static final String PATIENT_USERNAME_INVALID = "PATIENT_USERNAME_INVALID";
    public static final String MSG_USERNAME_INVALID = "用户名需为 4-32 位，仅支持字母、数字、下划线和连字符";

    public static final String PATIENT_PASSWORD_INVALID = "PATIENT_PASSWORD_INVALID";
    public static final String MSG_PASSWORD_INVALID = "密码需为 8-32 位，且必须同时包含字母和数字";

    public static final String PATIENT_NAME_INVALID = "PATIENT_NAME_INVALID";
    public static final String MSG_NAME_INVALID = "请输入姓名";

    public static final String PATIENT_BIRTHDAY_INVALID = "PATIENT_BIRTHDAY_INVALID";
    public static final String MSG_BIRTHDAY_INVALID = "生日格式应为 YYYY-MM-DD";

    public static final String PATIENT_PHONE_INVALID = "PATIENT_PHONE_INVALID";
    public static final String MSG_PHONE_INVALID = "请输入有效的手机号";

    public static final String PATIENT_GENDER_INVALID = "PATIENT_GENDER_INVALID";
    public static final String MSG_GENDER_INVALID = "性别取值不合法";

    public static final String PATIENT_BAD_REQUEST = "PATIENT_BAD_REQUEST";
    public static final String MSG_BAD_REQUEST = "请求参数不合法";

    // ==================== 冲突类（409） ====================

    public static final String PATIENT_USERNAME_TAKEN = "PATIENT_USERNAME_TAKEN";
    public static final String MSG_USERNAME_TAKEN = "该用户名已被占用，请更换";

    // ==================== 认证类（401） ====================

    /** 用户名不存在与密码错误共用此错误码与文案 */
    public static final String PATIENT_LOGIN_FAILED = "PATIENT_LOGIN_FAILED";
    public static final String MSG_LOGIN_FAILED = "用户名或密码错误";

    public static final String PATIENT_UNAUTHORIZED = "PATIENT_UNAUTHORIZED";
    public static final String MSG_UNAUTHORIZED = "请先登录";

    public static final String PATIENT_TOKEN_INVALID = "PATIENT_TOKEN_INVALID";
    public static final String MSG_TOKEN_INVALID = "登录已过期，请重新登录";

    // ==================== 限流类（429） ====================

    public static final String PATIENT_LOGIN_LOCKED = "PATIENT_LOGIN_LOCKED";
    public static final String MSG_LOGIN_LOCKED = "登录尝试过于频繁，请 5 分钟后再试";

    // ==================== 服务端异常（500） ====================

    public static final String PATIENT_INTERNAL_ERROR = "PATIENT_INTERNAL_ERROR";
    public static final String MSG_INTERNAL_ERROR = "服务异常，请稍后重试";

    private ErrorCodes() {
    }
}
