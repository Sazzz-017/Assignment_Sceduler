package com.example.slotbooking.exception;

/** 409 — booker already has a booking in this event (one booking per event). */
public class DuplicateBookingException extends RuntimeException {
    public DuplicateBookingException(String message) {
        super(message);
    }
}
