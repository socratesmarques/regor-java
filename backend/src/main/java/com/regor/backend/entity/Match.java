package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="matches")
public class Match {
    @Id public UUID id = UUID.randomUUID();
    public UUID userLow;
public UUID userHigh;
public Instant createdAt = Instant.now();
}
