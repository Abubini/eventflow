// src/test/java/com/ctbe/eventflow/security/JwtUtilsTest.java
package com.ctbe.eventflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    // 64-char hex string = 32 bytes = valid HMAC-SHA256 key
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 86400000L; // 24h

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtils.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtUtils.generateToken(email);
        assertThat(jwtUtils.getEmailFromToken(token)).isEqualTo(email);
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtils.generateToken("user@example.com");
        assertThat(jwtUtils.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtils.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(jwtUtils.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtils.validateToken("")).isFalse();
    }

    @Test
    void validateToken_randomString_returnsFalse() {
        assertThat(jwtUtils.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void getExpiryFromToken_returnsDateInFuture() {
        String token = jwtUtils.generateToken("user@example.com");
        LocalDateTime expiry = jwtUtils.getExpiryFromToken(token);
        assertThat(expiry).isAfter(LocalDateTime.now());
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtUtils.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String t1 = jwtUtils.generateToken("a@example.com");
        String t2 = jwtUtils.generateToken("b@example.com");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtUtils shortLivedUtils = new JwtUtils();
        ReflectionTestUtils.setField(shortLivedUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(shortLivedUtils, "jwtExpirationMs", 1L); // 1ms

        String token = shortLivedUtils.generateToken("user@example.com");
        Thread.sleep(50);
        assertThat(shortLivedUtils.validateToken(token)).isFalse();
    }
}