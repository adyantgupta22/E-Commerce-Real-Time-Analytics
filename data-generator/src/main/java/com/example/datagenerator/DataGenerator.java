package com.example.datagenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataGenerator {

    private static final String DEFAULT_API_URL = "http://localhost:8080/api/events";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private static final Random RANDOM = new Random();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final String[] PAGES = {
            "/home",
            "/products/electronics",
            "/products/books",
            "/products/clothing",
            "/cart",
            "/checkout",
            "/profile",
            "/search",
            "/deals",
            "/about"
    };

    private static final String[] EVENT_TYPES = {
            "page_view",
            "click",
            "scroll",
            "form_submit"
    };

    public static void main(String[] args) {
        String apiUrl = System.getenv("INGESTION_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = DEFAULT_API_URL;
        }
        
        final String finalApiUrl = apiUrl;
        System.out.println("Starting Data Generator. Sending events to: " + finalApiUrl);
        
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        // Generate ~10 events per second (100ms interval)
        scheduler.scheduleAtFixedRate(() -> sendEvent(finalApiUrl), 0, 100, TimeUnit.MILLISECONDS);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Data Generator...");
            scheduler.shutdown();
        }));
    }

    private static void sendEvent(String apiUrl) {
        try {
            Event event = createRandomEvent();
            String jsonEvent = OBJECT_MAPPER.writeValueAsString(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonEvent))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 202) {
                System.out.println("Sent event: " + jsonEvent);
            } else if (response.statusCode() == 429) {
                System.out.println("Rate limited - waiting...");
            } else {
                System.err.println("Unexpected response: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending event: " + e.getMessage());
        }
    }

    private static Event createRandomEvent() {
        // Generate dynamic user IDs - usr_XXX format with numeric suffix as per spec
        String userId = "usr_" + (100 + RANDOM.nextInt(900));
        // Generate dynamic session IDs - sess_XXX format with numeric suffix as per spec  
        String sessionId = "sess_" + (100 + RANDOM.nextInt(900));
        
        return new Event(
                Instant.now(),
                userId,
                EVENT_TYPES[RANDOM.nextInt(EVENT_TYPES.length)],
                PAGES[RANDOM.nextInt(PAGES.length)],
                sessionId
        );
    }

    public static class Event {
        public Instant timestamp;
        public String userId;
        public String eventType;
        public String pageUrl;
        public String sessionId;

        public Event(Instant timestamp, String userId, String eventType, String pageUrl, String sessionId) {
            this.timestamp = timestamp;
            this.userId = userId;
            this.eventType = eventType;
            this.pageUrl = pageUrl;
            this.sessionId = sessionId;
        }
    }
}
