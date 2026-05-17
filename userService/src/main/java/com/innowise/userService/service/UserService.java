package com.innowise.userService.service;

import com.innowise.userService.entity.UserEntity;
import com.innowise.userService.repository.UserRepository;
import com.innowise.userService.specification.UserSpecification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity createUser(UserEntity user) {
        if(userRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("Email already exists");
        }
        user.setActive(true);


        return userRepository.save(user);
    }

    public UserEntity getUserById(Long id){
        return userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User with id " + id + " not found")
        );
    }

    public Page<UserEntity> getAllUsers(Pageable pageable, String name, String surname){
        Specification<UserEntity> spec = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));

        return userRepository.findAll(spec, pageable);
    }

    @Transactional
    public UserEntity updateUser(Long id, UserEntity user){
        if(userRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("Email already exists");
        }

        UserEntity updatedUser = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User with id " + id + " not found")
        );
        updatedUser.setName(user.getName());
        updatedUser.setSurname(user.getSurname());
        updatedUser.setBirthDate(user.getBirthDate());
        updatedUser.setEmail(user.getEmail());

        return userRepository.save(updatedUser);
    }

    @Transactional
    public void activateUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
    }

    @Transactional
    public void deactivateUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
    }
}
