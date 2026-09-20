package com.example.slotbooking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record CreateSlotRequest(
        @NotNull Instant startTime,
        @NotNull @Positive Integer durationMinutes,
        String label
) {
}
