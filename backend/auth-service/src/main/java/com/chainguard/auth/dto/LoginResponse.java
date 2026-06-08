package com.chainguard.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String userId,
        List<String> roles
) {}
