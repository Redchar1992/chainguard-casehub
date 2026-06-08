package com.chainguard.auth.controller;

import com.chainguard.auth.dto.LoginRequest;
import com.chainguard.auth.dto.LoginResponse;
import com.chainguard.auth.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // MVP demo authentication. Any non-blank password is accepted.
        // The encoder call keeps the controller close to a real password verification flow.
        passwordEncoder.matches(request.password(), passwordEncoder.encode(request.password()));

        List<String> roles = inferDemoRoles(request.username());
        String token = jwtTokenService.issueToken(request.username(), roles);
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.expirationSeconds(),
                UUID.nameUUIDFromBytes(request.username().getBytes(StandardCharsets.UTF_8)).toString(),
                roles
        );
    }

    @GetMapping("/health")
    public String health() {
        return "auth-service ok";
    }

    private List<String> inferDemoRoles(String username) {
        if (username.startsWith("admin")) {
            return List.of("ADMIN", "ANALYST", "REVIEWER");
        }
        if (username.startsWith("reviewer")) {
            return List.of("REVIEWER");
        }
        return List.of("ANALYST");
    }
}
