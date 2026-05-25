package com.innowise.userservice.dao;

import com.innowise.userservice.entity.PaymentCardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CardDAO {
    PaymentCardEntity save(PaymentCardEntity card);
    Optional<PaymentCardEntity> findById(Long id);
    Page<PaymentCardEntity> findAll(Pageable pageable);
    List<PaymentCardEntity> findByUserId(Long userId);
    boolean existsByNumber(String number);
    long countByUserId(Long userId);
    void delete(PaymentCardEntity card);
}