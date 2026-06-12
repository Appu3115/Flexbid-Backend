package com.example.flexbid.service;

//import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
//import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.BiddingSlotRequest;
import com.example.flexbid.dto.BiddingTimeRequest;
import com.example.flexbid.dto.BiddingTimeSlotDTO;
import com.example.flexbid.model.BiddingTimeSlot;
import com.example.flexbid.model.DailyBiddingTimeConfig;
import com.example.flexbid.repository.BiddingTimeSlotRepository;
import com.example.flexbid.repository.DailyBiddingTimeConfigRepository;

@Service
public class BiddingTimeService {

    @Autowired
    private DailyBiddingTimeConfigRepository dailyConfigRepository;

    @Autowired
    private BiddingTimeSlotRepository slotRepository;
    
    @Autowired
    private WebSocketNotificationService notificationService;

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    public ResponseEntity<?> setDailyBiddingTime(BiddingTimeRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return ResponseEntity.badRequest().body("Start and End time required");
        }

        LocalTime startTime;
        LocalTime endTime;

        try {
            startTime = parseFlexibleTime(request.getStartTime());
            endTime = parseFlexibleTime(request.getEndTime());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid time format. Please use HH:mm or HH:mm:ss.");
        }

        if (endTime.isBefore(startTime)) {
            return ResponseEntity.badRequest().body("End time must be after start time.");
        }

        DailyBiddingTimeConfig config = dailyConfigRepository.findById(1).orElse(new DailyBiddingTimeConfig());
        config.setId(1);
        config.setDailyStartTime(startTime);
        config.setDailyEndTime(endTime);
        config.setUpdatedAt(LocalDateTime.now(INDIA_ZONE));

        dailyConfigRepository.save(config);
        
        // ✅ Send WebSocket notification
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BIDDING_TIME_UPDATED");
        payload.put("startTime", startTime.toString());
        payload.put("endTime", endTime.toString());

        notificationService.notifyTopic("ADMIN", payload);

        return ResponseEntity.ok("Default daily bidding time set.");
    }


    public ResponseEntity<?> addBiddingSlot(BiddingSlotRequest request) {
        try {
            // 🔒 Validate required fields
            if (request.getStartTime() == null || request.getStartTime().isBlank() ||
                request.getEndTime() == null || request.getEndTime().isBlank()) {
                return ResponseEntity.badRequest().body("Start and end time are required.");
            }

            // Default to today if slotDate is null
            LocalDate slotDate = request.getSlotDate() != null ? request.getSlotDate() : LocalDate.now();

            // Parse times
            LocalTime startTime, endTime;
            try {
                startTime = LocalTime.parse(request.getStartTime());
                endTime = LocalTime.parse(request.getEndTime());
            } catch (DateTimeParseException ex) {
                return ResponseEntity.badRequest().body("Invalid time format. Use HH:mm:ss.");
            }

            boolean crossesMidnight = endTime.isBefore(startTime);

            // Calculate actual date-time window
            LocalDateTime slotStart = LocalDateTime.of(slotDate, startTime);
            LocalDateTime slotEnd = crossesMidnight
                    ? LocalDateTime.of(slotDate.plusDays(1), endTime)
                    : LocalDateTime.of(slotDate, endTime);

            // ❌ Validate slot duration
            if (!crossesMidnight && !slotEnd.isAfter(slotStart)) {
                return ResponseEntity.badRequest().body("End time must be after start time.");
            }

            // ❌ Check for overlaps with existing slots
            List<BiddingTimeSlot> allSlots = slotRepository.findBySlotDateBetween(slotDate.minusDays(1), slotDate.plusDays(1));
            for (BiddingTimeSlot slot : allSlots) {
                LocalDateTime existingStart = LocalDateTime.of(slot.getSlotDate(), slot.getSlotStartTime());
                LocalDateTime existingEnd = slot.getSlotEndTime().isBefore(slot.getSlotStartTime())
                        ? LocalDateTime.of(slot.getSlotDate().plusDays(1), slot.getSlotEndTime())
                        : LocalDateTime.of(slot.getSlotDate(), slot.getSlotEndTime());

                if (slotStart.isBefore(existingEnd) && slotEnd.isAfter(existingStart)) {
                    return ResponseEntity.badRequest().body("Slot overlaps with an existing one.");
                }
            }

            // ✅ Save slot (assign to the actual start date)
            BiddingTimeSlot newSlot = new BiddingTimeSlot();
            newSlot.setSlotDate(slotDate);
            newSlot.setSlotStartTime(startTime);
            newSlot.setSlotEndTime(endTime);
            newSlot.setCreatedAt(LocalDateTime.now());
            slotRepository.save(newSlot);

            // ✅ WebSocket Broadcast
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "BIDDING_SLOT_ADDED");
            payload.put("slotDate", slotDate.toString());
            payload.put("startTime", startTime.toString());
            payload.put("endTime", endTime.toString());
            payload.put("message", "A new bidding slot has been added.");
            notificationService.notifyTopic("notifications", payload);

            return ResponseEntity.ok("Bidding slot added successfully.");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to add bidding slot.");
        }
    }

    

        public ResponseEntity<?> getBiddingTimes() {
            try {
                Optional<DailyBiddingTimeConfig> optionalConfig = dailyConfigRepository.findById(1);

                if (optionalConfig.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Daily bidding configuration not found");
                }

                DailyBiddingTimeConfig config = optionalConfig.get();

                if (config.getDailyStartTime() == null || config.getDailyEndTime() == null) {
                    return ResponseEntity.badRequest()
                            .body("Daily start or end time is not configured");
                }

                // Prepare daily time info
                Map<String, Object> response = new HashMap<>();
                response.put("dailyStartTime", config.getDailyStartTime().toString());
                response.put("dailyEndTime", config.getDailyEndTime().toString());
                response.put("lastUpdated", config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);

                // Fetch slots for today + next 2 days
                ZoneId zoneId = ZoneId.of("Asia/Kolkata");
                LocalDate today = LocalDate.now(zoneId);
                LocalDate endDate = today.plusDays(2);

                List<BiddingTimeSlot> slots = slotRepository.findBySlotDateBetween(today, endDate);
                Map<String, List<BiddingTimeSlotDTO>> slotsByDate = new HashMap<>();

                slots.stream()
                        .map(BiddingTimeSlotDTO::fromEntity)
                        .forEach(dto -> {
                            String dateKey = dto.getSlotDate().toString();
                            slotsByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dto);
                        });

                // Add to response
                response.put("biddingSlots", slotsByDate);

                return ResponseEntity.ok(response);

            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + ex.getMessage());
            }
        }

    // 🔧 Helper method for time parsing
    private LocalTime parseFlexibleTime(String timeStr) throws DateTimeParseException {
        timeStr = timeStr.trim();
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeStr, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        throw new DateTimeParseException("Invalid time format", timeStr, 0);
    }
    
    @Transactional
    public ResponseEntity<?> deleteBiddingSlot(Integer slotId) {
        Optional<BiddingTimeSlot> optionalSlot = slotRepository.findById(slotId);
        if (!optionalSlot.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Slot not found with id: " + slotId);
        }

        slotRepository.deleteById(slotId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BIDDING_SLOT_DELETED");
        payload.put("slotId", slotId);

        notificationService.notifyTopic("ADMIN", payload);
        
        return ResponseEntity.ok("Slot with id " + slotId + " deleted successfully.");
    }

}
