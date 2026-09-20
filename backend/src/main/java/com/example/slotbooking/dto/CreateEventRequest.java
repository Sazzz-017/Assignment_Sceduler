package com.example.slotbooking.dto;

import com.example.slotbooking.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
        @NotBlank String name,
        @NotNull EventType type,
        String description
) {
}
