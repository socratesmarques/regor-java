package com.regor.backend.dto;
import com.regor.backend.entity.SwipeKind;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class Api {
    private Api() {}
    public record Register(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=10,max=72) String password,
        @NotBlank @Size(max=80) String displayName,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,40}") String username,
        @NotNull @Past LocalDate birthDate) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=72) String password) {}
    public record Session(String token, Instant expiresAt, Account user) {}
    public record Account(UUID id, String email, String role, boolean vip, boolean verified, long coins) {}
    public record ProfileEdit(@NotBlank @Size(max=80) String displayName,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,40}") String username,
        @NotNull @Past LocalDate birthDate, @NotNull @Size(max=120) String location,
        @NotNull @Size(max=2000) String bio, @NotNull @Size(max=500) String interests,
        @NotNull @Size(max=30) String gender,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude) {}
    public record ProfileView(UUID id, String displayName, String username, int age,
        LocalDate birthDate, String location, String bio, String interests, String gender,
        Double latitude, Double longitude, boolean verified, boolean vip, boolean online, Double distanceKm, List<UUID> photos) {}
    public record Preferences(@Min(18) @Max(120) int minAge, @Min(18) @Max(120) int maxAge,
        @Min(1) @Max(20000) int maxDistanceKm, @NotNull @Size(max=80) String interest,
        @NotNull @Size(max=30) String gender, @NotNull @Size(max=120) String location,
        boolean matchesNotif, boolean messagesNotif, boolean showDistance, boolean showOnline,
        boolean discoverable, @Pattern(regexp="escuro|claro") @NotNull String theme,
        @Pattern(regexp="pt-BR") @NotNull String language) {}
    public record SwipeRequest(@NotNull UUID targetId, @NotNull SwipeKind kind) {}
    public record SwipeResult(UUID swipeId, UUID matchId, UUID chatId) {}
    public record MatchView(UUID id, UUID chatId, ProfileView profile, Instant createdAt) {}
    public record SendMessage(@NotNull UUID clientId, @NotBlank @Size(max=4000) String content) {}
    public record MessageView(UUID id, UUID chatId, UUID senderId, UUID clientId, String content, Instant createdAt) {}
    public record NotificationView(UUID id, String type, String text, UUID actorId, UUID chatId, Instant readAt, Instant createdAt) {}
    public record PasswordChange(@NotBlank @Size(max=72) String currentPassword,
        @NotBlank @Size(min=10,max=72) String newPassword) {}
}
