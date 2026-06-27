package com.ctbe.eventflow;

import com.ctbe.eventflow.service.EmailService;
import com.resend.Resend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventflowApplicationTests {

    // Mock the Resend bean so no real HTTP call is attempted
    @MockBean
    private Resend resend;

    // Mock EmailService entirely so no email logic runs during context load
    @MockBean
    private EmailService emailService;

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts without errors
    }
}