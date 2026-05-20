package com.innowise.user_service.repository;

import com.innowise.user_service.entity.PaymentCardsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCardsEntity,Long> {
    List<PaymentCardsEntity> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT c FROM PaymentCardsEntity c JOIN FETCH c.user WHERE c.user.id = :userId")
    List<PaymentCardsEntity> findAllCardsWithUser(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM payment_cards WHERE user_id = :userId AND active = true", nativeQuery = true)
    List<PaymentCardsEntity> findActiveCardsByUserIdNative(@Param("userId") Long userId);

    boolean existsByNumber(String number);
}
