package com.example.slotbooking.exception;

/** 403 — caller is not allowed to perform this action (e.g. cancelling someone else's booking). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
