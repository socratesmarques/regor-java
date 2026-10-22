package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="messages")
public class Message {
    @Id public UUID id = UUID.randomUUID();
    public UUID chatId;
public UUID senderId;
public UUID clientId;
@Column(length=4000) public String content;
public Instant createdAt = Instant.now();
}
