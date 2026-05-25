package com.ctbe.eventflow.mapper;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.model.User;
import org.springframework.stereotype.Component;
@Component public class UserMapper {
    public UserDTO toDTO(User user) {
        return UserDTO.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
            .role(user.getRole()).active(user.isActive()).createdAt(user.getCreatedAt()).build();
    }
}
