package com.innowise.userservice.unit;

import com.innowise.userservice.dto.UserRequestDTO;
import com.innowise.userservice.dto.UserResponseDTO;
import com.innowise.userservice.entity.UserEntity;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserDAO;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_ShouldReturnUserDto_WhenUserExists() {
        Long userId = 1L;
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setName("John");
        userEntity.setSurname("Doe");
        userEntity.setEmail("john@mail.com");
        userEntity.setActive(true);

        UserResponseDTO expectedDto = new UserResponseDTO(
                userId, "John", "Doe", null, "john@mail.com",
                true, LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(expectedDto);

        UserResponseDTO result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.name()).isEqualTo("John");
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(userEntity);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void createUser_ShouldSaveUser_WhenEmailIsNew() {
        UserRequestDTO requestDTO = new UserRequestDTO(
                "John", "Doe", null, "john@mail.com"
        );

        UserEntity entityToSave = new UserEntity();
        entityToSave.setName("John");
        entityToSave.setSurname("Doe");
        entityToSave.setEmail("john@mail.com");

        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(1L);
        savedEntity.setName("John");
        savedEntity.setSurname("Doe");
        savedEntity.setEmail("john@mail.com");
        savedEntity.setActive(true);

        UserResponseDTO expectedDto = new UserResponseDTO(
                1L, "John", "Doe", null, "john@mail.com",
                true, LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(userMapper.toEntity(requestDTO)).thenReturn(entityToSave);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        UserResponseDTO result = userService.createUser(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.active()).isTrue();
        verify(userRepository).save(entityToSave);
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
        UserRequestDTO requestDTO = new UserRequestDTO(
                "John", "Doe", null, "existing@mail.com"
        );

        UserEntity entity = new UserEntity();
        entity.setEmail("existing@mail.com");

        when(userMapper.toEntity(requestDTO)).thenReturn(entity);
        when(userRepository.existsByEmail("existing@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_ShouldUpdateFields_WhenEmailNotChanged() {
        Long userId = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO(
                "UpdatedName", "UpdatedSurname", null, "old@mail.com"
        );

        UserEntity existingUser = new UserEntity();
        existingUser.setId(userId);
        existingUser.setName("Old");
        existingUser.setSurname("Old");
        existingUser.setEmail("old@mail.com");

        UserEntity updatedUser = new UserEntity();
        updatedUser.setId(userId);
        updatedUser.setName("UpdatedName");
        updatedUser.setSurname("UpdatedSurname");
        updatedUser.setEmail("old@mail.com");

        UserResponseDTO expectedDto = new UserResponseDTO(
                userId, "UpdatedName", "UpdatedSurname", null, "old@mail.com",
                true, LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);

        UserResponseDTO result = userService.updateUser(userId, requestDTO);

        assertThat(result.name()).isEqualTo("UpdatedName");
        assertThat(result.email()).isEqualTo("old@mail.com");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_ShouldUpdateEmail_WhenEmailChangedAndUnique() {
        Long userId = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO(
                "John", "Doe", null, "new@mail.com"
        );

        UserEntity existingUser = new UserEntity();
        existingUser.setId(userId);
        existingUser.setName("John");
        existingUser.setEmail("old@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, requestDTO);

        assertThat(existingUser.getEmail()).isEqualTo("new@mail.com");
        verify(userRepository).existsByEmail("new@mail.com");
    }

    @Test
    void updateUser_ShouldThrowException_WhenNewEmailAlreadyExists() {
        Long userId = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO(
                "John", "Doe", null, "existing@mail.com"
        );

        UserEntity existingUser = new UserEntity();
        existingUser.setId(userId);
        existingUser.setEmail("old@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("existing@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(userId, requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void activateUser_ShouldSetActiveToTrue() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.activateUser(userId);

        assertThat(user.getActive()).isTrue();
        verify(userRepository).findById(userId);
    }

    @Test
    void activateUser_ShouldThrowException_WhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivateUser(userId);

        assertThat(user.getActive()).isFalse();
        verify(userRepository).findById(userId);
    }

    @Test
    void deactivateUser_ShouldThrowException_WhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        String name = "John";
        String surname = "Doe";

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");

        Page<UserEntity> userPage = new PageImpl<>(List.of(user), pageable, 1);

        UserResponseDTO responseDto = new UserResponseDTO(
                1L, "John", "Doe", null, "john@mail.com",
                true, LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(responseDto);

        Page<UserResponseDTO> result = userService.getAllUsers(pageable, name, surname);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("John");
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }
}