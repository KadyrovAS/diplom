package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.skypro.homework.dto.Login;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.dto.Role;
import ru.skypro.homework.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ValidCredentials_ShouldReturnOk() throws Exception {
        // Arrange
        Login login = new Login();
        login.setUsername("test@test.com");
        login.setPassword("password123");

        when(authService.login("test@test.com", "password123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    void login_InvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        Login login = new Login();
        login.setUsername("test@test.com");
        login.setPassword("wrongpassword");

        when(authService.login("test@test.com", "wrongpassword")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_InvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Login login = new Login();
        login.setUsername("a");  // Too short
        login.setPassword("b");   // Too short

        // Act & Assert - validation should fail
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_NewUser_ShouldReturnCreated() throws Exception {
        // Arrange
        Register register = new Register();
        register.setUsername("new@test.com");
        register.setPassword("password123");
        register.setFirstName("Иван");
        register.setLastName("Иванов");
        register.setPhone("+79991234567");
        register.setRole(Role.USER);

        when(authService.register(any(Register.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_ExistingUser_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Register register = new Register();
        register.setUsername("existing@test.com");
        register.setPassword("password123");
        register.setFirstName("Иван");
        register.setLastName("Иванов");
        register.setPhone("+79991234567");
        register.setRole(Role.USER);

        when(authService.register(any(Register.class))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_InvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Register register = new Register();
        register.setUsername("a");      // Too short
        register.setPassword("b");       // Too short
        register.setFirstName("c");      // Too short
        register.setLastName("d");       // Too short
        register.setPhone("123");        // Invalid format

        // Act & Assert - validation should fail
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isBadRequest());
    }
}