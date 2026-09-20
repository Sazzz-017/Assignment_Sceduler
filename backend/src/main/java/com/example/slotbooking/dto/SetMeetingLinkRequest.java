package com.example.slotbooking.dto;

import jakarta.validation.constraints.NotBlank;

public record SetMeetingLinkRequest(
        @NotBlank String bookerName,
        String meetingLink
) {
}
