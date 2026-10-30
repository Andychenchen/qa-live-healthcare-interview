package com.leansofx.qaserviceuser.exception;

import com.leansofx.qaserviceuser.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器，把各类异常映射为 PRD 第 4.2 节定义的错误码与 HTTP 状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：直接使用其携带的错误码、状态码与文案
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 请求体格式错误（JSON 解析失败、字段类型不匹配等）
     */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.PATIENT_BAD_REQUEST, ErrorCodes.MSG_BAD_REQUEST));
    }

    /**
     * 唯一约束冲突等数据库异常
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCodes.PATIENT_USERNAME_TAKEN, ErrorCodes.MSG_USERNAME_TAKEN));
    }

    /**
     * 未匹配到任何接口或静态资源：返回 404 而非 500
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "请求的资源不存在"));
    }

    /**
     * 兜底异常处理。日志中不输出请求体，避免密码等敏感信息泄露（PRD SEC-02 / SEC-09）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("未预期的异常: {}", e.getClass().getName(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.PATIENT_INTERNAL_ERROR, ErrorCodes.MSG_INTERNAL_ERROR));
    }
}
