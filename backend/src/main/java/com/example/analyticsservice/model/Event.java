package com.example.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;

@Data
public class Event {
    @NotNull
    private Instant timestamp;
    
    @NotNull
    @JsonProperty("user_id")
    private String userId;
    
    @NotNull
    @JsonProperty("event_type")
    private String eventType;
    
    @NotNull
    @JsonProperty("page_url")
    private String pageUrl;
    
    @NotNull
    @JsonProperty("session_id")
    private String sessionId;
}
