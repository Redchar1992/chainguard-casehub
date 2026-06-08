package com.chainguard.casehub.service;

/** Raised when a case status change violates the allowed lifecycle transitions. */
public class IllegalStatusTransitionException extends RuntimeException {
    public IllegalStatusTransitionException(String message) {
        super(message);
    }
}
