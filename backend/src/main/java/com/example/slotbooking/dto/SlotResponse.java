package com.example.slotbooking.dto;

import java.time.Instant;

public record SlotResponse(
        Long id,
        Instant startTime,
        int durationMinutes,
        String label,
        boolean booked,
        String bookerName,
        Long bookingId,
        String meetingLink
) {
}
