package com.chainguard.auth.service;

import com.chainguard.auth.user.Role;
import com.chainguard.auth.user.User;
import com.chainguard.auth.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Authenticates a user against the {@code users} table: looks up the username,
 * verifies the supplied password against the stored BCrypt hash, and derives
 * roles from {@code user_roles}. Roles are no longer inferred from the username.
 */
@Service
public class UserAuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new AuthenticatedUser(user.getId(), user.getUsername(), roles);
    }
}
