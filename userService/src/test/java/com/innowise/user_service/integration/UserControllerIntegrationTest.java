package com.innowise.user_service.integration;

import com.innowise.user_service.dto.UserRequestDTO;
import com.innowise.user_service.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration.class)
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
    private String generateUniqueEmail() {
        return "user_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999) + "@test.com";
    }

    private Long createTestUser() {
        String uniqueEmail = generateUniqueEmail();
        UserRequestDTO request = new UserRequestDTO("John", "Doe", null, uniqueEmail);
        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity("/api/users", request, UserResponseDTO.class);
        UserResponseDTO user = response.getBody();
        if (user == null || user.id() == null) {
            throw new RuntimeException("Failed to create test user");
        }
        return user.id();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        String uniqueEmail = generateUniqueEmail();
        UserRequestDTO request = new UserRequestDTO("John", "Doe", null, uniqueEmail);

        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity("/api/users", request, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("John");
        assertThat(response.getBody().email()).isEqualTo(uniqueEmail);
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void createUser_ShouldReturnConflict_WhenEmailExists() {
        String uniqueEmail = generateUniqueEmail();
        UserRequestDTO request1 = new UserRequestDTO("John", "Doe", null, uniqueEmail);
        restTemplate.postForEntity("/api/users", request1, UserResponseDTO.class);

        UserRequestDTO request2 = new UserRequestDTO("Jane", "Smith", null, uniqueEmail);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request2, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already exists");
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        Long userId = createTestUser();

        ResponseEntity<UserResponseDTO> response = restTemplate.getForEntity("/api/users/" + userId, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(userId);
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("User not found");
    }

    @Test
    void updateUser_ShouldUpdateFields() {
        Long userId = createTestUser();

        String uniqueEmail = "updated_" + System.currentTimeMillis() + "@test.com";
        UserRequestDTO updateRequest = new UserRequestDTO(
                "UpdatedName",
                "UpdatedSurname",
                null,
                uniqueEmail
        );

        ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                "/api/users/" + userId,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(updateRequest),
                UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("UpdatedName");
        assertThat(response.getBody().surname()).isEqualTo("UpdatedSurname");
        assertThat(response.getBody().email()).isEqualTo(uniqueEmail);
    }

    @Test
    void updateUser_ShouldReturnConflict_WhenEmailAlreadyExists() {
        // Создаем первого пользователя
        String existingEmail = generateUniqueEmail();
        UserRequestDTO user1 = new UserRequestDTO("John", "Doe", null, existingEmail);
        restTemplate.postForEntity("/api/users", user1, UserResponseDTO.class);

        // Создаем второго пользователя
        Long userId2 = createTestUser();

        // Пытаемся обновить второго пользователя, используя email первого
        UserRequestDTO updateRequest = new UserRequestDTO("Jane", "Smith", null, existingEmail);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + userId2,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(updateRequest),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already exists");
    }

    @Test
    void activateUser_ShouldSetActiveToTrue() {
        Long userId = createTestUser();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/activate",
                org.springframework.http.HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity("/api/users/" + userId, UserResponseDTO.class);
        assertThat(user.getBody().active()).isTrue();
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        Long userId = createTestUser();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/deactivate",
                org.springframework.http.HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity("/api/users/" + userId, UserResponseDTO.class);
        assertThat(user.getBody().active()).isFalse();
    }
}