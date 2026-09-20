package com.example.slotbooking.repository;

import com.example.slotbooking.model.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findBySlotId(Long slotId);

    List<Booking> findByEventId(Long eventId);

    long countByEventId(Long eventId);

    boolean existsByEventIdAndBookerName(Long eventId, String bookerName);
}
