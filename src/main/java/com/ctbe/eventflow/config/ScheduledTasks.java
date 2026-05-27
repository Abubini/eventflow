package com.ctbe.eventflow.config;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Component @RequiredArgsConstructor
public class ScheduledTasks {
    private static final Logger log=LoggerFactory.getLogger(ScheduledTasks.class);
    private final TokenBlacklistRepository tokenBlacklistRepository;
    @Scheduled(cron="0 0 2 * * *") @Transactional
    public void purgeExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Purged expired JWT blacklist tokens");
    }
}
