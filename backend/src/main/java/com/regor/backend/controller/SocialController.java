package com.regor.backend.controller;
import com.regor.backend.dto.Api.*;
import com.regor.backend.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @Validated
public class SocialController {
    private final MatchingService matching;private final ChatService chats;private final NotificationService notifications;
    public SocialController(MatchingService matching,ChatService chats,NotificationService notifications) {
        this.matching=matching;this.chats=chats;this.notifications=notifications;
    }
    private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @PostMapping("/swipes") public SwipeResult swipe(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody SwipeRequest request) {
        return matching.swipe(id(jwt),request);
    }
    @GetMapping({"/matches","/chats"}) public List<MatchView> matches(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) { return matching.list(id(jwt),page); }
    @GetMapping("/chats/{chatId}/messages") public List<MessageView> messages(@AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID chatId,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) { return chats.list(id(jwt),chatId,page); }
    @PostMapping("/chats/{chatId}/messages") public MessageView send(@AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID chatId,@Valid @RequestBody SendMessage request) { return chats.send(id(jwt),chatId,request); }
    @GetMapping("/notifications") public List<NotificationView> notifications(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) { return notifications.list(id(jwt),page); }
    @PutMapping("/notifications/{notificationId}/read") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID notificationId) { notifications.markRead(id(jwt),notificationId); }
}
