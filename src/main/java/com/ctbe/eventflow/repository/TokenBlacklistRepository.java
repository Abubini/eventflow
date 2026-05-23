package com.ctbe.eventflow.repository;
import com.ctbe.eventflow.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist,Long> {
    boolean existsByToken(String token);
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
