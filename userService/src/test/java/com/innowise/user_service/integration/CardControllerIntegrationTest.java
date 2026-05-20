package com.innowise.user_service.integration;

import com.innowise.user_service.dto.CardRequestDTO;
import com.innowise.user_service.dto.CardResponseDTO;
import com.innowise.user_service.dto.UserRequestDTO;
import com.innowise.user_service.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersConfiguration.class)
@Transactional
class CardControllerIntegrationTest {

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
    private String generateUniqueCardNumber() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return sb.toString();
    }

    private String generateUniqueEmail() {
        return "carduser_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999) + "@test.com";
    }

    private Long createTestUser() {
        String uniqueEmail = generateUniqueEmail();
        UserRequestDTO userRequest = new UserRequestDTO("John", "Doe", null, uniqueEmail);
        ResponseEntity<UserResponseDTO> userResponse = restTemplate.postForEntity("/api/users", userRequest, UserResponseDTO.class);
        UserResponseDTO user = userResponse.getBody();
        if (user == null || user.id() == null) {
            throw new RuntimeException("Failed to create test user");
        }
        return user.id();
    }

    private Long createTestCard(Long userId, String holder, LocalDate expirationDate) {
        String uniqueNumber = generateUniqueCardNumber();
        CardRequestDTO request = new CardRequestDTO(uniqueNumber, holder, expirationDate);
        ResponseEntity<CardResponseDTO> response = restTemplate.postForEntity("/api/users/" + userId + "/cards", request, CardResponseDTO.class);
        CardResponseDTO card = response.getBody();
        if (card == null || card.id() == null) {
            throw new RuntimeException("Failed to create test card");
        }
        return card.id();
    }

    @Test
    void createCard_ShouldReturnCreatedCard() {
        Long userId = createTestUser();
        String uniqueNumber = generateUniqueCardNumber();
        CardRequestDTO request = new CardRequestDTO(uniqueNumber, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<CardResponseDTO> response = restTemplate.postForEntity("/api/users/" + userId + "/cards", request, CardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().number()).isEqualTo(uniqueNumber);
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }

    @Test
    void createCard_ShouldReturnConflict_WhenCardLimitReached() {
        Long userId = createTestUser();

        for (int i = 0; i < 5; i++) {
            createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));
        }

        String sixthCardNumber = generateUniqueCardNumber();
        CardRequestDTO sixthCard = new CardRequestDTO(sixthCardNumber, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users/" + userId + "/cards", sixthCard, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("5 cards");
    }

    @Test
    void getCardById_ShouldReturnCard_WhenCardExists() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<CardResponseDTO> response = restTemplate.getForEntity("/api/cards/" + cardId, CardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(cardId);
    }

    @Test
    void getCardById_ShouldReturnNotFound_WhenCardDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cards/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Card with id 99999 not found");
    }

    @Test
    void getCardsByUserId_ShouldReturnCardsList() {
        Long userId = createTestUser();
        createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));
        createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<CardResponseDTO[]> response = restTemplate.getForEntity("/api/users/" + userId + "/cards", CardResponseDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void updateCard_ShouldUpdateFields() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId, "Old Holder", LocalDate.of(2028, 12, 31));

        ResponseEntity<CardResponseDTO> getResponse = restTemplate.getForEntity("/api/cards/" + cardId, CardResponseDTO.class);
        String cardNumber = getResponse.getBody().number();

        CardRequestDTO updateRequest = new CardRequestDTO(cardNumber, "New Holder", LocalDate.of(2029, 12, 31));

        ResponseEntity<CardResponseDTO> response = restTemplate.exchange("/api/cards/" + cardId, HttpMethod.PUT, new org.springframework.http.HttpEntity<>(updateRequest), CardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().holder()).isEqualTo("New Holder");
    }

    @Test
    void activateCard_ShouldSetActiveToTrue() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<Void> response = restTemplate.exchange("/api/cards/" + cardId + "/activate", HttpMethod.PATCH, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CardResponseDTO> card = restTemplate.getForEntity("/api/cards/" + cardId, CardResponseDTO.class);
        assertThat(card.getBody().active()).isTrue();
    }

    @Test
    void deactivateCard_ShouldSetActiveToFalse() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId, "John Doe", LocalDate.of(2028, 12, 31));

        ResponseEntity<Void> response = restTemplate.exchange("/api/cards/" + cardId + "/deactivate", HttpMethod.PATCH, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<CardResponseDTO> card = restTemplate.getForEntity("/api/cards/" + cardId, CardResponseDTO.class);
        assertThat(card.getBody().active()).isFalse();
    }
}