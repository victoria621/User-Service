package com.innowise.userservice.integration;

import com.innowise.userservice.dto.UserRequestDTO;
import com.innowise.userservice.dto.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
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

    private void createTestUserWithName(String name, String surname) {
        String uniqueEmail = generateUniqueEmail();
        UserRequestDTO request = new UserRequestDTO(name, surname, null, uniqueEmail);
        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity("/api/users", request, UserResponseDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @BeforeEach
    void setUp() {
        createTestUserWithName("Alice", "Smith");
        createTestUserWithName("Bob", "Johnson");
        createTestUserWithName("Charlie", "Brown");
        createTestUserWithName("Diana", "Smith");
        createTestUserWithName("Eve", "Williams");
    }

    @Test
    void getAllUsers_ShouldReturnPagedUsers() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getTotalElements()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void getAllUsers_ShouldFilterByName() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users?name=Ali&page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).allMatch(user -> user.name().contains("Ali"));
    }

    @Test
    void getAllUsers_ShouldFilterBySurname() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users?surname=Smith&page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).allMatch(user -> user.surname().equals("Smith"));
    }

    @Test
    void getAllUsers_ShouldFilterByNameAndSurname() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users?name=Ali&surname=Smith&page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).allMatch(user ->
                user.name().contains("Ali") && user.surname().equals("Smith")
        );
    }

    @Test
    void getAllUsers_ShouldReturnEmptyPage_WhenNoMatch() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users?name=NonExistentName&page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isZero();
    }

    @Test
    void getAllUsers_ShouldUseDefaultPagination_WhenNoParams() {
        ResponseEntity<Page<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<UserResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isGreaterThanOrEqualTo(5);
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
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
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
        String existingEmail = generateUniqueEmail();
        UserRequestDTO user1 = new UserRequestDTO("John", "Doe", null, existingEmail);
        restTemplate.postForEntity("/api/users", user1, UserResponseDTO.class);

        Long userId2 = createTestUser();

        UserRequestDTO updateRequest = new UserRequestDTO("Jane", "Smith", null, existingEmail);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + userId2,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
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
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity("/api/users/" + userId, UserResponseDTO.class);
        assertThat(user.getBody()).isNotNull();
        assertThat(user.getBody().active()).isTrue();
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        Long userId = createTestUser();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/deactivate",
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity("/api/users/" + userId, UserResponseDTO.class);
        assertThat(user.getBody()).isNotNull();
        assertThat(user.getBody().active()).isFalse();
    }

    @Test
    void deleteUser_ShouldRemoveUser() {
        Long userId = createTestUser();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.getForEntity("/api/users/" + userId, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}