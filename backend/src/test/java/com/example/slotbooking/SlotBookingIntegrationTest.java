package com.example.slotbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.slotbooking.dto.BookingRequest;
import com.example.slotbooking.model.Event;
import com.example.slotbooking.model.EventType;
import com.example.slotbooking.model.Slot;
import com.example.slotbooking.repository.BookingRepository;
import com.example.slotbooking.repository.EventRepository;
import com.example.slotbooking.repository.SlotRepository;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SlotBookingIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private Long eventId;
    private Long slot1Id;
    private Long slot2Id;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        slotRepository.deleteAll();
        eventRepository.deleteAll();

        Event event = new Event();
        event.setName("Final Demos");
        event.setType(EventType.PRESENTATION);
        event = eventRepository.save(event);
        eventId = event.getId();

        slot1Id = slotRepository.save(new Slot(event, Instant.parse("2026-10-01T09:00:00Z"), 15, "Room A")).getId();
        slot2Id = slotRepository.save(new Slot(event, Instant.parse("2026-10-01T09:15:00Z"), 15, "Room A")).getId();
    }

    @Test
    void concurrentBookingsOnSameSlot_exactlyOneWins() throws Exception {
        int racers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(racers);
        CountDownLatch ready = new CountDownLatch(racers);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < racers; i++) {
            final String booker = "racer-" + i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    ResponseEntity<String> resp = rest.postForEntity(
                            "/api/slots/" + slot1Id + "/bookings",
                            new BookingRequest(booker, null),
                            String.class);
                    if (resp.getStatusCode() == HttpStatus.CREATED) {
                        created.incrementAndGet();
                    } else if (resp.getStatusCode() == HttpStatus.CONFLICT) {
                        conflict.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

        assertThat(created.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(racers - 1);
        assertThat(other.get()).isZero();
        assertThat(bookingRepository.findByEventId(eventId)).hasSize(1);
    }

    @Test
    void bookingAlreadyTakenSlot_returns409() {
        ResponseEntity<String> first = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Alice", null), String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Bob", null), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sameBookerTwiceInEvent_returns409() {
        rest.postForEntity("/api/slots/" + slot1Id + "/bookings", new BookingRequest("Alice", null), String.class);

        ResponseEntity<String> second = rest.postForEntity(
                "/api/slots/" + slot2Id + "/bookings", new BookingRequest("Alice", null), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelFreesSlotForRebooking() {
        ResponseEntity<BookingResponseView> booked = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Bob", null), BookingResponseView.class);
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long bookingId = booked.getBody().id();

        rest.delete("/api/bookings/" + bookingId + "?bookerName=Bob");

        ResponseEntity<String> rebook = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Carol", null), String.class);
        assertThat(rebook.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void cancelWithWrongName_isForbiddenAndKeepsBooking() {
        ResponseEntity<BookingResponseView> booked = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Dan", null), BookingResponseView.class);
        Long bookingId = booked.getBody().id();

        ResponseEntity<String> resp = rest.exchange(
                "/api/bookings/" + bookingId + "?bookerName=Eve",
                org.springframework.http.HttpMethod.DELETE, null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bookingRepository.existsById(bookingId)).isTrue();
    }

    @Test
    void adminEndpointWithoutPasscode_returns401() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/admin/events/" + eventId,
                org.springframework.http.HttpMethod.DELETE, null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(eventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void adminEndpointWithCorrectPasscode_succeeds() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Admin-Passcode", "test-pass");
        ResponseEntity<String> resp = rest.exchange(
                "/api/admin/slots/" + slot2Id,
                org.springframework.http.HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(slotRepository.existsById(slot2Id)).isFalse();
    }

    @Test
    void ownerCanAttachMeetingLink() {
        ResponseEntity<BookingResponseView> booked = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Grace", null), BookingResponseView.class);
        Long bookingId = booked.getBody().id();

        ResponseEntity<String> put = rest.exchange(
                "/api/bookings/" + bookingId + "/meeting-link",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(new MeetingLinkBody("Grace", "https://meet.google.com/abc-defg-hij")),
                String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);

        EventView detail = rest.getForObject("/api/events/" + eventId, EventView.class);
        SlotView booledSlot = detail.slots().stream().filter(s -> s.id().equals(slot1Id)).findFirst().orElseThrow();
        assertThat(booledSlot.meetingLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
    }

    @Test
    void nonOwnerCannotAttachMeetingLink() {
        ResponseEntity<BookingResponseView> booked = rest.postForEntity(
                "/api/slots/" + slot1Id + "/bookings", new BookingRequest("Grace", null), BookingResponseView.class);
        Long bookingId = booked.getBody().id();

        ResponseEntity<String> put = rest.exchange(
                "/api/bookings/" + bookingId + "/meeting-link",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(new MeetingLinkBody("Mallory", "https://meet.google.com/evil")),
                String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** Minimal view of the booking JSON for tests. */
    record BookingResponseView(Long id, Long slotId, Long eventId, String bookerName) {
    }

    record MeetingLinkBody(String bookerName, String meetingLink) {
    }

    record EventView(Long id, java.util.List<SlotView> slots) {
    }

    record SlotView(Long id, boolean booked, String bookerName, String meetingLink) {
    }
}
