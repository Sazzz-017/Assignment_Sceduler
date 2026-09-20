package com.example.slotbooking.exception;

/** 401 — missing or invalid admin passcode. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
