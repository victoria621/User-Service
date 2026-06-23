package com.innowise.userservice.dao;

import com.innowise.userservice.dao.CardDAO;
import com.innowise.userservice.entity.PaymentCardEntity;
import com.innowise.userservice.repository.PaymentCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CardDAOImpl implements CardDAO {

    private final PaymentCardRepository cardRepository;

    public CardDAOImpl(PaymentCardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public PaymentCardEntity save(PaymentCardEntity card) {
        return cardRepository.save(card);
    }

    @Override
    public Optional<PaymentCardEntity> findById(Long id) {
        return cardRepository.findById(id);
    }

    @Override
    public Page<PaymentCardEntity> findAll(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    @Override
    public List<PaymentCardEntity> findByUserId(Long userId) {
        return cardRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByNumber(String number) {
        return cardRepository.existsByNumber(number);
    }

    @Override
    public long countByUserId(Long userId) {
        return cardRepository.countByUserId(userId);
    }

    @Override
    public void delete(PaymentCardEntity card) {
        cardRepository.delete(card);
    }
}