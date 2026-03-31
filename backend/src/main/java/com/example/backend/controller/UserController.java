package com.example.backend.controller;

import com.example.backend.dto.UserDto;
import com.example.backend.service.SessionAuthService;
import com.example.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SessionAuthService sessionAuthService;

    public UserController(UserService userService, SessionAuthService sessionAuthService) {
        this.userService = userService;
        this.sessionAuthService = sessionAuthService;
    }

    /** POST /api/users */
    @PostMapping
    public ResponseEntity<UserDto.Response> create(
            @Valid @RequestBody UserDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        sessionAuthService.requireAuthenticatedUser(httpRequest);
        throw new ResponseStatusException(FORBIDDEN, "Accounts can only be created through the sign-up page.");
    }

    /** GET /api/users?professor=true|false */
    @GetMapping
    public ResponseEntity<Void> list(
            @RequestParam(required = false) Boolean professor,
            HttpServletRequest httpRequest) {
        sessionAuthService.requireAuthenticatedUser(httpRequest);
        throw new ResponseStatusException(FORBIDDEN, "You do not have access to view the user list.");
    }

    /** GET /api/users/{uuid} */
    @GetMapping("/{uuid}") 
    public ResponseEntity<UserDto.Response> get(@PathVariable UUID uuid, HttpServletRequest httpRequest) {
        ensureCurrentUserOwns(uuid, httpRequest);
        return ResponseEntity.ok(userService.findByUuid(uuid));
    }

    /** PATCH /api/users/{uuid} */
    @PatchMapping("/{uuid}")
    public ResponseEntity<UserDto.Response> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UserDto.UpdateRequest req,
            HttpServletRequest httpRequest) {
        ensureCurrentUserOwns(uuid, httpRequest);
        return ResponseEntity.ok(userService.update(uuid, req));
    }

    /** DELETE /api/users/{uuid} */
    @DeleteMapping("/{uuid}") 
    public ResponseEntity<Void> delete(@PathVariable UUID uuid, HttpServletRequest httpRequest) {
        sessionAuthService.requireAuthenticatedUser(httpRequest);
        throw new ResponseStatusException(FORBIDDEN, "User accounts cannot be deleted here.");
    }

    private void ensureCurrentUserOwns(UUID uuid, HttpServletRequest httpRequest) {
        Integer currentUserId = sessionAuthService.requireAuthenticatedUser(httpRequest).userId();
        Integer requestedUserId = userService.findUserIdByUuid(uuid);
        if (!requestedUserId.equals(currentUserId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only view or update your own profile.");
        }
    }
}
