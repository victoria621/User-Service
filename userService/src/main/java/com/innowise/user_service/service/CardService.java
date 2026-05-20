package com.innowise.user_service.service;

import com.innowise.user_service.dto.CardRequestDTO;
import com.innowise.user_service.dto.CardResponseDTO;
import com.innowise.user_service.entity.PaymentCardsEntity;
import com.innowise.user_service.entity.UserEntity;
import com.innowise.user_service.exception.BusinessException;
import com.innowise.user_service.exception.ResourceNotFoundException;
import com.innowise.user_service.mapper.CardMapper;
import com.innowise.user_service.repository.PaymentCardRepository;
import com.innowise.user_service.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private static final String USER_NOT_FOUND = "User with id %d not found";
    private static final String CARD_NOT_FOUND = "Card with id %d not found";
    private static final String CARD_NOT_FOUND_SIMPLE = "Card not found";
    private static final String NO_CARDS_FOUND = "No cards found for user with id %d";
    private static final String MAX_CARDS_LIMIT = "User already has 5 cards, cannot add more";
    private static final String CARD_NUMBER_EXISTS = "Card number already exists";

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    public CardService(PaymentCardRepository paymentCardRepository,
                       UserRepository userRepository,
                       CardMapper cardMapper) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public CardResponseDTO createCard(CardRequestDTO requestDTO, Long userId) {
        PaymentCardsEntity paymentCardsEntity = cardMapper.toEntity(requestDTO);
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId)));

        if (paymentCardRepository.countByUserId(userId) >= 5) {
            throw new BusinessException(MAX_CARDS_LIMIT);
        }

        if (paymentCardRepository.existsByNumber(paymentCardsEntity.getNumber())) {
            throw new BusinessException(CARD_NUMBER_EXISTS);
        }

        paymentCardsEntity.setUser(userEntity);
        PaymentCardsEntity savedCard = paymentCardRepository.save(paymentCardsEntity);

        return cardMapper.toDto(savedCard);
    }

    @Cacheable(value = "cards", key = "#id")
    public CardResponseDTO getCardById(Long id) {
        PaymentCardsEntity paymentCardsEntity = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return cardMapper.toDto(paymentCardsEntity);
    }

    public List<CardResponseDTO> getCardsByUserId(Long userId) {
        List<PaymentCardsEntity> cards = paymentCardRepository.findByUserId(userId);
        if (cards.isEmpty()) {
            throw new ResourceNotFoundException(String.format(NO_CARDS_FOUND, userId));
        }
        return cardMapper.toDtoList(cards);
    }

    public Page<CardResponseDTO> getAllCards(Pageable pageable) {
        Page<PaymentCardsEntity> cards = paymentCardRepository.findAll(pageable);
        return cards.map(cardMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public CardResponseDTO updateCard(Long id, CardRequestDTO requestDTO) {
        PaymentCardsEntity paymentCardsEntity = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (!paymentCardsEntity.getNumber().equals(requestDTO.number())
                && paymentCardRepository.existsByNumber(requestDTO.number())) {
            throw new BusinessException(CARD_NUMBER_EXISTS);
        }

        paymentCardsEntity.setNumber(requestDTO.number());
        paymentCardsEntity.setHolder(requestDTO.holder());
        paymentCardsEntity.setExpirationDate(requestDTO.expirationDate());

        PaymentCardsEntity updatedCard = paymentCardRepository.save(paymentCardsEntity);
        return cardMapper.toDto(updatedCard);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void activateCard(Long id) {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_SIMPLE));
        card.setActive(true);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void deactivateCard(Long id) {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND_SIMPLE));
        card.setActive(false);
    }
}