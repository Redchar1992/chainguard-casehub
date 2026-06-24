package com.chainguard.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Fails loud when the shared demo JWT secret is left in place outside the
 * dev/test profiles. The secret is shared by every ChainGuard service, so using
 * the well-known default in a real deployment means any holder of this source
 * tree can forge tokens for every role. We warn rather than crash so existing
 * demo/dev runs keep working, but the warning is impossible to miss in logs.
 */
@Component
public class JwtSecretGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);
    static final String DEFAULT_SECRET = "chainguard-demo-secret-key-must-be-at-least-32-bytes";
    private static final Set<String> SAFE_PROFILES = Set.of("dev", "test", "local");

    private final String secret;
    private final Environment environment;

    public JwtSecretGuard(@Value("${security.jwt.secret}") String secret, Environment environment) {
        this.secret = secret;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!DEFAULT_SECRET.equals(secret)) {
            return;
        }
        boolean safeProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch(SAFE_PROFILES::contains);
        if (safeProfile) {
            return;
        }
        log.warn("SECURITY WARNING: the default shared JWT secret is in use with no dev/test/local profile active. "
                + "Set SECURITY_JWT_SECRET to a unique, secret value before any non-development deployment; "
                + "the default is public and lets anyone forge tokens for any role.");
    }
}
