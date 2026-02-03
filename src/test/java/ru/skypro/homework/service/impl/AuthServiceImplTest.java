package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.mapper.UserMapper;
import ru.skypro.homework.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private Register registerDto;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        registerDto = new Register();
        registerDto.setUsername("test@test.com");
        registerDto.setPassword("password123");
        registerDto.setFirstName("Иван");
        registerDto.setLastName("Иванов");
        registerDto.setPhone("+79991234567");
        registerDto.setRole(Role.USER);

        userEntity = new UserEntity();
        userEntity.setEmail("test@test.com");
        userEntity.setPassword("encodedPassword");
        userEntity.setFirstName("Иван");
        userEntity.setLastName("Иванов");
        userEntity.setPhone("+79991234567");
        userEntity.setRole(Role.USER);
    }

    @Test
    void login_ValidCredentials_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        // Act
        boolean result = authService.login("test@test.com", "password123");

        // Assert
        assertTrue(result);
    }

    @Test
    void login_InvalidPassword_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword"))
                .thenReturn(false);

        // Act
        boolean result = authService.login("test@test.com", "wrongpassword");

        // Assert
        assertFalse(result);
    }

    @Test
    void login_UserNotFound_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        // Act
        boolean result = authService.login("unknown@test.com", "password123");

        // Assert
        assertFalse(result);
    }

    @Test
    void register_NewUser_ShouldReturnTrueAndSaveUser() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userMapper.toEntity(registerDto)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        boolean result = authService.register(registerDto);

        // Assert
        assertTrue(result);
        assertEquals("encodedPassword", userEntity.getPassword());
        verify(userRepository).save(userEntity);
    }

    @Test
    void register_UserAlreadyExists_ShouldReturnFalse() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // Act
        boolean result = authService.register(registerDto);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_NoRoleSpecified_ShouldSetDefaultRole() {
        // Arrange
        registerDto.setRole(null);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userMapper.toEntity(registerDto)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        authService.register(registerDto);

        // Assert
        assertEquals(Role.USER, userEntity.getRole());
    }

    @Test
    void register_ExceptionDuringSave_ShouldReturnFalse() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userMapper.toEntity(registerDto)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        boolean result = authService.register(registerDto);

        // Assert
        assertFalse(result);
    }
}