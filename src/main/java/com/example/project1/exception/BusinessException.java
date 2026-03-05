package com.example.project1.exception;

/**
 * 业务异常。
 *
 * 用于承载可预期的业务失败（例如库存不足、库存不存在）。
 */
public class BusinessException extends RuntimeException {

    /**
     * 自定义业务错误码。
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

