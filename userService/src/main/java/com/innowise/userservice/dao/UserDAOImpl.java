package com.innowise.userservice.dao;

import com.innowise.userservice.entity.UserEntity;
import com.innowise.userservice.repository.UserDAO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDAOImpl implements com.innowise.userservice.dao.UserDAO {

    private final UserDAO userRepository;

    public UserDAOImpl(UserDAO userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Page<UserEntity> findAll(Specification<UserEntity> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void delete(UserEntity user) {
        userRepository.delete(user);
    }
}