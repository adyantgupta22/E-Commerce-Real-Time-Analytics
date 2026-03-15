package com.example.analyticsservice.service;

import com.example.analyticsservice.dto.AnalyticsDto;
import com.example.analyticsservice.model.Event;
import com.example.analyticsservice.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        analyticsService = new AnalyticsServiceImpl(redisTemplate);
    }

    @Test
    void testProcessEvent() {
        Event event = new Event();
        event.setUserId("user1");
        event.setPageUrl("/home");
        event.setSessionId("session1");
        event.setTimestamp(Instant.now());

        analyticsService.processEvent(event);

        verify(redisTemplate.opsForSet()).add(anyString(), anyString());
        verify(redisTemplate.opsForZSet()).incrementScore(anyString(), anyString(), anyLong());
        verify(redisTemplate.opsForHash()).increment(anyString(), anyString(), anyLong());
    }

    @Test
    void testGetAnalytics() {
        when(redisTemplate.keys("active_users:*")).thenReturn(Set.of("active_users:1"));
        when(setOperations.members("active_users:1")).thenReturn(Set.of("user1", "user2"));
        when(redisTemplate.keys("page_views:*")).thenReturn(Set.of("page_views:1"));
        when(redisTemplate.keys("active_sessions:*")).thenReturn(Set.of("active_sessions:user1"));
        when(hashOperations.size("active_sessions:user1")).thenReturn(2L);

        AnalyticsDto analytics = analyticsService.getAnalytics();

        assertEquals(2, analytics.getActiveUsers());
        assertEquals(2, analytics.getActiveSessions().get("user1"));
    }
}
