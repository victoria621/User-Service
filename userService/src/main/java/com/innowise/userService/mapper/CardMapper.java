package com.innowise.userService.mapper;

import com.innowise.userService.dto.CardRequestDTO;
import com.innowise.userService.dto.CardResponseDTO;
import com.innowise.userService.entity.PaymentCardsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {
    PaymentCardsEntity toEntity(CardRequestDTO dto);
    @Mapping(target = "userId", source = "user.id")
    CardResponseDTO toDto(PaymentCardsEntity entity);
    List<CardResponseDTO> toDtoList(List<PaymentCardsEntity> entities);
}
