package com.example.slotbooking.service;

import com.example.slotbooking.dto.BookingRequest;
import com.example.slotbooking.dto.BookingResponse;
import com.example.slotbooking.exception.DuplicateBookingException;
import com.example.slotbooking.exception.ForbiddenException;
import com.example.slotbooking.exception.NotFoundException;
import com.example.slotbooking.exception.SlotAlreadyBookedException;
import com.example.slotbooking.model.Booking;
import com.example.slotbooking.model.Slot;
import com.example.slotbooking.repository.BookingRepository;
import com.example.slotbooking.repository.EventRepository;
import com.example.slotbooking.repository.SlotRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;

    public BookingService(SlotRepository slotRepository,
                          BookingRepository bookingRepository,
                          EventRepository eventRepository) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Books a slot. The no-override guarantee is enforced at the database level:
     * the UNIQUE(slot_id) constraint means a concurrent second insert fails, so exactly
     * one booker wins even under a simultaneous race. The pre-checks below only exist to
     * return friendlier messages in the common (non-racing) case.
     */
    @Transactional
    public BookingResponse book(Long slotId, BookingRequest request) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
        Long eventId = slot.getEvent().getId();
        String bookerName = request.bookerName().trim();

        if (bookingRepository.findBySlotId(slotId).isPresent()) {
            throw new SlotAlreadyBookedException("This slot has already been booked.");
        }
        if (bookingRepository.existsByEventIdAndBookerName(eventId, bookerName)) {
            throw new DuplicateBookingException("You already have a booking in this event.");
        }

        Booking booking = new Booking(slot, eventId, bookerName, request.memberNames());
        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            String detail = rootMessage(ex).toLowerCase();
            if (detail.contains("uk_booking_event_booker")) {
                throw new DuplicateBookingException("You already have a booking in this event.");
            }
            throw new SlotAlreadyBookedException("This slot was just booked by someone else.");
        }
    }

    /** Self-service: attach or update a meeting link (e.g. Google Meet) on your own booking. */
    @Transactional
    public BookingResponse setMeetingLink(Long bookingId, String bookerName, String meetingLink) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        if (bookerName == null || !booking.getBookerName().equalsIgnoreCase(bookerName.trim())) {
            throw new ForbiddenException("Booker name does not match; cannot edit this booking.");
        }
        String trimmed = meetingLink == null ? null : meetingLink.trim();
        booking.setMeetingLink(trimmed == null || trimmed.isEmpty() ? null : trimmed);
        return toResponse(bookingRepository.save(booking));
    }

    /** Self-cancel: only allowed when the supplied name matches the booking's booker. */
    @Transactional
    public void cancel(Long bookingId, String bookerName) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        if (bookerName == null || !booking.getBookerName().equalsIgnoreCase(bookerName.trim())) {
            throw new ForbiddenException("Booker name does not match; cannot cancel this booking.");
        }
        bookingRepository.delete(booking);
    }

    /** Professor override: clears any booking regardless of booker. */
    @Transactional
    public void adminClear(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event " + eventId + " not found");
        }
        Map<Long, Booking> bookingsBySlot = bookingRepository.findByEventId(eventId).stream()
                .collect(Collectors.toMap(b -> b.getSlot().getId(), Function.identity()));
        List<Slot> slots = slotRepository.findByEventIdOrderByStartTimeAsc(eventId);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        StringBuilder sb = new StringBuilder();
        sb.append("startTime,durationMinutes,label,bookerName,memberNames,meetingLink,bookedAt\n");
        for (Slot slot : slots) {
            Booking b = bookingsBySlot.get(slot.getId());
            sb.append(csv(fmt.format(slot.getStartTime()))).append(',')
              .append(slot.getDurationMinutes()).append(',')
              .append(csv(slot.getLabel())).append(',')
              .append(csv(b != null ? b.getBookerName() : "")).append(',')
              .append(csv(b != null ? b.getMemberNames() : "")).append(',')
              .append(csv(b != null ? b.getMeetingLink() : "")).append(',')
              .append(csv(b != null ? fmt.format(b.getCreatedAt()) : "")).append('\n');
        }
        return sb.toString();
    }

    private static BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getSlot().getId(),
                b.getEventId(),
                b.getBookerName(),
                b.getMemberNames(),
                b.getMeetingLink(),
                b.getCreatedAt());
    }

    private static String csv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String rootMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        int guard = 0;
        while (current != null && guard++ < 10) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
