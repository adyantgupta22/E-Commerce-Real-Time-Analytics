package com.example.analyticsservice.service;


import com.example.analyticsservice.dto.AnalyticsDto;
import com.example.analyticsservice.model.Event;

public interface AnalyticsService {
    void processEvent(Event event);
    AnalyticsDto getAnalytics();
}
