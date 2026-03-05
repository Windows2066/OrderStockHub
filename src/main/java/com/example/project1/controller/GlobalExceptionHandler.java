package com.example.project1.controller;

import com.example.project1.common.ApiResponse;
import com.example.project1.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 统一将参数错误、业务错误、系统错误转换为结构化响应，
 * 便于前端和调用方稳定处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验失败（@Valid）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不合法"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(4001, message);
    }

    /**
     * 处理普通参数绑定错误（如 query/path 参数类型不匹配）。
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数绑定失败"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(4002, message);
    }

    /**
     * 处理约束校验异常（通常来自 @Validated + 参数级校验）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return ApiResponse.fail(4003, ex.getMessage());
    }

    /**
     * 处理请求体格式错误（例如 JSON 结构不合法）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ApiResponse.fail(4004, "请求体格式错误，请检查 JSON 内容");
    }

    /**
     * 处理业务异常（如库存不足）。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 兜底处理其它未捕获异常。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(5000, "系统繁忙，请稍后重试");
    }
}

