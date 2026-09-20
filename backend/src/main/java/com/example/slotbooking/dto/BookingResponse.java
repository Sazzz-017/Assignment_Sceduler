package com.example.slotbooking.dto;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long slotId,
        Long eventId,
        String bookerName,
        String memberNames,
        String meetingLink,
        Instant createdAt
) {
}
