package com.example.analyticsservice.controller;

import com.example.analyticsservice.model.Event;
import com.example.analyticsservice.service.AnalyticsService;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final AnalyticsService analyticsService;
    private final Bucket rateLimitBucket;

    public EventController(AnalyticsService analyticsService, Bucket rateLimitBucket) {
        this.analyticsService = analyticsService;
        this.rateLimitBucket = rateLimitBucket;
    }

    @PostMapping
    public ResponseEntity<String> receiveEvent(@Valid @RequestBody Event event) {
        if (rateLimitBucket.tryConsume(1)) {
            analyticsService.processEvent(event);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }
    }
}
