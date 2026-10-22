package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="swipes")
public class Swipe {
    @Id public UUID id = UUID.randomUUID();
    public UUID actorId;
public UUID targetId;
@Enumerated(EnumType.STRING) @Column(length=20) public SwipeKind kind;
public Instant createdAt = Instant.now();
}
