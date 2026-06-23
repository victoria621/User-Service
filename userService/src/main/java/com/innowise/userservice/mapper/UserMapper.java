package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.UserRequestDTO;
import com.innowise.userservice.dto.UserResponseDTO;
import com.innowise.userservice.entity.UserEntity;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {CardMapper.class})
public interface UserMapper {
    UserEntity toEntity(UserRequestDTO dto);

    UserResponseDTO toDto(UserEntity entity);


}
