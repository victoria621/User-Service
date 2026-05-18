package com.innowise.userService.mapper;

import com.innowise.userService.dto.CardRequestDTO;
import com.innowise.userService.dto.CardResponseDTO;
import com.innowise.userService.entity.PaymentCardsEntity;
import org.mapstruct.Mapper;

import javax.smartcardio.Card;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {
    PaymentCardsEntity toEntity(CardRequestDTO dto);
    CardResponseDTO toDto(PaymentCardsEntity entity);
    List<CardResponseDTO> toDtoList(List<PaymentCardsEntity> entities);
}
