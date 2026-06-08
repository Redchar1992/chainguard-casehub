package com.chainguard.auth.controller;

import com.chainguard.auth.audit.AuditService;
import com.chainguard.auth.dto.LoginRequest;
import com.chainguard.auth.dto.LoginResponse;
import com.chainguard.auth.service.AuthenticatedUser;
import com.chainguard.auth.service.JwtTokenService;
import com.chainguard.auth.service.UserAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAuthenticationService authenticationService;
    private final JwtTokenService jwtTokenService;
    private final AuditService auditService;

    public AuthController(
            UserAuthenticationService authenticationService,
            JwtTokenService jwtTokenService,
            AuditService auditService
    ) {
        this.authenticationService = authenticationService;
        this.jwtTokenService = jwtTokenService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser user = authenticationService.authenticate(request.username(), request.password());

        String token = jwtTokenService.issueToken(user.id().toString(), user.username(), user.roles());
        auditService.record(
                user.id(),
                "USER_LOGIN",
                "USER",
                user.id().toString(),
                Map.of("username", user.username(), "roles", user.roles())
        );

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.expirationSeconds(),
                user.id().toString(),
                user.roles()
        );
    }

    @GetMapping("/health")
    public String health() {
        return "auth-service ok";
    }
}
