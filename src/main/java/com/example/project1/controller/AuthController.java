package com.example.project1.controller;

import com.example.project1.common.ApiResponse;
import com.example.project1.dto.LoginRequest;
import com.example.project1.dto.LoginResponse;
import com.example.project1.exception.BusinessException;
import com.example.project1.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权控制器。
 *
 * 当前先提供最小登录能力，后续可以平滑替换为数据库用户体系。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;

    @Value("${app.security.login.username}")
    private String configuredUsername;

    @Value("${app.security.login.password}")
    private String configuredPassword;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 登录并返回 JWT。
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        if (!configuredUsername.equals(request.getUsername()) || !configuredPassword.equals(request.getPassword())) {
            throw new BusinessException(4011, "用户名或密码错误");
        }

        // 当前演示环境用固定用户ID=1，后续接用户表时替换。
        String token = jwtTokenService.generateToken(1L, request.getUsername());
        LoginResponse response = new LoginResponse(token, jwtTokenService.getExpireSeconds());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

