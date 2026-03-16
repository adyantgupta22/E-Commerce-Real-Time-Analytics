package com.example.analyticsservice.service.impl;

import com.example.analyticsservice.dto.AnalyticsDto;
import com.example.analyticsservice.dto.PageView;
import com.example.analyticsservice.model.Event;
import com.example.analyticsservice.service.AnalyticsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String ACTIVE_USERS_PREFIX = "active_users:";
    private static final String PAGE_VIEWS_PREFIX = "page_views:";
    private static final String ACTIVE_SESSIONS_PREFIX = "active_sessions:";

    private final StringRedisTemplate redisTemplate;

    public AnalyticsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void processEvent(Event event) {
        updateActiveUsers(event);
        updatePageViews(event);
        updateActiveSessions(event);
    }

    @Override
    public AnalyticsDto getAnalytics() {
        AnalyticsDto analyticsDto = new AnalyticsDto();
        analyticsDto.setActiveUsers(getActiveUsers());
        analyticsDto.setTopPages(getTopPages());
        analyticsDto.setActiveSessions(getActiveSessions());
        return analyticsDto;
    }

    private void updateActiveUsers(Event event) {
        // Use minute-based buckets for 5-minute rolling window
        String key = ACTIVE_USERS_PREFIX + Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
        redisTemplate.opsForSet().add(key, event.getUserId());
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }

    private void updatePageViews(Event event) {
        // Use minute-based buckets for 15-minute rolling window
        String key = PAGE_VIEWS_PREFIX + Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
        redisTemplate.opsForZSet().incrementScore(key, event.getPageUrl(), 1);
        redisTemplate.expire(key, 15, TimeUnit.MINUTES);
    }

    private void updateActiveSessions(Event event) {
        // Track sessions per user with 5-minute expiry
        String key = ACTIVE_SESSIONS_PREFIX + event.getUserId();
        redisTemplate.opsForSet().add(key, event.getSessionId());
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }

    private long getActiveUsers() {
        Set<String> keys = redisTemplate.keys(ACTIVE_USERS_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        Set<String> uniqueUsers = new HashSet<>();
        for (String key : keys) {
            Set<String> members = redisTemplate.opsForSet().members(key);
            if (members != null) {
                uniqueUsers.addAll(members);
            }
        }
        return uniqueUsers.size();
    }

    private List<PageView> getTopPages() {
        Set<String> keys = redisTemplate.keys(PAGE_VIEWS_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        // Aggregate page views across all time buckets
        Map<String, Double> aggregatedViews = new HashMap<>();
        for (String key : keys) {
            Set<ZSetOperations.TypedTuple<String>> entries = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
            if (entries != null) {
                for (ZSetOperations.TypedTuple<String> entry : entries) {
                    String url = entry.getValue();
                    Double score = entry.getScore();
                    if (url != null && score != null) {
                        aggregatedViews.merge(url, score, Double::sum);
                    }
                }
            }
        }

        // Sort by views and return top 5
        return aggregatedViews.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new PageView(entry.getKey(), entry.getValue().longValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Long> getActiveSessions() {
        Set<String> keys = redisTemplate.keys(ACTIVE_SESSIONS_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Long> activeSessions = new HashMap<>();
        for (String key : keys) {
            String userId = key.replace(ACTIVE_SESSIONS_PREFIX, "");
            Long sessionCount = redisTemplate.opsForSet().size(key);
            if (sessionCount != null && sessionCount > 0) {
                activeSessions.put(userId, sessionCount);
            }
        }
        return activeSessions;
    }
}
