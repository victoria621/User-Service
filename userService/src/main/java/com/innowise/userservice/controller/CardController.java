package com.innowise.userservice.controller;

import com.innowise.userservice.dto.CardRequestDTO;
import com.innowise.userservice.dto.CardResponseDTO;
import com.innowise.userservice.service.CardService;
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
@RequestMapping("/api/cards")
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

    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDTO> getCardById(
            @PathVariable Long id
    ){
        log.info("getCardById");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.getCardById(id));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<CardResponseDTO>> getCardByUserId(
            @PathVariable Long userId
    ){
        log.info("getCardByUserId");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.getCardsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<CardResponseDTO>> getAllCards(
            Pageable pageable
    ) {
        log.info("getAllCards");
        Page<CardResponseDTO> page = cardService.getAllCards(pageable);
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardRequestDTO card
    ){
        log.info("updateCard");
        return ResponseEntity.status(HttpStatus.OK)
                .body(cardService.updateCard(id, card));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable("id") Long id
    ){
        log.info("Activate card");
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable("id") Long id
    ){
        log.info("Deactivate card");
        cardService.deactivateCard(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        log.info("REST request to delete Card : {}", id);
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
