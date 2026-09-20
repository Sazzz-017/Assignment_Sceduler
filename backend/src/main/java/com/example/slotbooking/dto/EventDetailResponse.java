package com.example.slotbooking.dto;

import com.example.slotbooking.model.EventType;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String name,
        EventType type,
        String description,
        List<SlotResponse> slots
) {
}
