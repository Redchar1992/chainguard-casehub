package com.chainguard.auth.service;

/** Raised when a login attempt fails username lookup or password verification. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
