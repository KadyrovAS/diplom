package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.*;
import ru.skypro.homework.entity.AdEntity;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.exception.BadRequestException;
import ru.skypro.homework.exception.ForbiddenException;
import ru.skypro.homework.exception.NotFoundException;
import ru.skypro.homework.mapper.AdMapper;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.UserRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdServiceImplTest {

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdMapper adMapper;

    @Mock
    private FileService fileService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdServiceImpl adService;

    private UserEntity testUser;
    private UserEntity testAdmin;
    private AdEntity testAd;
    private CreateOrUpdateAd createAdDto;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1);
        testUser.setEmail("user@test.com");
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");
        testUser.setPhone("+79991234567");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.USER);

        testAdmin = new UserEntity();
        testAdmin.setId(2);
        testAdmin.setEmail("admin@test.com");
        testAdmin.setRole(Role.ADMIN);

        testAd = new AdEntity();
        testAd.setId(100);
        testAd.setTitle("Test Ad");
        testAd.setPrice(5000);
        testAd.setDescription("Test Description");
        testAd.setAuthor(testUser);
        testAd.setImage("test-image.jpg");

        createAdDto = new CreateOrUpdateAd();
        createAdDto.setTitle("New Ad");
        createAdDto.setPrice(3000);
        createAdDto.setDescription("New Description");
    }

    @Test
    void getAllAds_ShouldReturnAdsList() {
        // Arrange
        List<AdEntity> adEntities = Collections.singletonList(testAd);
        Ad adDto = new Ad();
        adDto.setPk(100);
        adDto.setTitle("Test Ad");
        adDto.setPrice(5000);
        adDto.setAuthor(1);
        adDto.setImage("/ads/100/image");

        when(adRepository.findAll()).thenReturn(adEntities);
        when(adMapper.toDto(testAd)).thenReturn(adDto);

        // Act
        Ads result = adService.getAllAds();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCount());
        assertEquals(1, result.getResults().size());
        assertEquals(100, result.getResults().get(0).getPk());

        verify(adRepository).findAll();
        verify(adMapper).toDto(testAd);
    }

    @Test
    void getAllAds_EmptyList_ShouldReturnEmptyAds() {
        // Arrange
        when(adRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        Ads result = adService.getAllAds();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCount());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void addAd_ValidData_ShouldCreateAd() throws IOException {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(adMapper.toEntity(createAdDto)).thenReturn(testAd);
        when(fileService.saveImage(image, "ads")).thenReturn("saved-image.jpg");
        when(adRepository.save(any(AdEntity.class))).thenReturn(testAd);

        Ad expectedAd = new Ad();
        expectedAd.setPk(100);
        expectedAd.setTitle("Test Ad");
        when(adMapper.toDto(testAd)).thenReturn(expectedAd);

        // Act
        Ad result = adService.addAd(createAdDto, image, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        verify(userRepository).findByEmail("user@test.com");
        verify(fileService).saveImage(image, "ads");
        verify(adRepository).save(any(AdEntity.class));
    }

    @Test
    void addAd_UserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(authentication.getName()).thenReturn("unknown@test.com");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adService.addAd(createAdDto, image, authentication));

        assertTrue(exception.getMessage().contains("Пользователь не найден"));
    }

    @Test
    void addAd_NoImage_ShouldThrowBadRequestException() {
        // Arrange
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(adMapper.toEntity(createAdDto)).thenReturn(new AdEntity());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adService.addAd(createAdDto, null, authentication));

        assertEquals("Изображение объявления обязательно", exception.getMessage());
    }
    @Test
    void getAd_ExistingId_ShouldReturnExtendedAd() {
        // Arrange
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));

        ExtendedAd expected = new ExtendedAd();
        expected.setPk(100);
        expected.setTitle("Test Ad");
        when(adMapper.toExtendedAd(testAd)).thenReturn(expected);

        // Act
        ExtendedAd result = adService.getAd(100);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        verify(adRepository).findById(100);
    }

    @Test
    void getAd_NonExistingId_ShouldThrowNotFoundException() {
        // Arrange
        when(adRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> adService.getAd(999));

        assertTrue(exception.getMessage().contains("Объявление не найдено"));
    }

    @Test
    void deleteAd_AuthorDeletesOwnAd_ShouldDelete() throws IOException {
        // Arrange
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        // Act
        adService.deleteAd(100, authentication);

        // Assert
        verify(fileService).deleteImage("ads", "test-image.jpg");
        verify(adRepository).delete(testAd);
    }

    @Test
    void deleteAd_AdminDeletesAnyAd_ShouldDelete() throws IOException {
        // Arrange
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(authentication.getName()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));

        // Act
        adService.deleteAd(100, authentication);

        // Assert
        verify(fileService).deleteImage("ads", "test-image.jpg");
        verify(adRepository).delete(testAd);
    }

    @Test
    void deleteAd_UserTriesToDeleteOtherUsersAd_ShouldThrowForbiddenException() {
        // Arrange
        UserEntity otherUser = new UserEntity();
        otherUser.setId(3);
        otherUser.setEmail("other@test.com");
        otherUser.setRole(Role.USER);

        testAd.setAuthor(otherUser);

        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> adService.deleteAd(100, authentication));

        assertEquals("Нет прав на удаление объявления", exception.getMessage());
        verify(adRepository, never()).delete(any());
    }

    @Test
    void updateAd_AuthorUpdatesOwnAd_ShouldUpdate() {
        // Arrange
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(adRepository.save(any(AdEntity.class))).thenReturn(testAd);

        Ad expectedAd = new Ad();
        expectedAd.setPk(100);
        expectedAd.setTitle("Updated Title");
        when(adMapper.toDto(testAd)).thenReturn(expectedAd);

        // Act
        Ad result = adService.updateAd(100, createAdDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        verify(adMapper).updateEntity(createAdDto, testAd);
        verify(adRepository).save(testAd);
    }

    @Test
    void getMyAds_ShouldReturnUsersAds() {
        // Arrange
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(adRepository.findByAuthor(testUser)).thenReturn(Collections.singletonList(testAd));

        Ad adDto = new Ad();
        adDto.setPk(100);
        when(adMapper.toDto(testAd)).thenReturn(adDto);

        // Act
        Ads result = adService.getMyAds(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCount());
        assertEquals(100, result.getResults().get(0).getPk());
        verify(adRepository).findByAuthor(testUser);
    }

    @Test
    void updateAdImage_ValidImage_ShouldUpdate() throws IOException {
        // Arrange
        MultipartFile image = mock(MultipartFile.class);
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(fileService.saveImage(image, "ads")).thenReturn("new-image.jpg");

        // Act
        adService.updateAdImage(100, image, authentication);

        // Assert
        verify(fileService).deleteImage("ads", "test-image.jpg");
        verify(fileService).saveImage(image, "ads");
        verify(adRepository).save(testAd);
        assertEquals("new-image.jpg", testAd.getImage());
    }

    @Test
    void getAdImage_ExistingImage_ShouldReturnBytes() throws IOException {
        // Arrange
        byte[] imageBytes = "test image bytes".getBytes();
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));
        when(fileService.loadImage("ads", "test-image.jpg")).thenReturn(imageBytes);

        // Act
        byte[] result = adService.getAdImage(100);

        // Assert
        assertNotNull(result);
        assertEquals(imageBytes, result);
    }

    @Test
    void getAdImage_NoImage_ShouldReturnEmptyArray() {
        // Arrange
        testAd.setImage(null);
        when(adRepository.findById(100)).thenReturn(Optional.of(testAd));

        // Act
        byte[] result = adService.getAdImage(100);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }
}