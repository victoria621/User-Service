package com.innowise.userService.service;

import com.innowise.userService.dto.CardRequestDTO;
import com.innowise.userService.dto.CardResponseDTO;
import com.innowise.userService.entity.PaymentCardsEntity;
import com.innowise.userService.entity.UserEntity;
import com.innowise.userService.exception.BusinessException;
import com.innowise.userService.exception.ResourceNotFoundException;
import com.innowise.userService.mapper.CardMapper;
import com.innowise.userService.repository.PaymentCardRepository;
import com.innowise.userService.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    public CardService(PaymentCardRepository paymentCardRepository, UserRepository userRepository, CardMapper cardMapper) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public CardResponseDTO createCard(CardRequestDTO requestDTO, Long userId) {
        PaymentCardsEntity paymentCardsEntity = cardMapper.toEntity(requestDTO);
        UserEntity userEntity =userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (paymentCardRepository.countByUserId(userId) >= 5) {
            throw new BusinessException("User already has 5 cards, cannot add more");
        }

        if (paymentCardRepository.existsByNumber(paymentCardsEntity.getNumber())) {
            throw new BusinessException("Card number already exists");
        }

        paymentCardsEntity.setUser(userEntity);

        PaymentCardsEntity paymentCardsEntity1 = paymentCardRepository.save(paymentCardsEntity);

        return cardMapper.toDto(paymentCardsEntity1);
    }

    public CardResponseDTO getCardById(Long id)  {
        PaymentCardsEntity paymentCardsEntity = paymentCardRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Card with id " + id + " not found")
        );
        return cardMapper.toDto(paymentCardsEntity);
    }

    public List<CardResponseDTO> getCardsByUserId(Long userId)  {
        List<PaymentCardsEntity> cards = paymentCardRepository.findByUserId(userId);
        if (cards.isEmpty()) {
            throw new ResourceNotFoundException("No cards found for user with id " + userId);
        }
        return cardMapper.toDtoList(cards);

    }

    public Page<CardResponseDTO> getAllCards(Pageable pageable) {
        Page<PaymentCardsEntity> cards = paymentCardRepository.findAll(pageable);
        return cards.map(cardMapper::toDto);

    }

    @Transactional
    public CardResponseDTO updateCard(Long id, CardRequestDTO requestDTO)  {
        PaymentCardsEntity paymentCardsEntity = paymentCardRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Card with id " + id + " not found")
        );

        if (!paymentCardsEntity.getNumber().equals(requestDTO.number())) {
            if (paymentCardRepository.existsByNumber(requestDTO.number())) {
                throw new BusinessException("Card number already exists");
            }
        }
        paymentCardsEntity.setNumber(requestDTO.number());
        paymentCardsEntity.setHolder(requestDTO.holder());
        paymentCardsEntity.setExpirationDate(requestDTO.expirationDate());

        PaymentCardsEntity paymentCardsEntity1 = paymentCardRepository.save(paymentCardsEntity);

        return cardMapper.toDto(paymentCardsEntity1);
    }

    @Transactional
    public void activateCard(Long id)  {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        card.setActive(true);
    }

    @Transactional
    public void deactivateCard(Long id) {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        card.setActive(false);
    }

}
