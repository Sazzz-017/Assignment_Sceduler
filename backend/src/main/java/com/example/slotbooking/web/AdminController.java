package com.example.slotbooking.web;

import com.example.slotbooking.dto.CreateEventRequest;
import com.example.slotbooking.dto.CreateSlotRequest;
import com.example.slotbooking.dto.EventDetailResponse;
import com.example.slotbooking.dto.SlotResponse;
import com.example.slotbooking.dto.UpdateEventRequest;
import com.example.slotbooking.service.BookingService;
import com.example.slotbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EventService eventService;
    private final BookingService bookingService;

    public AdminController(EventService eventService, BookingService bookingService) {
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetailResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/events/{id}")
    public EventDetailResponse updateEvent(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/events/{id}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse addSlot(@PathVariable Long id, @Valid @RequestBody CreateSlotRequest request) {
        return eventService.addSlot(id, request);
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        eventService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> clearBooking(@PathVariable Long id) {
        bookingService.adminClear(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{id}/export.csv")
    public ResponseEntity<String> exportCsv(@PathVariable Long id) {
        String csv = bookingService.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event-" + id + "-bookings.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
