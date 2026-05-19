package com.innowise.userService.unit;

import com.innowise.userService.dto.CardRequestDTO;
import com.innowise.userService.dto.CardResponseDTO;
import com.innowise.userService.entity.PaymentCardsEntity;
import com.innowise.userService.entity.UserEntity;
import com.innowise.userService.exception.BusinessException;
import com.innowise.userService.exception.ResourceNotFoundException;
import com.innowise.userService.mapper.CardMapper;
import com.innowise.userService.repository.PaymentCardRepository;
import com.innowise.userService.repository.UserRepository;
import com.innowise.userService.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardById_ShouldReturnCardDto_WhenCardExists() {
        Long cardId = 1L;
        PaymentCardsEntity cardEntity = new PaymentCardsEntity();
        cardEntity.setId(cardId);
        cardEntity.setNumber("1234567890123456");
        cardEntity.setHolder("John Doe");

        CardResponseDTO expectedDto = new CardResponseDTO(
                cardId, "1234567890123456", "John Doe",
                LocalDate.of(2025, 12, 31), true, 1L,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));
        when(cardMapper.toDto(cardEntity)).thenReturn(expectedDto);

        CardResponseDTO result = cardService.getCardById(cardId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(cardId);
        assertThat(result.number()).isEqualTo("1234567890123456");
        verify(cardRepository).findById(cardId);
    }

    @Test
    void getCardById_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 999L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Card with id " + cardId + " not found");
    }

    @Test
    void createCard_ShouldSaveCard_WhenValid() {
        Long userId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31)
        );

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);

        PaymentCardsEntity cardEntity = new PaymentCardsEntity();
        cardEntity.setNumber("1234567890123456");

        PaymentCardsEntity savedCard = new PaymentCardsEntity();
        savedCard.setId(1L);
        savedCard.setNumber("1234567890123456");
        savedCard.setHolder("John Doe");
        savedCard.setUser(userEntity);

        CardResponseDTO expectedDto = new CardResponseDTO(
                1L, "1234567890123456", "John Doe",
                LocalDate.of(2025, 12, 31), true, userId,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(cardRepository.countByUserId(userId)).thenReturn(2L);
        when(cardRepository.existsByNumber("1234567890123456")).thenReturn(false);
        when(cardMapper.toEntity(requestDTO)).thenReturn(cardEntity);
        when(cardRepository.save(cardEntity)).thenReturn(savedCard);
        when(cardMapper.toDto(savedCard)).thenReturn(expectedDto);

        CardResponseDTO result = cardService.createCard(requestDTO, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(cardRepository).save(cardEntity);
    }

    @Test
    void createCard_ShouldThrowException_WhenUserNotFound() {
        Long userId = 999L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(requestDTO, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id " + userId + " not found");
    }

    @Test
    void createCard_ShouldThrowException_WhenUserHas5Cards() {
        Long userId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31)
        );

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(cardRepository.countByUserId(userId)).thenReturn(5L);

        assertThatThrownBy(() -> cardService.createCard(requestDTO, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User already has 5 cards, cannot add more");
    }

    @Test
    void createCard_ShouldThrowException_WhenCardNumberAlreadyExists() {
        Long userId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2025, 12, 31)
        );

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);

        PaymentCardsEntity cardEntity = new PaymentCardsEntity();
        cardEntity.setNumber("1234567890123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(cardRepository.countByUserId(userId)).thenReturn(2L);
        when(cardRepository.existsByNumber("1234567890123456")).thenReturn(true);
        when(cardMapper.toEntity(requestDTO)).thenReturn(cardEntity);

        assertThatThrownBy(() -> cardService.createCard(requestDTO, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Card number already exists");
    }

    @Test
    void getCardsByUserId_ShouldReturnCardsList_WhenCardsExist() {
        Long userId = 1L;
        PaymentCardsEntity card1 = new PaymentCardsEntity();
        card1.setId(1L);
        PaymentCardsEntity card2 = new PaymentCardsEntity();
        card2.setId(2L);

        List<PaymentCardsEntity> cards = List.of(card1, card2);

        CardResponseDTO dto1 = new CardResponseDTO(1L, "1111", "John", null, true, userId, null, null);
        CardResponseDTO dto2 = new CardResponseDTO(2L, "2222", "John", null, true, userId, null, null);

        when(cardRepository.findByUserId(userId)).thenReturn(cards);
        when(cardMapper.toDtoList(cards)).thenReturn(List.of(dto1, dto2));

        List<CardResponseDTO> result = cardService.getCardsByUserId(userId);

        assertThat(result).hasSize(2);
        verify(cardRepository).findByUserId(userId);
    }

    @Test
    void getCardsByUserId_ShouldThrowException_WhenNoCardsFound() {
        Long userId = 1L;
        when(cardRepository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> cardService.getCardsByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No cards found for user with id " + userId);
    }

    @Test
    void getAllCards_ShouldReturnPageOfCards() {
        Pageable pageable = PageRequest.of(0, 10);
        PaymentCardsEntity card = new PaymentCardsEntity();
        card.setId(1L);

        Page<PaymentCardsEntity> cardPage = new PageImpl<>(List.of(card), pageable, 1);

        CardResponseDTO dto = new CardResponseDTO(1L, "1111", "John", null, true, 1L, null, null);

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);
        when(cardMapper.toDto(card)).thenReturn(dto);

        Page<CardResponseDTO> result = cardService.getAllCards(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void updateCard_ShouldUpdateFields_WhenNumberNotChanged() {
        Long cardId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "Updated Holder", LocalDate.of(2026, 12, 31)
        );

        PaymentCardsEntity existingCard = new PaymentCardsEntity();
        existingCard.setId(cardId);
        existingCard.setNumber("1234567890123456");
        existingCard.setHolder("Old Holder");

        PaymentCardsEntity savedCard = new PaymentCardsEntity();
        savedCard.setId(cardId);
        savedCard.setNumber("1234567890123456");
        savedCard.setHolder("Updated Holder");

        CardResponseDTO expectedDto = new CardResponseDTO(
                cardId, "1234567890123456", "Updated Holder",
                LocalDate.of(2026, 12, 31), true, 1L, null, null
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(existingCard)).thenReturn(savedCard);
        when(cardMapper.toDto(savedCard)).thenReturn(expectedDto);

        CardResponseDTO result = cardService.updateCard(cardId, requestDTO);

        assertThat(result.holder()).isEqualTo("Updated Holder");
        verify(cardRepository, never()).existsByNumber(any());
    }

    @Test
    void updateCard_ShouldCheckUniqueness_WhenNumberChanged() {
        Long cardId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "9999999999999999", "John Doe", LocalDate.of(2026, 12, 31)
        );

        PaymentCardsEntity existingCard = new PaymentCardsEntity();
        existingCard.setId(cardId);
        existingCard.setNumber("1234567890123456");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardRepository.existsByNumber("9999999999999999")).thenReturn(false);
        when(cardRepository.save(existingCard)).thenReturn(existingCard);

        cardService.updateCard(cardId, requestDTO);

        assertThat(existingCard.getNumber()).isEqualTo("9999999999999999");
        verify(cardRepository).existsByNumber("9999999999999999");
    }

    @Test
    void updateCard_ShouldThrowException_WhenNewNumberAlreadyExists() {
        Long cardId = 1L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "9999999999999999", "John Doe", LocalDate.of(2026, 12, 31)
        );

        PaymentCardsEntity existingCard = new PaymentCardsEntity();
        existingCard.setId(cardId);
        existingCard.setNumber("1234567890123456");

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardRepository.existsByNumber("9999999999999999")).thenReturn(true);

        assertThatThrownBy(() -> cardService.updateCard(cardId, requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Card number already exists");
    }

    @Test
    void updateCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 999L;
        CardRequestDTO requestDTO = new CardRequestDTO(
                "1234567890123456", "John Doe", LocalDate.of(2026, 12, 31)
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.updateCard(cardId, requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Card with id " + cardId + " not found");
    }

    @Test
    void activateCard_ShouldSetActiveToTrue() {
        Long cardId = 1L;
        PaymentCardsEntity card = new PaymentCardsEntity();
        card.setId(cardId);
        card.setActive(false);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.activateCard(cardId);

        assertThat(card.getActive()).isTrue();
        verify(cardRepository).findById(cardId);
    }

    @Test
    void activateCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 999L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.activateCard(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Card not found");
    }

    @Test
    void deactivateCard_ShouldSetActiveToFalse() {
        Long cardId = 1L;
        PaymentCardsEntity card = new PaymentCardsEntity();
        card.setId(cardId);
        card.setActive(true);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.deactivateCard(cardId);

        assertThat(card.getActive()).isFalse();
        verify(cardRepository).findById(cardId);
    }

    @Test
    void deactivateCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 999L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.deactivateCard(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Card not found");
    }
}