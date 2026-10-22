package com.regor.backend.service;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.*;
import com.regor.backend.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private final ChatRepository chats;private final MatchRepository matches;
    private final ChatParticipantRepository participants;private final MessageRepository messages;
    private final UserRepository users;private final NotificationService notifications;private final ApplicationEventPublisher events;
    public ChatService(ChatRepository chats,MatchRepository matches,ChatParticipantRepository participants,
        MessageRepository messages,UserRepository users,NotificationService notifications,ApplicationEventPublisher events) {
        this.chats=chats;this.matches=matches;this.participants=participants;this.messages=messages;
        this.users=users;this.notifications=notifications;this.events=events;
    }
    public void authorize(UUID user,UUID chatId) {
        var chat=chats.findById(chatId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Conversa indisponível"));
        var match=matches.findById(chat.matchId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(!(match.userLow.equals(user)||match.userHigh.equals(user))||!participants.existsByChatIdAndUserId(chatId,user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente participantes de um match podem conversar");
    }
    public List<MessageView> list(UUID user,UUID chatId,int page) {
        authorize(user,chatId);
        var result=new ArrayList<>(messages.findByChatIdOrderByCreatedAtDescIdDesc(chatId,PageRequest.of(page,50)));
        Collections.reverse(result);return result.stream().map(this::view).toList();
    }
    @Transactional
    public MessageView send(UUID user,UUID chatId,SendMessage request) {
        authorize(user,chatId);
        users.lockById(user).orElseThrow();
        var old=messages.findBySenderIdAndClientId(user,request.clientId());
        if(old.isPresent()) {
            if(!old.get().chatId.equals(chatId)||!old.get().content.equals(request.content().strip()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Identificador de mensagem já utilizado");
            return view(old.get());
        }
        var m=new Message();m.chatId=chatId;m.senderId=user;m.clientId=request.clientId();m.content=request.content().strip();
        messages.saveAndFlush(m);
        for(var p:participants.findByChatId(chatId)) {
            events.publishEvent(new RealtimeEvent(p.userId,"message",view(m)));
            if(!p.userId.equals(user)) notifications.create(p.userId,user,"MESSAGE","Você recebeu uma nova mensagem.",chatId);
        }
        return view(m);
    }
    private MessageView view(Message m) { return new MessageView(m.id,m.chatId,m.senderId,m.clientId,m.content,m.createdAt); }
}
