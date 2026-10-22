package com.regor.backend.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="user_preferences")
public class UserPreference {
    @Id public UUID id = UUID.randomUUID();
    public int minAge = 18;
public int maxAge = 60;
public int maxDistanceKm = 100;
@Column(length=80) public String interest = "";
@Column(length=30) public String gender = "";
@Column(length=120) public String location = "";
public boolean matchesNotif = true;
public boolean messagesNotif = true;
public boolean showDistance = true;
public boolean showOnline = true;
public boolean discoverable = true;
@Column(length=10) public String theme = "escuro";
@Column(length=10) public String language = "pt-BR";
}
