package com.example.slotbooking.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingRequest(
        @NotBlank String bookerName,
        String memberNames
) {
}
