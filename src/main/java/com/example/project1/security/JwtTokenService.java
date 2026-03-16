package com.example.project1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 令牌服务。
 *
 * 负责签发和解析访问令牌，给接口鉴权提供统一能力。
 */
@Service
public class JwtTokenService {

    /**
     * 用于 HMAC 签名的密钥。
     */
    private final SecretKey secretKey;

    /**
     * 令牌过期秒数。
     */
    private final long expireSeconds;

    public JwtTokenService(@Value("${app.security.jwt.secret}") String secret,
                           @Value("${app.security.jwt.expire-seconds:7200}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成 JWT。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验并解析 JWT。
     *
     * @param token JWT 字符串
     * @return 解析成功时返回声明，失败时返回 empty
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}

