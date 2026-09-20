package com.example.slotbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "booking",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_slot", columnNames = "slot_id"),
                @UniqueConstraint(name = "uk_booking_event_booker", columnNames = {"event_id", "booker_name"})
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private Slot slot;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "booker_name", nullable = false)
    private String bookerName;

    @Column(name = "member_names", length = 2000)
    private String memberNames;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Booking() {
    }

    public Booking(Slot slot, Long eventId, String bookerName, String memberNames) {
        this.slot = slot;
        this.eventId = eventId;
        this.bookerName = bookerName;
        this.memberNames = memberNames;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Slot getSlot() {
        return slot;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getBookerName() {
        return bookerName;
    }

    public String getMemberNames() {
        return memberNames;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
