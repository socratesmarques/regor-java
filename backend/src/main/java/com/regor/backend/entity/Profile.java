package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="profiles")
public class Profile {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable=false, length=80) public String displayName;
@Column(nullable=false, unique=true, length=40) public String username;
public LocalDate birthDate;
@Column(length=120) public String location = "";
@Column(length=2000) public String bio = "";
@Column(length=500) public String interests = "";
@Column(length=30) public String gender = "";
public Double latitude;
public Double longitude;
}
