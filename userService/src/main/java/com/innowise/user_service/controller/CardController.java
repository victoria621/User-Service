package com.innowise.user_service.controller;

import com.innowise.user_service.dto.CardRequestDTO;
import com.innowise.user_service.dto.CardResponseDTO;
import com.innowise.user_service.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService cardService;
    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/users/{userId}/cards")
    public ResponseEntity<CardResponseDTO> createCard(
            @PathVariable Long userId,
            @Valid @RequestBody CardRequestDTO card
    ){
        log.info("createCard");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.createCard(card,userId));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<CardResponseDTO> getCardById(
            @PathVariable Long id
    ){
        log.info("getCardById");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.getCardById(id));
    }

    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<List<CardResponseDTO>> getCardByUserId(
            @PathVariable Long userId
    ){
        log.info("getCardByUserId");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.getCardsByUserId(userId));
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<CardResponseDTO>> getAllCards(
            Pageable pageable
    ) {
        log.info("getAllCards");
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardRequestDTO card
    ){
        log.info("updateCard");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.updateCard(id, card));
    }

    @PatchMapping("/cards/{id}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable("id") Long id
    ){
        log.info("Activate User");
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cards/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable("id") Long id
    ){
        log.info("Deactivate User");
        cardService.deactivateCard(id);
        return ResponseEntity.noContent().build();
    }
}
