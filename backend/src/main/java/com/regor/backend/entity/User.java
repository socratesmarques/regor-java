package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="users")
public class User {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable=false, unique=true, length=254) public String email;
@Column(nullable=false, length=100) public String passwordHash;
@Column(nullable=false, length=20) public String role = "USER";
public boolean vip;
public boolean verified;
public long coins;
public long tokenVersion;
public Instant createdAt = Instant.now();
}
