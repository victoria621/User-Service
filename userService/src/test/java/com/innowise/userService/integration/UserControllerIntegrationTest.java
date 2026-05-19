package com.innowise.userService.integration;

import com.innowise.userService.dto.UserRequestDTO;
import com.innowise.userService.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createUser_ShouldReturnCreatedUser() {
        UserRequestDTO request = new UserRequestDTO(
                "John", "Doe", null, "john@test.com"
        );

        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity(
                "/api/users", request, UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("John");
        assertThat(response.getBody().email()).isEqualTo("john@test.com");
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void createUser_ShouldReturnConflict_WhenEmailExists() {
        UserRequestDTO request1 = new UserRequestDTO(
                "John", "Doe", null, "duplicate@test.com"
        );
        restTemplate.postForEntity("/api/users", request1, UserResponseDTO.class);

        UserRequestDTO request2 = new UserRequestDTO(
                "Jane", "Smith", null, "duplicate@test.com"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users", request2, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already exists");
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        UserRequestDTO request = new UserRequestDTO(
                "John", "Doe", null, "john2@test.com"
        );
        ResponseEntity<UserResponseDTO> created = restTemplate.postForEntity(
                "/api/users", request, UserResponseDTO.class
        );
        Long userId = created.getBody().id();

        ResponseEntity<UserResponseDTO> response = restTemplate.getForEntity(
                "/api/users/" + userId, UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("John");
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users/999", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("User not found");
    }

    @Test
    void updateUser_ShouldUpdateFields() {
        UserRequestDTO createRequest = new UserRequestDTO(
                "Old", "Name", null, "update@test.com"
        );
        ResponseEntity<UserResponseDTO> created = restTemplate.postForEntity(
                "/api/users", createRequest, UserResponseDTO.class
        );
        Long userId = created.getBody().id();

        UserRequestDTO updateRequest = new UserRequestDTO(
                "New", "Name", null, "update@test.com"
        );
        HttpEntity<UserRequestDTO> entity = new HttpEntity<>(updateRequest);

        ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                "/api/users/" + userId, HttpMethod.PUT, entity, UserResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("New");
    }

    @Test
    void activateUser_ShouldSetActiveToTrue() {
        UserRequestDTO request = new UserRequestDTO(
                "John", "Doe", null, "activate@test.com"
        );
        ResponseEntity<UserResponseDTO> created = restTemplate.postForEntity(
                "/api/users", request, UserResponseDTO.class
        );
        Long userId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/activate", HttpMethod.PATCH, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity(
                "/api/users/" + userId, UserResponseDTO.class
        );
        assertThat(user.getBody().active()).isTrue();
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        UserRequestDTO request = new UserRequestDTO(
                "John", "Doe", null, "deactivate@test.com"
        );
        ResponseEntity<UserResponseDTO> created = restTemplate.postForEntity(
                "/api/users", request, UserResponseDTO.class
        );
        Long userId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/users/" + userId + "/deactivate", HttpMethod.PATCH, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponseDTO> user = restTemplate.getForEntity(
                "/api/users/" + userId, UserResponseDTO.class
        );
        assertThat(user.getBody().active()).isFalse();
    }

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {
        UserRequestDTO request1 = new UserRequestDTO(
                "Alice", "Smith", null, "alice@test.com"
        );
        UserRequestDTO request2 = new UserRequestDTO(
                "Bob", "Johnson", null, "bob@test.com"
        );
        restTemplate.postForEntity("/api/users", request1, UserResponseDTO.class);
        restTemplate.postForEntity("/api/users", request2, UserResponseDTO.class);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users?page=0&size=10", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Alice");
        assertThat(response.getBody()).contains("Bob");
    }
}