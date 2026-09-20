package com.example.slotbooking.repository;

import com.example.slotbooking.model.Slot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByEventIdOrderByStartTimeAsc(Long eventId);
}
