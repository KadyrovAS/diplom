package ru.skypro.homework.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.skypro.homework.exception.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Test
    void handleForbiddenException_ShouldReturnForbidden() {
        // Given
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        ForbiddenException exception = new ForbiddenException("Access denied");

        // When
        ResponseEntity<?> response = globalExceptionHandler.handleForbiddenException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
    }
}