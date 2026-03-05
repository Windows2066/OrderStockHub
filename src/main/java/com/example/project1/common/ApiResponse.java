package com.example.project1.common;

/**
 * 统一响应体。
 *
 * @param <T> data 字段的数据类型
 */
public class ApiResponse<T> {

    /**
     * 业务响应码，成功固定为0，其它值表示失败。
     */
    private int code;

    /**
     * 响应说明信息。
     */
    private String message;

    /**
     * 实际业务数据。
     */
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "OK", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

