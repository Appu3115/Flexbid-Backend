package com.example.flexbid.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class RazorpayClientProvider {

	@Autowired
    private RazorpayClient razorpayClient;
	
	   @Value("${razorpay.key_id}")
	    private String razorpayKeyId;

	    @Value("${razorpay.key_secret}")
	    private String razorpayKeySecret;

    public RazorpayClientProvider() throws RazorpayException {
        this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    public RazorpayClient getClient() {
        return razorpayClient;
    }
}

