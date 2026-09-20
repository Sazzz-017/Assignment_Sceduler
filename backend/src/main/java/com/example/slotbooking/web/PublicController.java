package com.example.slotbooking.web;

import com.example.slotbooking.dto.BookingRequest;
import com.example.slotbooking.dto.BookingResponse;
import com.example.slotbooking.dto.EventDetailResponse;
import com.example.slotbooking.dto.EventSummaryResponse;
import com.example.slotbooking.dto.SetMeetingLinkRequest;
import com.example.slotbooking.service.BookingService;
import com.example.slotbooking.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PublicController {

    private final EventService eventService;
    private final BookingService bookingService;

    public PublicController(EventService eventService, BookingService bookingService) {
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping("/events")
    public List<EventSummaryResponse> listEvents() {
        return eventService.listEvents();
    }

    @GetMapping("/events/{id}")
    public EventDetailResponse getEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping("/slots/{slotId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@PathVariable Long slotId, @Valid @RequestBody BookingRequest request) {
        return bookingService.book(slotId, request);
    }

    @PutMapping("/bookings/{id}/meeting-link")
    public BookingResponse setMeetingLink(@PathVariable Long id, @Valid @RequestBody SetMeetingLinkRequest request) {
        return bookingService.setMeetingLink(id, request.bookerName(), request.meetingLink());
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @RequestParam String bookerName) {
        bookingService.cancel(id, bookerName);
        return ResponseEntity.noContent().build();
    }
}
