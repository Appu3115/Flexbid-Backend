package com.example.flexbid.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flexbid.dto.BiddingSlotRequest;
import com.example.flexbid.dto.BiddingTimeRequest;
import com.example.flexbid.service.BiddingTimeService;


@RestController
@RequestMapping("/api/admin/bidding-time")
@CrossOrigin(origins = "*")
public class BiddingTimeController {

	@Autowired
    private BiddingTimeService biddingTimeService;

    @PostMapping("/daily/set")
    public ResponseEntity<?> setDailyTime(@RequestBody BiddingTimeRequest request) {
        return biddingTimeService.setDailyBiddingTime(request);
    }

    @PostMapping("/slot/add")
    public ResponseEntity<?> addTimeSlot(@RequestBody BiddingSlotRequest request) {
        return biddingTimeService.addBiddingSlot(request);
    }
    
    @GetMapping("/get-times")
    public ResponseEntity<?> getBiddingTimes() {
        return biddingTimeService.getBiddingTimes();
    }
    
    @DeleteMapping("/bidding-slots/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable("id") Integer slotId) {
        return biddingTimeService.deleteBiddingSlot(slotId);
    }

}

