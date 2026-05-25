package com.innowise.userservice.service;

import com.innowise.userservice.dao.CardDAO;
import com.innowise.userservice.dao.UserDAO;
import com.innowise.userservice.dto.CardRequestDTO;
import com.innowise.userservice.dto.CardResponseDTO;
import com.innowise.userservice.entity.PaymentCardEntity;
import com.innowise.userservice.entity.UserEntity;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private final CardDAO cardDao;
    private final UserDAO userDao;
    private final CardMapper cardMapper;

    private static final String USER_NOT_FOUND = "User with id %d not found";
    private static final String CARD_NOT_FOUND = "Card not found";
    private static final int MAX_CARDS = 3;

    public CardService(CardDAO cardDao, UserDAO userDao, CardMapper cardMapper) {
        this.cardDao = cardDao;
        this.userDao = userDao;
        this.cardMapper = cardMapper;
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public CardResponseDTO createCard(CardRequestDTO requestDTO, Long userId) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId)));

        if (cardDao.countByUserId(userId) >= MAX_CARDS) {
            throw new BusinessException("User already has maximum number of cards");
        }

        if (cardDao.existsByNumber(requestDTO.number())) {
            throw new BusinessException("Card number already exists");
        }

        PaymentCardEntity card = cardMapper.toEntity(requestDTO);
        card.setUser(user);
        card.setActive(true);

        return cardMapper.toDto(cardDao.save(card));
    }

    @Cacheable(value = "cards", key = "#id")
    public CardResponseDTO getCardById(Long id) {
        return cardDao.findById(id)
                .map(cardMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
    }

    public List<CardResponseDTO> getCardsByUserId(Long userId) {
        return cardMapper.toDtoList(cardDao.findByUserId(userId));
    }

    public Page<CardResponseDTO> getAllCards(Pageable pageable) {
        return cardDao.findAll(pageable).map(cardMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = {"cards", "users"}, key = "#result.userId")
    public CardResponseDTO updateCard(Long id, CardRequestDTO dto) {
        PaymentCardEntity card = cardDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));

        if (!card.getNumber().equals(dto.number()) && cardDao.existsByNumber(dto.number())) {
            throw new BusinessException("Card number already exists");
        }

        card.setNumber(dto.number());
        card.setHolder(dto.holder());
        card.setExpirationDate(dto.expirationDate());

        return cardMapper.toDto(cardDao.save(card));
    }

    @Transactional
    @CacheEvict(value = {"cards", "users"}, key = "#result.userId")
    public void deleteCard(Long id) {
        PaymentCardEntity card = cardDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        Long userId = card.getUser().getId();
        cardDao.delete(card);
        evictUserCache(userId);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void evictUserCache(Long userId) {
    }

    @Transactional
    @CacheEvict(value = {"cards", "users"})
    public void activateCard(Long id) {
        PaymentCardEntity card = cardDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
        card.setActive(true);
        cardDao.save(card);
    }

    @Transactional
    @CacheEvict(value = {"cards", "users"})
    public void deactivateCard(Long id) {
        PaymentCardEntity card = cardDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
        card.setActive(false);
        cardDao.save(card);
    }
}