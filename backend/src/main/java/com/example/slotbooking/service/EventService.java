package com.example.slotbooking.service;

import com.example.slotbooking.dto.CreateEventRequest;
import com.example.slotbooking.dto.CreateSlotRequest;
import com.example.slotbooking.dto.EventDetailResponse;
import com.example.slotbooking.dto.EventSummaryResponse;
import com.example.slotbooking.dto.SlotResponse;
import com.example.slotbooking.dto.UpdateEventRequest;
import com.example.slotbooking.exception.NotFoundException;
import com.example.slotbooking.model.Booking;
import com.example.slotbooking.model.Event;
import com.example.slotbooking.model.Slot;
import com.example.slotbooking.repository.BookingRepository;
import com.example.slotbooking.repository.EventRepository;
import com.example.slotbooking.repository.SlotRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    public EventService(EventRepository eventRepository,
                        SlotRepository slotRepository,
                        BookingRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> listEvents() {
        return eventRepository.findAll().stream()
                .map(event -> {
                    long slotCount = event.getSlots().size();
                    long bookedCount = bookingRepository.countByEventId(event.getId());
                    return new EventSummaryResponse(
                            event.getId(),
                            event.getName(),
                            event.getType(),
                            event.getDescription(),
                            slotCount,
                            bookedCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));

        Map<Long, Booking> bookingsBySlot = bookingRepository.findByEventId(eventId).stream()
                .collect(Collectors.toMap(b -> b.getSlot().getId(), Function.identity()));

        List<SlotResponse> slots = slotRepository.findByEventIdOrderByStartTimeAsc(eventId).stream()
                .map(slot -> {
                    Booking booking = bookingsBySlot.get(slot.getId());
                    return new SlotResponse(
                            slot.getId(),
                            slot.getStartTime(),
                            slot.getDurationMinutes(),
                            slot.getLabel(),
                            booking != null,
                            booking != null ? booking.getBookerName() : null,
                            booking != null ? booking.getId() : null,
                            booking != null ? booking.getMeetingLink() : null);
                })
                .toList();

        return new EventDetailResponse(
                event.getId(),
                event.getName(),
                event.getType(),
                event.getDescription(),
                slots);
    }

    @Transactional
    public EventDetailResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setType(request.type());
        event.setDescription(request.description());
        Event saved = eventRepository.save(event);
        return getEvent(saved.getId());
    }

    @Transactional
    public EventDetailResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        event.setName(request.name());
        event.setType(request.type());
        event.setDescription(request.description());
        eventRepository.save(event);
        return getEvent(eventId);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event " + eventId + " not found");
        }
        eventRepository.deleteById(eventId);
    }

    @Transactional
    public SlotResponse addSlot(Long eventId, CreateSlotRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        Slot slot = new Slot(event, request.startTime(), request.durationMinutes(), request.label());
        Slot saved = slotRepository.save(slot);
        return new SlotResponse(
                saved.getId(),
                saved.getStartTime(),
                saved.getDurationMinutes(),
                saved.getLabel(),
                false,
                null,
                null,
                null);
    }

    @Transactional
    public void deleteSlot(Long slotId) {
        if (!slotRepository.existsById(slotId)) {
            throw new NotFoundException("Slot " + slotId + " not found");
        }
        bookingRepository.findBySlotId(slotId).ifPresent(bookingRepository::delete);
        slotRepository.deleteById(slotId);
    }
}
