package com.innowise.userservice.repository;

import com.innowise.userservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.name = :name AND u.surname = :surname")
    List<UserEntity> findByNameAndSurnameJPQL(@Param("name") String name, @Param("surname") String surname);

    @Query(value = "SELECT * FROM users WHERE active = true", nativeQuery = true)
    List<UserEntity> findAllActiveUsersNative();

    Page<UserEntity> findAll(Pageable pageable);
}