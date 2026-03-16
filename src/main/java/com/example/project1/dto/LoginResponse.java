package com.example.project1.dto;

/**
 * 登录响应参数。
 */
public class LoginResponse {

    /** JWT 访问令牌。 */
    private String token;

    /** 令牌有效期（秒）。 */
    private long expireSeconds;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expireSeconds) {
        this.token = token;
        this.expireSeconds = expireSeconds;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}

