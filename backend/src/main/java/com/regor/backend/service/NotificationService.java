package com.regor.backend.service;
import com.regor.backend.entity.Notification;
import com.regor.backend.dto.Api.NotificationView;
import com.regor.backend.repository.*;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserPreferenceRepository preferences;
    private final ApplicationEventPublisher events;
    public NotificationService(NotificationRepository notifications,UserPreferenceRepository preferences,ApplicationEventPublisher events) {
        this.notifications=notifications;this.preferences=preferences;this.events=events;
    }
    public void create(UUID user,UUID actor,String type,String text,UUID chat) {
        var pref=preferences.findById(user).orElseThrow();
        if(type.equals("MESSAGE")&&!pref.messagesNotif || type.equals("MATCH")&&!pref.matchesNotif) return;
        var n=new Notification();n.userId=user;n.actorId=actor;n.type=type;n.text=text;n.chatId=chat;notifications.save(n);
        events.publishEvent(new RealtimeEvent(user,"notification",view(n)));
    }
    public List<NotificationView> list(UUID id,int page) {
        return notifications.findByUserIdOrderByCreatedAtDesc(id,PageRequest.of(page,50)).stream().map(this::view).toList();
    }
    @Transactional public void markRead(UUID user,UUID id) {
        var n=notifications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!n.userId.equals(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        n.readAt=Instant.now();
    }
    private NotificationView view(Notification n) { return new NotificationView(n.id,n.type,n.text,n.actorId,n.chatId,n.readAt,n.createdAt); }
}
