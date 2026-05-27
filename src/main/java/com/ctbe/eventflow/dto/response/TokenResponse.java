package com.ctbe.eventflow.dto.response;
import lombok.Builder; import lombok.Data;
@Data @Builder public class TokenResponse {
    private String token; private String type; private Long expiresIn; private UserDTO user;
}
