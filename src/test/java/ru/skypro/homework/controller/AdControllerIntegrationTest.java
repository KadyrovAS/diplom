package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.skypro.homework.dto.*;
import ru.skypro.homework.exception.NotFoundException;
import ru.skypro.homework.service.AdService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdService adService;

    @Autowired
    private ObjectMapper objectMapper;

    private Ad testAd;
    private ExtendedAd testExtendedAd;
    private CreateOrUpdateAd createAdDto;
    private Ads adsList;

    @BeforeEach
    void setUp() {
        testAd = new Ad();
        testAd.setPk(1);
        testAd.setTitle("Test Ad");
        testAd.setPrice(5000);
        testAd.setAuthor(1);
        testAd.setImage("/ads/1/image");

        testExtendedAd = new ExtendedAd();
        testExtendedAd.setPk(1);
        testExtendedAd.setTitle("Test Ad");
        testExtendedAd.setPrice(5000);
        testExtendedAd.setDescription("Test Description");
        testExtendedAd.setAuthorFirstName("Иван");
        testExtendedAd.setAuthorLastName("Иванов");
        testExtendedAd.setEmail("test@test.com");
        testExtendedAd.setPhone("+79991234567");
        testExtendedAd.setImage("/ads/1/image");

        createAdDto = new CreateOrUpdateAd();
        createAdDto.setTitle("New Ad");
        createAdDto.setPrice(3000);
        createAdDto.setDescription("New Description");

        adsList = new Ads();
        adsList.setCount(1);
        adsList.setResults(Collections.singletonList(testAd));
    }

    @Test
    @WithMockUser
    void getAllAds_ShouldReturnAdsList() throws Exception {
        // Arrange
        when(adService.getAllAds()).thenReturn(adsList);

        // Act & Assert
        mockMvc.perform(get("/ads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].pk").value(1))
                .andExpect(jsonPath("$.results[0].title").value("Test Ad"))
                .andExpect(jsonPath("$.results[0].price").value(5000));
    }

    @Test
    @WithMockUser
    void addAd_ValidData_ShouldCreateAd() throws Exception {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image".getBytes());

        MockMultipartFile properties = new MockMultipartFile(
                "properties", "", "application/json",
                objectMapper.writeValueAsBytes(createAdDto));

        when(adService.addAd(any(CreateOrUpdateAd.class), any(), any())).thenReturn(testAd);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/ads")
                        .file(image)
                        .file(properties)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.title").value("Test Ad"));
    }

    @Test
    @WithMockUser
    void getAd_ExistingId_ShouldReturnAd() throws Exception {
        // Arrange
        when(adService.getAd(1)).thenReturn(testExtendedAd);

        // Act & Assert
        mockMvc.perform(get("/ads/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.title").value("Test Ad"))
                .andExpect(jsonPath("$.price").value(5000));
    }

    @Test
    @WithMockUser
    void getAd_NonExistingId_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(adService.getAd(999)).thenThrow(new NotFoundException("Объявление не найдено"));

        // Act & Assert
        mockMvc.perform(get("/ads/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteAd_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(adService).deleteAd(anyInt(), any());

        // Act & Assert
        mockMvc.perform(delete("/ads/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(adService).deleteAd(anyInt(), any());
    }

    @Test
    @WithMockUser
    void updateAd_ValidData_ShouldReturnUpdatedAd() throws Exception {
        // Arrange
        when(adService.updateAd(anyInt(), any(CreateOrUpdateAd.class), any())).thenReturn(testAd);

        // Act & Assert
        mockMvc.perform(patch("/ads/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAdDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.title").value("Test Ad"));
    }

    @Test
    @WithMockUser
    void getMyAds_ShouldReturnUserAds() throws Exception {
        // Arrange
        when(adService.getMyAds(any())).thenReturn(adsList);

        // Act & Assert
        mockMvc.perform(get("/ads/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].pk").value(1));
    }

    @Test
    @WithMockUser
    void updateAdImage_ShouldReturnOk() throws Exception {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image".getBytes());

        doNothing().when(adService).updateAdImage(anyInt(), any(), any());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/ads/1/image")
                        .file(image)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(adService).updateAdImage(anyInt(), any(), any());
    }

    @Test
    void getAdImage_ExistingImage_ShouldReturnImage() throws Exception {
        // Arrange
        byte[] imageBytes = "test image".getBytes();
        when(adService.getAdImage(1)).thenReturn(imageBytes);

        // Act & Assert
        mockMvc.perform(get("/ads/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void getAdImage_NonExistingImage_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(adService.getAdImage(1)).thenThrow(new NotFoundException("Изображение не найдено"));

        // Act & Assert
        mockMvc.perform(get("/ads/1/image"))
                .andExpect(status().isNotFound());
    }

     @Test
     void getMyAds_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
         mockMvc.perform(get("/ads/me"))
                 .andExpect(status().isUnauthorized());
     }
}