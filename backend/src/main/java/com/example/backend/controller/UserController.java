package com.example.backend.controller;

import com.example.backend.dto.UserDto;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** POST /api/users */
    @PostMapping
    public ResponseEntity<UserDto.Response> create(@Valid @RequestBody UserDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    /** GET /api/users?professor=true|false */
    @GetMapping
    public ResponseEntity<List<UserDto.Response>> list(
            @RequestParam(required = false) Boolean professor) {

        List<UserDto.Response> result;
        if (professor == null) result = userService.findAll();
        else if (professor)    result = userService.findProfessors();
        else                   result = userService.findStudents();

        return ResponseEntity.ok(result);
    }

    /** GET /api/users/{uuid} */
    @GetMapping("/{uuid}")
    public ResponseEntity<UserDto.Response> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(userService.findByUuid(uuid));
    }

    /** PATCH /api/users/{uuid} */
    @PatchMapping("/{uuid}")
    public ResponseEntity<UserDto.Response> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UserDto.UpdateRequest req) {
        return ResponseEntity.ok(userService.update(uuid, req));
    }

    /** DELETE /api/users/{uuid} */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        userService.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}