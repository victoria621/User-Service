package com.innowise.userService.service;

import com.innowise.userService.entity.PaymentCardsEntity;
import com.innowise.userService.entity.UserEntity;
import com.innowise.userService.repository.PaymentCardRepository;
import com.innowise.userService.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;

    public CardService(PaymentCardRepository paymentCardRepository, UserRepository userRepository) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentCardsEntity createCard(PaymentCardsEntity paymentCardsEntity, Long userId) {
        UserEntity userEntity =userRepository.findById(userId).orElseThrow(
                () -> new RuntimeException("User with id " + userId + " not found"));

        if (paymentCardRepository.countByUserId(userId) >= 5) {
            throw new RuntimeException("User already has 5 cards, cannot add more");
        }

        if (paymentCardRepository.existsByNumber(paymentCardsEntity.getNumber())) {
            throw new RuntimeException("Card number already exists");
        }

        paymentCardsEntity.setUser(userEntity);

        return paymentCardRepository.save(paymentCardsEntity);
    }

    public PaymentCardsEntity getCardById(Long id) {
        return paymentCardRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Card with id " + id + " not found")
        );
    }

    public List<PaymentCardsEntity> getCardsByUserId(Long userId){
        List<PaymentCardsEntity> cards = paymentCardRepository.findByUserId(userId);
        if (cards.isEmpty()) {
            throw new RuntimeException("No cards found for user with id " + userId);
        }
        return cards;

    }

    public Page<PaymentCardsEntity> getAllCards(Pageable pageable) {
        return paymentCardRepository.findAll(pageable);

    }

    @Transactional
    public PaymentCardsEntity updateCard(Long id, PaymentCardsEntity paymentCardsEntity) {
        PaymentCardsEntity paymentCardsEntity1 = paymentCardRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Card with id " + id + " not found")
        );
        if (!paymentCardsEntity1.getNumber().equals(paymentCardsEntity.getNumber())) {
            if (paymentCardRepository.existsByNumber(paymentCardsEntity.getNumber())) {
                throw new RuntimeException("Card number already exists");
            }
        }
        paymentCardsEntity1.setNumber(paymentCardsEntity.getNumber());
        paymentCardsEntity1.setHolder(paymentCardsEntity.getHolder());
        paymentCardsEntity1.setExpirationDate(paymentCardsEntity.getExpirationDate());
        return paymentCardRepository.save(paymentCardsEntity1);
    }

    @Transactional
    public void activateCard(Long id) {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setActive(true);
    }

    @Transactional
    public void deactivateCard(Long id) {
        PaymentCardsEntity card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setActive(false);
    }

}
