package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.security.JwtTokenProvider;
import com.ecommerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor 
@Slf4j 
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and session profile")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user profile and returns a signed JWT session token")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or email already taken")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and caches session token in Redis")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Resolves authenticated user info from Bearer JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile resolved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
    })
    public ResponseEntity<UserSummaryDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized request to /me: missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Unauthorized request to /me: invalid or expired JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    // Exception Handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request handled: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
