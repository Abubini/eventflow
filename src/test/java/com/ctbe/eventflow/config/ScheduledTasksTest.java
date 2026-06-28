package com.ctbe.eventflow.config;

import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksTest {

    @Mock TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks ScheduledTasks scheduledTasks;

    @Test
    void purgeExpiredTokens_callsRepositoryWithCurrentTime() {
        scheduledTasks.purgeExpiredTokens();

        verify(tokenBlacklistRepository, times(1))
                .deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    void purgeExpiredTokens_passesCurrentOrPastTime() {
        LocalDateTime before = LocalDateTime.now();

        scheduledTasks.purgeExpiredTokens();

        ArgumentCaptor<LocalDateTime> cap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenBlacklistRepository).deleteExpiredTokens(cap.capture());

        // The time passed must be now or very slightly after "before"
        assertThat(cap.getValue()).isAfterOrEqualTo(before);
    }

    @Test
    void purgeExpiredTokens_doesNotThrowWhenRepositorySucceeds() {
        doNothing().when(tokenBlacklistRepository).deleteExpiredTokens(any());

        org.assertj.core.api.Assertions
                .assertThatCode(() -> scheduledTasks.purgeExpiredTokens())
                .doesNotThrowAnyException();
    }

    @Test
    void purgeExpiredTokens_callsRepositoryExactlyOnce() {
        scheduledTasks.purgeExpiredTokens();
        scheduledTasks.purgeExpiredTokens();

        verify(tokenBlacklistRepository, times(2)).deleteExpiredTokens(any());
    }
}