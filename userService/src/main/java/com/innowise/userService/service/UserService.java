package com.innowise.userService.service;

import com.innowise.userService.dto.UserRequestDTO;
import com.innowise.userService.dto.UserResponseDTO;
import com.innowise.userService.entity.UserEntity;
import com.innowise.userService.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        UserEntity user = userMapper.toEntity(requestDTO);
        if(userRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("Email already exists");
        }
        user.setActive(true);

        UserEntity savedEntity = userRepository.save(user);

        return userMapper.toDto(savedEntity);
    }

    public UserResponseDTO getUserById(Long id){
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User not found")
        );
        return userMapper.toDto(user);
    }

    public Page<UserResponseDTO> getAllUsers(Pageable pageable, String name, String surname){
        Specification<UserEntity> spec = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));

        Page<UserEntity> user = userRepository.findAll(spec, pageable);
        return user.map(userMapper::toDto);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO){
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User not found")
        );
        if (!user.getEmail().equals(requestDTO.email())) {
            if (userRepository.existsByEmail(requestDTO.email())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(requestDTO.email());
        }

        user.setName(requestDTO.name());
        user.setSurname(requestDTO.surname());
        user.setBirthDate(requestDTO.birthDate());

        return userMapper.toDto(userRepository.save(user));
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
