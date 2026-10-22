package com.regor.backend.service;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.*;
import com.regor.backend.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchingService {
    private final UserRepository users; private final SwipeRepository swipes; private final MatchRepository matches;
    private final ChatRepository chats; private final ChatParticipantRepository participants;
    private final ProfileService profiles; private final NotificationService notifications;
    public MatchingService(UserRepository users,SwipeRepository swipes,MatchRepository matches,ChatRepository chats,
        ChatParticipantRepository participants,ProfileService profiles,NotificationService notifications) {
        this.users=users;this.swipes=swipes;this.matches=matches;this.chats=chats;
        this.participants=participants;this.profiles=profiles;this.notifications=notifications;
    }
    @Transactional
    public SwipeResult swipe(UUID actor,SwipeRequest request) {
        UUID target=request.targetId();
        if(actor.equals(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Não é possível curtir o próprio perfil");
        UUID low=actor.toString().compareTo(target.toString())<0?actor:target, high=low.equals(actor)?target:actor;
        // Both directions lock the same user rows in the same order before checking reciprocity.
        users.lockById(low).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado"));
        users.lockById(high).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado"));
        var previous=swipes.findByActorIdAndTargetId(actor,target);
        var swipe=previous.orElseGet(Swipe::new);
        boolean changed=previous.isEmpty()||swipe.kind!=request.kind();
        swipe.actorId=actor;swipe.targetId=target;swipe.kind=request.kind();swipes.saveAndFlush(swipe);
        var match=matches.findByUserLowAndUserHigh(low,high);
        if(match.isEmpty()&&request.kind().isPositive()&&
            swipes.findByActorIdAndTargetId(target,actor).map(s->s.kind.isPositive()).orElse(false)) {
            var m=new Match();m.userLow=low;m.userHigh=high;matches.save(m);
            var c=new Chat();c.matchId=m.id;chats.save(c);
            for(UUID user:List.of(low,high)) {
                var p=new ChatParticipant();p.chatId=c.id;p.userId=user;participants.save(p);
                notifications.create(user,user.equals(actor)?target:actor,"MATCH","Vocês deram match! Comece uma conversa.",c.id);
            }
            match=Optional.of(m);
        } else if(changed&&request.kind().isPositive()&&match.isEmpty()) {
            notifications.create(target,actor,request.kind().name(),
                request.kind()==SwipeKind.SUPERLIKE?"Você recebeu um Super Like!":"Alguém curtiu seu perfil.",null);
        }
        UUID chat=match.flatMap(m->chats.findByMatchId(m.id)).map(c->c.id).orElse(null);
        return new SwipeResult(swipe.id,match.map(m->m.id).orElse(null),chat);
    }
    public List<MatchView> list(UUID user,int page) {
        return matches.forUser(user,PageRequest.of(page,30)).stream().map(m->new MatchView(m.id,
            chats.findByMatchId(m.id).orElseThrow().id,profiles.view(m.userLow.equals(user)?m.userHigh:m.userLow,user),m.createdAt)).toList();
    }
}
