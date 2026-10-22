package com.regor.backend.service;
import java.util.UUID;
public record RealtimeEvent(UUID userId, String type, Object data) {}
