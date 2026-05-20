package com.innowise.user_service.mapper;

import com.innowise.user_service.dto.UserRequestDTO;
import com.innowise.user_service.dto.UserResponseDTO;
import com.innowise.user_service.entity.UserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity(UserRequestDTO dto);

    UserResponseDTO toDto(UserEntity entity);

    List<UserResponseDTO> toDtoList(List<UserEntity> entities);

}
