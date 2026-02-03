package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.NewPassword;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.dto.UpdateUser;
import ru.skypro.homework.dto.User;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.exception.BadRequestException;
import ru.skypro.homework.exception.ForbiddenException;
import ru.skypro.homework.exception.NotFoundException;
import ru.skypro.homework.mapper.UserMapper;
import ru.skypro.homework.repository.UserRepository;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileService fileService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity userEntity;
    private User userDto;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setId(1);
        userEntity.setEmail("test@test.com");
        userEntity.setPassword("encodedPassword");
        userEntity.setFirstName("Иван");
        userEntity.setLastName("Иванов");
        userEntity.setPhone("+79991234567");
        userEntity.setRole(Role.USER);
        userEntity.setImage("avatar.jpg");

        userDto = new User();
        userDto.setId(1);
        userDto.setEmail("test@test.com");
        userDto.setFirstName("Иван");
        userDto.setLastName("Иванов");
        userDto.setPhone("+79991234567");
        userDto.setRole(Role.USER);
        userDto.setImage("/users/1/image");
    }

    @Test
    void getCurrentUser_ExistingUser_ShouldReturnUserDto() {
        // Arrange
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        // Act
        User result = userService.getCurrentUser(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Иван", result.getFirstName());
        verify(userRepository).findByEmail("test@test.com");
    }

    @Test
    void getCurrentUser_UserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(authentication.getName()).thenReturn("unknown@test.com");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getCurrentUser(authentication));

        assertTrue(exception.getMessage().contains("Пользователь не найден"));
    }

    @Test
    void updateUser_ValidData_ShouldUpdateAndReturnDto() {
        // Arrange
        UpdateUser updateUser = new UpdateUser();
        updateUser.setFirstName("Петр");
        updateUser.setLastName("Петров");
        updateUser.setPhone("+79998887766");

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Настройка маппера для обновления сущности
        doAnswer(invocation -> {
            UpdateUser dto = invocation.getArgument(0);
            UserEntity entity = invocation.getArgument(1);
            entity.setFirstName(dto.getFirstName());
            entity.setLastName(dto.getLastName());
            entity.setPhone(dto.getPhone());
            return null;
        }).when(userMapper).updateEntity(any(UpdateUser.class), any(UserEntity.class));

        // Act
        UpdateUser result = userService.updateUser(updateUser, authentication);

        // Assert
        assertNotNull(result);
        assertEquals("Петр", result.getFirstName());
        assertEquals("Петров", result.getLastName());
        verify(userMapper).updateEntity(updateUser, userEntity);
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateUser_InvalidFirstName_ShouldThrowBadRequestException() {
        // Arrange
        UpdateUser updateUser = new UpdateUser();
        updateUser.setFirstName("A"); // Too short

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updateUser(updateUser, authentication));

        assertEquals("Имя должно быть от 3 до 10 символов", exception.getMessage());
    }

    @Test
    void updateUser_InvalidPhone_ShouldThrowBadRequestException() {
        // Arrange
        UpdateUser updateUser = new UpdateUser();
        updateUser.setPhone("123456"); // Invalid format

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updateUser(updateUser, authentication));

        assertEquals("Номер телефона должен соответствовать формату: +7 XXX XXX-XX-XX",
                exception.getMessage());
    }

    @Test
    void updatePassword_ValidData_ShouldUpdatePassword() {
        // Arrange
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("oldPassword123");
        newPassword.setNewPassword("newPassword123");

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        // Act
        userService.updatePassword(newPassword, authentication);

        // Assert
        assertEquals("newEncodedPassword", userEntity.getPassword());
        verify(userRepository).save(userEntity);
    }

    @Test
    void updatePassword_WrongCurrentPassword_ShouldThrowForbiddenException() {
        // Arrange
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("wrongPassword");
        newPassword.setNewPassword("newPassword123");

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> userService.updatePassword(newPassword, authentication));

        assertEquals("Текущий пароль неверен", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_SameOldAndNewPassword_ShouldThrowBadRequestException() {
        // Arrange
        NewPassword newPassword = new NewPassword();
        newPassword.setCurrentPassword("oldPassword123");
        newPassword.setNewPassword("oldPassword123");

        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updatePassword(newPassword, authentication));

        assertEquals("Новый пароль должен отличаться от старого", exception.getMessage());
    }

    @Test
    void updateUserImage_ValidImage_ShouldUpdateAvatar() throws IOException {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getSize()).thenReturn(1024L);
        when(image.isEmpty()).thenReturn(false);
        when(fileService.saveImage(image, "users")).thenReturn("new-avatar.jpg");

        // Act
        userService.updateUserImage(image, authentication);

        // Assert
        verify(fileService).deleteImage("users", "avatar.jpg");
        verify(fileService).saveImage(image, "users");
        assertEquals("new-avatar.jpg", userEntity.getImage());
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateUserImage_InvalidFileType_ShouldThrowBadRequestException() {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(image.getContentType()).thenReturn("application/pdf");
        when(image.isEmpty()).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updateUserImage(image, authentication));

        assertEquals("Разрешены только изображения в формате JPEG, JPG или PNG",
                exception.getMessage());
    }

    @Test
    void updateUserImage_FileTooLarge_ShouldThrowBadRequestException() {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB
        when(image.isEmpty()).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> userService.updateUserImage(image, authentication));

        assertEquals("Размер файла не должен превышать 10MB", exception.getMessage());
    }

    @Test
    void getUserImage_ExistingImage_ShouldReturnBytes() throws IOException {
        // Arrange
        byte[] imageBytes = "avatar bytes".getBytes();
        when(userRepository.findById(1)).thenReturn(Optional.of(userEntity));
        when(fileService.loadImage("users", "avatar.jpg")).thenReturn(imageBytes);

        // Act
        byte[] result = userService.getUserImage(1);

        // Assert
        assertNotNull(result);
        assertEquals(imageBytes, result);
    }

    @Test
    void getUserImage_UserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getUserImage(999));

        assertTrue(exception.getMessage().contains("Пользователь не найден"));
    }

    @Test
    void getUserImage_NoAvatar_ShouldThrowNotFoundException() {
        // Arrange
        userEntity.setImage(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(userEntity));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getUserImage(1));

        assertEquals("Аватар пользователя не найден", exception.getMessage());
    }

    @Test
    void userExists_ExistingEmail_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // Act
        boolean result = userService.userExists("test@test.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void getUserByEmail_ExistingEmail_ShouldReturnUserEntity() {
        // Arrange
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(userEntity));

        // Act
        UserEntity result = userService.getUserByEmail("test@test.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }
}