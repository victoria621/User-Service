package com.innowise.user_service.mapper;

import com.innowise.user_service.dto.CardRequestDTO;
import com.innowise.user_service.dto.CardResponseDTO;
import com.innowise.user_service.entity.PaymentCardsEntity;
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
