// src/test/java/com/ctbe/eventflow/repository/TokenBlacklistRepositoryTest.java
package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.TokenBlacklist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TokenBlacklistRepositoryTest {

    @Autowired TokenBlacklistRepository tokenBlacklistRepository;

    @Test
    void existsByToken_existingToken_returnsTrue() {
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token("some.jwt.token")
                .expiresAt(LocalDateTime.now().plusHours(1)).build());

        assertThat(tokenBlacklistRepository.existsByToken("some.jwt.token")).isTrue();
    }

    @Test
    void existsByToken_nonExistent_returnsFalse() {
        assertThat(tokenBlacklistRepository.existsByToken("ghost.token")).isFalse();
    }

    @Test
    void deleteExpiredTokens_removesOnlyExpired() {
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token("expired.token")
                .expiresAt(LocalDateTime.now().minusHours(1)).build()); // expired
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token("valid.token")
                .expiresAt(LocalDateTime.now().plusHours(1)).build()); // still valid

        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());

        assertThat(tokenBlacklistRepository.existsByToken("expired.token")).isFalse();
        assertThat(tokenBlacklistRepository.existsByToken("valid.token")).isTrue();
    }

    @Test
    void deleteExpiredTokens_noExpiredTokens_deletesNothing() {
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token("valid.token")
                .expiresAt(LocalDateTime.now().plusHours(2)).build());

        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());

        assertThat(tokenBlacklistRepository.count()).isEqualTo(1L);
    }
}