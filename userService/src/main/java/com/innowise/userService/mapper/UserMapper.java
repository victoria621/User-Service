package com.innowise.userService.mapper;

import com.innowise.userService.dto.UserRequestDTO;
import com.innowise.userService.dto.UserResponseDTO;
import com.innowise.userService.entity.UserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity(UserRequestDTO dto);

    UserResponseDTO toDto(UserEntity entity);

    List<UserResponseDTO> toDtoList(List<UserEntity> entities);

}
