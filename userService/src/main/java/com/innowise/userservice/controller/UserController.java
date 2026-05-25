package com.innowise.userservice.controller;

import com.innowise.userservice.dto.UserRequestDTO;
import com.innowise.userservice.dto.UserResponseDTO;
import com.innowise.userservice.service.UserService;
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
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @RequestBody @Valid UserRequestDTO dto
    ) {
        log.info("Create User");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable("id") Long id
    ) {
        log.info("Get User");
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname
    ) {
        log.info("Get All Users");
        Page<UserResponseDTO> page = userService.getAllUsers(pageable, name, surname);
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable("id") Long id,
            @RequestBody @Valid UserRequestDTO dto
    ){
        log.info("Update User");
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.updateUser(id, dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable("id") Long id
    ) {
        log.info("Activate User");
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable("id") Long id
    ) {
        log.info("Deactivate User");
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("REST request to delete User : {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }
}
