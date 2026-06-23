package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.CardRequestDTO;
import com.innowise.userservice.dto.CardResponseDTO;
import com.innowise.userservice.entity.PaymentCardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {
    PaymentCardEntity toEntity(CardRequestDTO dto);
    @Mapping(target = "userId", source = "user.id")
    CardResponseDTO toDto(PaymentCardEntity entity);
    List<CardResponseDTO> toDtoList(List<PaymentCardEntity> entities);
}
