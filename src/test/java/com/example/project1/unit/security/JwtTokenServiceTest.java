package com.example.project1.unit.security;

import com.example.project1.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * JWT 令牌服务单元测试。
 */
class JwtTokenServiceTest {

    private static final String SECRET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Test
    void shouldGenerateAndParseTokenSuccessfully() {
        JwtTokenService service = new JwtTokenService(SECRET, 3600);

        String token = service.generateToken(1L, "admin");
        Optional<Claims> claimsOptional = service.parse(token);

        Assertions.assertTrue(claimsOptional.isPresent());
        Claims claims = claimsOptional.get();
        Assertions.assertEquals("admin", claims.getSubject());
        Assertions.assertEquals(1, claims.get("userId", Integer.class));
    }

    @Test
    void shouldReturnEmptyWhenTokenInvalid() {
        JwtTokenService service = new JwtTokenService(SECRET, 3600);

        Optional<Claims> claimsOptional = service.parse("invalid-token");

        Assertions.assertTrue(claimsOptional.isEmpty());
    }
}

