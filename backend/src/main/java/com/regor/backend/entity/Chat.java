package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="chats")
public class Chat {
    @Id public UUID id = UUID.randomUUID();
    public UUID matchId;
public Instant createdAt = Instant.now();
}
