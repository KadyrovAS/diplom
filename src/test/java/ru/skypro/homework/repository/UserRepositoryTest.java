package ru.skypro.homework.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.dto.Role;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void findByEmail_ExistingEmail_ShouldReturnUser() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        user.setFirstName("Иван");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        // Act
        Optional<UserEntity> result = userRepository.findByEmail("test@test.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@test.com", result.get().getEmail());
        assertEquals("Иван", result.get().getFirstName());
    }

    @Test
    void existsByEmail_ExistingEmail_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // Act
        boolean result = userRepository.existsByEmail("test@test.com");

        // Assert
        assertTrue(result);
    }
}