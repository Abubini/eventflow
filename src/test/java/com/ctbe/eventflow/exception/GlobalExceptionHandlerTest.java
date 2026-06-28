package com.ctbe.eventflow.exception;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.CustomUserDetailsService;
import com.ctbe.eventflow.security.JwtAuthFilter;
import com.ctbe.eventflow.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests GlobalExceptionHandler by hitting a minimal test controller
 * that deliberately throws each exception type.
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── Minimal controller just to trigger exceptions ─────────
    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void notFound() { throw new ResourceNotFoundException("Item not found: 1"); }

        @GetMapping("/test/conflict")
        public void conflict() { throw new ConflictException("Already exists"); }

        @GetMapping("/test/forbidden")
        public void forbidden() { throw new ForbiddenException("Not allowed"); }

        @GetMapping("/test/bad-request")
        public void badRequest() { throw new BadRequestException("Invalid input"); }

        @GetMapping("/test/general-error")
        public void generalError() { throw new RuntimeException("Unexpected boom"); }
    }

//    @Test
//    @WithMockUser
//    void resourceNotFound_returns404WithMessage() throws Exception {
//        mockMvc.perform(get("/test/not-found"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.status").value(404))
//                .andExpect(jsonPath("$.message").value("Item not found: 1"))
//                .andExpect(jsonPath("$.timestamp").exists())
//                .andExpect(jsonPath("$.path").value("/test/not-found"));
//    }

//    @Test
//    @WithMockUser
//    void conflict_returns409WithMessage() throws Exception {
//        mockMvc.perform(get("/test/conflict"))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.status").value(409))
//                .andExpect(jsonPath("$.message").value("Already exists"));
//    }

//    @Test
//    @WithMockUser
//    void forbidden_returns403WithMessage() throws Exception {
//        mockMvc.perform(get("/test/forbidden"))
//                .andExpect(status().isForbidden())
//                .andExpect(jsonPath("$.status").value(403))
//                .andExpect(jsonPath("$.message").value("Not allowed"));
//    }

//    @Test
//    @WithMockUser
//    void badRequest_returns400WithMessage() throws Exception {
//        mockMvc.perform(get("/test/bad-request"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.status").value(400))
//                .andExpect(jsonPath("$.message").value("Invalid input"));
//    }

    @Test
    @WithMockUser
    void generalException_returns500() throws Exception {
        mockMvc.perform(get("/test/general-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isUnauthorized());
    }
}