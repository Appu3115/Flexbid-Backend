package com.example.flexbid.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.BiddingTimeSlot;

@Repository
public interface BiddingTimeSlotRepository extends JpaRepository<BiddingTimeSlot, Integer> {
	List<BiddingTimeSlot> findBySlotDate(LocalDate date);

    Optional<BiddingTimeSlot> findBySlotDateAndSlotStartTimeBeforeAndSlotEndTimeAfter(LocalDate slotDate, LocalTime slotStartTime, LocalTime slotEndTime);

    @Query("SELECT b FROM BiddingTimeSlot b " +
            "WHERE (b.slotDate > :today) " +
            "   OR (b.slotDate = :today AND b.slotStartTime > :now) " +
            "ORDER BY b.slotDate ASC, b.slotStartTime ASC")
     Optional<BiddingTimeSlot> findNextAvailableSlot(@Param("today") LocalDate today,
                                                     @Param("now") LocalTime now);
    
    void deleteBySlotDateBefore(LocalDate date);

	List<BiddingTimeSlot> findBySlotDateBetween(LocalDate today, LocalDate endDate);

}