package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.AnalyticsDto;
import com.example.analyticsservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AnalyticsDto getAnalytics() {
        return analyticsService.getAnalytics();
    }
}
