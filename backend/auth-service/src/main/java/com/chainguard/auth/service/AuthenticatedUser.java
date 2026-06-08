package com.chainguard.auth.service;

import java.util.List;
import java.util.UUID;

/** Result of a successful authentication: the user identity and granted roles. */
public record AuthenticatedUser(UUID id, String username, List<String> roles) {
}
