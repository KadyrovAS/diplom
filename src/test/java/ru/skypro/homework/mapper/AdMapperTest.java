package ru.skypro.homework.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.skypro.homework.dto.Ad;
import ru.skypro.homework.dto.CreateOrUpdateAd;
import ru.skypro.homework.dto.ExtendedAd;
import ru.skypro.homework.entity.AdEntity;
import ru.skypro.homework.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdMapperTest {

    @Autowired
    private AdMapper adMapper;

    @Test
    void toDto_ShouldConvertEntityToDto() {
        // Arrange
        UserEntity author = new UserEntity();
        author.setId(1);
        author.setFirstName("Иван");
        author.setLastName("Иванов");
        author.setEmail("test@test.com");
        author.setPhone("+79991234567");

        AdEntity entity = new AdEntity();
        entity.setId(100);
        entity.setTitle("Test Ad");
        entity.setPrice(5000);
        entity.setDescription("Test Description");
        entity.setImage("image.jpg");
        entity.setAuthor(author);

        // Act
        Ad result = adMapper.toDto(entity);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        assertEquals("Test Ad", result.getTitle());
        assertEquals(5000, result.getPrice());
        assertEquals(1, result.getAuthor());
        assertEquals("/ads/100/image", result.getImage());
    }

    @Test
    void toDto_NullImage_ShouldReturnEmptyImage() {
        // Arrange
        AdEntity entity = new AdEntity();
        entity.setId(100);
        entity.setTitle("Test Ad");
        entity.setImage(null);

        UserEntity author = new UserEntity();
        author.setId(1);
        entity.setAuthor(author);

        // Act
        Ad result = adMapper.toDto(entity);

        // Assert
        assertEquals("", result.getImage());
    }

    @Test
    void toExtendedAd_ShouldConvertEntityToExtendedAd() {
        // Arrange
        UserEntity author = new UserEntity();
        author.setId(1);
        author.setFirstName("Иван");
        author.setLastName("Иванов");
        author.setEmail("test@test.com");
        author.setPhone("+79991234567");

        AdEntity entity = new AdEntity();
        entity.setId(100);
        entity.setTitle("Test Ad");
        entity.setPrice(5000);
        entity.setDescription("Test Description");
        entity.setImage("image.jpg");
        entity.setAuthor(author);

        // Act
        ExtendedAd result = adMapper.toExtendedAd(entity);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        assertEquals("Test Ad", result.getTitle());
        assertEquals(5000, result.getPrice());
        assertEquals("Test Description", result.getDescription());
        assertEquals("/ads/100/image", result.getImage());
        assertEquals("Иван", result.getAuthorFirstName());
        assertEquals("Иванов", result.getAuthorLastName());
        assertEquals("test@test.com", result.getEmail());
        assertEquals("+79991234567", result.getPhone());
    }

    @Test
    void toExtendedAd_NullAuthor_ShouldHandleGracefully() {
        // Arrange
        AdEntity entity = new AdEntity();
        entity.setId(100);
        entity.setTitle("Test Ad");
        entity.setPrice(5000);
        entity.setDescription("Test Description");
        entity.setImage("image.jpg");
        entity.setAuthor(null);

        // Act
        ExtendedAd result = adMapper.toExtendedAd(entity);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getPk());
        assertNull(result.getAuthorFirstName());
        assertNull(result.getAuthorLastName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());
    }

    @Test
    void toEntity_ShouldConvertDtoToEntity() {
        // Arrange
        CreateOrUpdateAd dto = new CreateOrUpdateAd();
        dto.setTitle("New Ad");
        dto.setPrice(3000);
        dto.setDescription("New Description");

        // Act
        AdEntity result = adMapper.toEntity(dto);

        // Assert
        assertNotNull(result);
        assertEquals("New Ad", result.getTitle());
        assertEquals(3000, result.getPrice());
        assertEquals("New Description", result.getDescription());
    }

    @Test
    void updateEntity_ShouldUpdateExistingEntity() {
        // Arrange
        CreateOrUpdateAd dto = new CreateOrUpdateAd();
        dto.setTitle("Updated Ad");
        dto.setPrice(4000);
        dto.setDescription("Updated Description");

        AdEntity entity = new AdEntity();
        entity.setTitle("Old Ad");
        entity.setPrice(2000);
        entity.setDescription("Old Description");

        // Act
        adMapper.updateEntity(dto, entity);

        // Assert
        assertEquals("Updated Ad", entity.getTitle());
        assertEquals(4000, entity.getPrice());
        assertEquals("Updated Description", entity.getDescription());
    }
}