/* JWT 工具测试 */
package com.freshmart.util;

import com.freshmart.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具测试
 */
class JwtUtilTest {

    private static final String SECRET = "freshmart-unit-test-secret-key-0123456789";
    private static final long TTL = 3_600_000L;

    @Test
    @DisplayName("签发的 token 可以被解析，且业务字段不丢失")
    void createAndParse() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", 1L);
        claims.put("name", "admin");

        String token = JwtUtil.createJWT(SECRET, TTL, claims);
        Claims parsed = JwtUtil.parseJWT(SECRET, token);

        assertEquals(1, ((Number) parsed.get("empId")).intValue());
        assertEquals("admin", parsed.get("name"));
    }

    @Test
    @DisplayName("过期 token 解析时抛出 ExpiredJwtException")
    void parseExpiredToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", 1L);

        String expired = JwtUtil.createJWT(SECRET, -1_000L, claims);

        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseJWT(SECRET, expired));
    }
}
