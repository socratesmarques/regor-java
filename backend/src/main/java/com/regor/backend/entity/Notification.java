package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="notifications")
public class Notification {
    @Id public UUID id = UUID.randomUUID();
    public UUID userId;
public UUID actorId;
@Column(length=20) public String type;
@Column(length=250) public String text;
public UUID chatId;
public Instant readAt;
public Instant createdAt = Instant.now();
}
