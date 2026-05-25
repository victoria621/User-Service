package com.innowise.userservice.service;

import com.innowise.userservice.dao.UserDAO;
import com.innowise.userservice.dto.UserRequestDTO;
import com.innowise.userservice.dto.UserResponseDTO;
import com.innowise.userservice.entity.UserEntity;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.specification.UserSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private final UserDAO userDAO;
    private final UserMapper userMapper;
    private final CardMapper cardMapper;
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    public UserService(UserDAO userDAO, UserMapper userMapper, CardMapper cardMapper) {
        this.userDAO = userDAO;
        this.userMapper = userMapper;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        if (userDAO.existsByEmail(requestDTO.email())) {
            throw new BusinessException("Email already exists");
        }
        UserEntity user = userMapper.toEntity(requestDTO);
        user.setActive(true);
        return userMapper.toDto(userDAO.save(user));
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getUserById(Long id) {
        UserEntity user = userDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE));

        UserResponseDTO dto = userMapper.toDto(user);
        if (user.getPaymentCards() != null) {
            dto = new UserResponseDTO(
                    dto.id(), dto.name(), dto.surname(), dto.birthDate(),
                    dto.email(), dto.active(), dto.createdAt(), dto.updatedAt(),
                    cardMapper.toDtoList(user.getPaymentCards())
            );
        }
        return dto;
    }

    public Page<UserResponseDTO> getAllUsers(Pageable pageable, String name, String surname) {
        Specification<UserEntity> spec = Specification.where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));

        return userDAO.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO)  {
        UserEntity user = userDAO.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE)
        );

        if (!user.getEmail().equals(requestDTO.email()) && userDAO.existsByEmail(requestDTO.email())) {
            throw new BusinessException("Email already exists");
        }

        user.setName(requestDTO.name());
        user.setSurname(requestDTO.surname());
        user.setBirthDate(requestDTO.birthDate());
        user.setEmail(requestDTO.email());

        return userMapper.toDto(userDAO.save(user));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        UserEntity user = userDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE));
        userDAO.delete(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void activateUser(Long id) {
        UserEntity user = userDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException (USER_NOT_FOUND_MESSAGE));
        user.setActive(true);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deactivateUser(Long id) {
        UserEntity user = userDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException (USER_NOT_FOUND_MESSAGE));
        user.setActive(false);
    }
}
