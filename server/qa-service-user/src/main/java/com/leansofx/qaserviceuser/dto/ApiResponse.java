package com.leansofx.qaserviceuser.dto;

/**
 * 统一 API 响应结构（PRD 5.3 节）。
 *
 * @param code    业务错误码，成功为 OK
 * @param message 用户可见文案
 * @param data    业务数据，失败时为 null
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("OK", message, data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
