package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="photos")
public class Photo {
    @Id public UUID id = UUID.randomUUID();
    public UUID userId;
@Column(columnDefinition="bytea") public byte[] content;
@Column(length=30) public String mediaType;
public Instant createdAt = Instant.now();
}
