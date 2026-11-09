package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.model.Role;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtTokenProvider;

import lombok.AllArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@AllArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    // public AuthService(
    //         UserRepository userRepository,
    //         PasswordEncoder passwordEncoder,
    //         JwtTokenProvider jwtTokenProvider,
    //         StringRedisTemplate redisTemplate) {
    //     this.userRepository = userRepository;
    //     this.passwordEncoder = passwordEncoder;
    //     this.jwtTokenProvider = jwtTokenProvider;
    //     this.redisTemplate = redisTemplate;
    // }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.ROLE_CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        // Cache session token in Redis with TTL for fast token validation / logout blacklisting
        saveTokenInRedis(savedUser.getId(), token);

        return AuthResponse.of(
                token,
                jwtTokenProvider.getExpirationMs(),
                new UserSummaryDto(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole())
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        saveTokenInRedis(user.getId(), token);

        return AuthResponse.of(
                token,
                jwtTokenProvider.getExpirationMs(),
                new UserSummaryDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole())
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return new UserSummaryDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    // Redis cache
    private void saveTokenInRedis(
        Long userId, 
        String token
    ) {
        String redisKey = "auth:session:" + userId;
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        redisTemplate
            .opsForValue()
            .set(redisKey, token, Duration.ofSeconds(ttlSeconds));
    }
}
