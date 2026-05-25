package com.innowise.userservice.integration;

import com.innowise.userservice.dto.CardRequestDTO;
import com.innowise.userservice.dto.CardResponseDTO;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CardControllerIntegrationTest {

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

    private CardRequestDTO createValidCardRequest() {
        return new CardRequestDTO(
                String.valueOf(1000000000000000L + ThreadLocalRandom.current().nextLong(9000000000000000L)),
                "John Doe",
                LocalDate.now().plusYears(3)
        );
    }

    private Long createTestCard(Long userId) {
        CardRequestDTO request = createValidCardRequest();
        ResponseEntity<CardResponseDTO> response = restTemplate.postForEntity(
                "/api/cards/users/" + userId + "/cards",
                request,
                CardResponseDTO.class
        );
        CardResponseDTO card = response.getBody();
        if (card == null || card.id() == null) {
            throw new RuntimeException("Failed to create test card");
        }
        return card.id();
    }

    @BeforeEach
    void setUp() {
        Long userId = createTestUser();
        for (int i = 0; i < 3; i++) {
            createTestCard(userId);
        }
    }

    @Test
    void getAllCards_ShouldReturnPagedCards() {
        ResponseEntity<Page<CardResponseDTO>> response = restTemplate.exchange(
                "/api/cards?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<CardResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
    }

    @Test
    void getAllCards_ShouldReturnSecondPage() {
        ResponseEntity<Page<CardResponseDTO>> response = restTemplate.exchange(
                "/api/cards?page=1&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Page<CardResponseDTO>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getCardById_ShouldReturnCard_WhenCardExists() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId);

        ResponseEntity<CardResponseDTO> response = restTemplate.getForEntity("/api/cards/" + cardId, CardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(cardId);
    }

    @Test
    void getCardById_ShouldReturnNotFound_WhenCardDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cards/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Card not found");
    }

    @Test
    void getCardsByUserId_ShouldReturnUserCards() {
        Long userId = createTestUser();
        createTestCard(userId);
        createTestCard(userId);

        ResponseEntity<CardResponseDTO[]> response = restTemplate.getForEntity(
                "/api/cards/users/" + userId,
                CardResponseDTO[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void createCard_ShouldReturnCreatedCard() {
        Long userId = createTestUser();
        CardRequestDTO request = createValidCardRequest();

        ResponseEntity<CardResponseDTO> response = restTemplate.postForEntity(
                "/api/cards/users/" + userId + "/cards",
                request,
                CardResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().number()).isEqualTo(request.number());
        assertThat(response.getBody().active()).isTrue();
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }

    @Test
    void createCard_ShouldReturnConflict_WhenCardNumberExists() {
        Long userId = createTestUser();
        CardRequestDTO request = createValidCardRequest();

        restTemplate.postForEntity("/api/cards/users/" + userId + "/cards", request, CardResponseDTO.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/cards/users/" + userId + "/cards",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Card number already exists");
    }

    @Test
    void createCard_ShouldReturnConflict_WhenUserHasMaxCards() {
        Long userId = createTestUser();
        for (int i = 0; i < 3; i++) {
            createTestCard(userId);
        }

        CardRequestDTO request = createValidCardRequest();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/cards/users/" + userId + "/cards",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("maximum number of cards");
    }

    @Test
    void updateCard_ShouldUpdateFields() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId);
        CardRequestDTO updateRequest = new CardRequestDTO(
                String.valueOf(2000000000000000L + ThreadLocalRandom.current().nextLong(9000000000000000L)),
                "Updated Holder",
                LocalDate.now().plusYears(4)
        );

        ResponseEntity<CardResponseDTO> response = restTemplate.exchange(
                "/api/cards/" + cardId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                CardResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().holder()).isEqualTo("Updated Holder");
        assertThat(response.getBody().number()).isEqualTo(updateRequest.number());
    }

    @Test
    void activateCard_ShouldSetActiveToTrue() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cards/" + cardId + "/activate",
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deactivateCard_ShouldSetActiveToFalse() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cards/" + cardId + "/deactivate",
                HttpMethod.PATCH,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteCard_ShouldRemoveCard() {
        Long userId = createTestUser();
        Long cardId = createTestCard(userId);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cards/" + cardId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}