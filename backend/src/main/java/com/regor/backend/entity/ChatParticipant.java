package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="chat_participants")
public class ChatParticipant {
    @Id public UUID id = UUID.randomUUID();
    public UUID chatId;
public UUID userId;
}
