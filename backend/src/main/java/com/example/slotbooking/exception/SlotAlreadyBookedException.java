package com.example.slotbooking.exception;

/** 409 — the slot is already booked (no-override guarantee). */
public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}
