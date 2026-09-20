package com.example.slotbooking.dto;

import com.example.slotbooking.model.EventType;

public record EventSummaryResponse(
        Long id,
        String name,
        EventType type,
        String description,
        long slotCount,
        long bookedCount
) {
}
