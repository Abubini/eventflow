// src/test/java/com/ctbe/eventflow/repository/UserRepositoryTest.java
package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    @Test
    void findByEmail_existingUser_returnsUser() {
        userRepository.save(User.builder().name("Alice").email("alice@test.com")
                .passwordHash("hash").role(UserRole.ATTENDEE).active(true).build());

        Optional<User> found = userRepository.findByEmail("alice@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void findByEmail_nonExistent_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nobody@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(User.builder().name("Bob").email("bob@test.com")
                .passwordHash("hash").role(UserRole.ATTENDEE).active(true).build());

        assertThat(userRepository.existsByEmail("bob@test.com")).isTrue();
    }

    @Test
    void existsByEmail_nonExistentEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@test.com")).isFalse();
    }

    @Test
    void save_persistsAllFields() {
        User user = User.builder().name("Carol").email("carol@test.com")
                .passwordHash("$2a$bcrypt").role(UserRole.ORGANIZER).active(true).build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(UserRole.ORGANIZER);
    }
}