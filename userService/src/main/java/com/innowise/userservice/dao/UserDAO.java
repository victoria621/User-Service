package com.innowise.userservice.dao;

import com.innowise.userservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface UserDAO {
    UserEntity save(UserEntity user);
    Optional<UserEntity> findByIdWithCards(Long id);
    Page<UserEntity> findAll(Specification<UserEntity> spec, Pageable pageable);
    boolean existsByEmail(String email);
    void delete(UserEntity user);
    Optional<UserEntity> findById(Long id);
}