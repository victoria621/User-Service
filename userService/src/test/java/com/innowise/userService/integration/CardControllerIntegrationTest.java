package com.innowise.userService.integration;

import com.innowise.userService.dto.CardRequestDTO;
import com.innowise.userService.dto.CardResponseDTO;
import com.innowise.userService.dto.UserRequestDTO;
import com.innowise.userService.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CardControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Long createTestUser() {
        UserRequestDTO userRequest = new UserRequestDTO(
                "John", "Doe", null, "carduser@test.com"
        );
        ResponseEntity<UserResponseDTO> userResponse = restTemplate.postForEntity(
                "/api/users", userRequest, UserResponseDTO.class
        );
        return userResponse.getBody().id();
    }

    @Test
    void createCard_ShouldReturnCreatedCard() {
        Long userId = createTestUser();
        CardRequestDTO request = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2028, 12, 31)
        );

        ResponseEntity<CardResponseDTO> response = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", request, CardResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().number()).isEqualTo("1234567890123456");
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }

    @Test
    void createCard_ShouldReturnConflict_WhenCardLimitReached() {
        Long userId = createTestUser();

        for (int i = 0; i < 5; i++) {
            CardRequestDTO request = new CardRequestDTO(
                    "111111111111111" + i, "John Doe", LocalDate.of(2028, 12, 31)
            );
            restTemplate.postForEntity("/api/users/" + userId + "/cards", request, CardResponseDTO.class);
        }

        CardRequestDTO sixthCard = new CardRequestDTO(
                "9999999999999999", "John Doe", LocalDate.of(2028, 12, 31)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", sixthCard, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("5 cards");
    }

    @Test
    void getCardById_ShouldReturnCard_WhenCardExists() {
        Long userId = createTestUser();
        CardRequestDTO request = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2028, 12, 31)
        );
        ResponseEntity<CardResponseDTO> created = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", request, CardResponseDTO.class
        );
        Long cardId = created.getBody().id();

        ResponseEntity<CardResponseDTO> response = restTemplate.getForEntity(
                "/api/cards/" + cardId, CardResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().number()).isEqualTo("1234567890123456");
    }

    @Test
    void getCardById_ShouldReturnNotFound_WhenCardDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/cards/999", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Card with id 999 not found");
    }

    @Test
    void getCardsByUserId_ShouldReturnCardsList() {
        Long userId = createTestUser();
        CardRequestDTO request1 = new CardRequestDTO(
                "1111111111111111", "John Doe", LocalDate.of(2028, 12, 31)
        );
        CardRequestDTO request2 = new CardRequestDTO(
                "2222222222222222", "John Doe", LocalDate.of(2028, 12, 31)
        );
        restTemplate.postForEntity("/api/users/" + userId + "/cards", request1, CardResponseDTO.class);
        restTemplate.postForEntity("/api/users/" + userId + "/cards", request2, CardResponseDTO.class);

        ResponseEntity<CardResponseDTO[]> response = restTemplate.getForEntity(
                "/api/users/" + userId + "/cards", CardResponseDTO[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void updateCard_ShouldUpdateFields() {
        Long userId = createTestUser();
        CardRequestDTO createRequest = new CardRequestDTO(
                "1234567890123456", "Old Holder", LocalDate.of(2028, 12, 31)
        );
        ResponseEntity<CardResponseDTO> created = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", createRequest, CardResponseDTO.class
        );
        Long cardId = created.getBody().id();

        CardRequestDTO updateRequest = new CardRequestDTO(
                "1234567890123456", "New Holder", LocalDate.of(2029, 12, 31)
        );

        ResponseEntity<CardResponseDTO> response = restTemplate.exchange(
                "/api/cards/" + cardId, HttpMethod.PUT, new org.springframework.http.HttpEntity<>(updateRequest), CardResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().holder()).isEqualTo("New Holder");
    }

    @Test
    void activateCard_ShouldSetActiveToTrue() {
        Long userId = createTestUser();
        CardRequestDTO request = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2028, 12, 31)
        );
        ResponseEntity<CardResponseDTO> created = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", request, CardResponseDTO.class
        );
        Long cardId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cards/" + cardId + "/activate", HttpMethod.PATCH, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CardResponseDTO> card = restTemplate.getForEntity(
                "/api/cards/" + cardId, CardResponseDTO.class
        );
        assertThat(card.getBody().active()).isTrue();
    }

    @Test
    void deactivateCard_ShouldSetActiveToFalse() {
        Long userId = createTestUser();
        CardRequestDTO request = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2028, 12, 31)
        );
        ResponseEntity<CardResponseDTO> created = restTemplate.postForEntity(
                "/api/users/" + userId + "/cards", request, CardResponseDTO.class
        );
        Long cardId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cards/" + cardId + "/deactivate", HttpMethod.PATCH, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CardResponseDTO> card = restTemplate.getForEntity(
                "/api/cards/" + cardId, CardResponseDTO.class
        );
        assertThat(card.getBody().active()).isFalse();
    }
}