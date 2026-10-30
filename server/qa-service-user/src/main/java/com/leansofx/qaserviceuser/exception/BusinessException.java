package com.leansofx.qaserviceuser.exception;

/**
 * 业务异常，携带 PRD 第 4.2 节定义的错误码与 HTTP 状态码。
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final int status;

    public BusinessException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
