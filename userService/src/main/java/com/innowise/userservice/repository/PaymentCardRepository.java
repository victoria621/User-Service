package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCardEntity, Long> {

    List<PaymentCardEntity> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT c FROM PaymentCardEntity c JOIN FETCH c.user WHERE c.user.id = :userId")
    List<PaymentCardEntity> findAllCardsWithUser(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM payment_cards WHERE user_id = :userId AND active = true", nativeQuery = true)
    List<PaymentCardEntity> findActiveCardsByUserIdNative(@Param("userId") Long userId);

    boolean existsByNumber(String number);
}