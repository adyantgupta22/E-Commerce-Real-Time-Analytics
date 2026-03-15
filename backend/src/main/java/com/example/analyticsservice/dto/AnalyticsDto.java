package com.example.analyticsservice.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsDto {
    private long activeUsers;
    private List<PageView> topPages;
    private Map<String, Long> activeSessions;
}
