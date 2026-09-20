package com.example.slotbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "slot")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(length = 200)
    private String label;

    protected Slot() {
    }

    public Slot(Event event, Instant startTime, int durationMinutes, String label) {
        this.event = event;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getLabel() {
        return label;
    }
}
