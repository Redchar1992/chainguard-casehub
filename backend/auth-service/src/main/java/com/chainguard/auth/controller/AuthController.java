package com.chainguard.auth.controller;

import com.chainguard.auth.dto.LoginRequest;
import com.chainguard.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // MVP mock token. Replace with Spring Security + JWT implementation.
        return new LoginResponse(
                "demo-jwt-token-for-" + request.username(),
                "Bearer",
                3600,
                UUID.nameUUIDFromBytes(request.username().getBytes()).toString(),
                List.of("ANALYST", "REVIEWER")
        );
    }

    @GetMapping("/health")
    public String health() {
        return "auth-service ok";
    }
}
