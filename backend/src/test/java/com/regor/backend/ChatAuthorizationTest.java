package com.regor.backend;
import com.regor.backend.entity.*;
import com.regor.backend.repository.*;
import com.regor.backend.service.*;
import com.regor.backend.dto.Api.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ChatAuthorizationTest {
    @Test void participantEntryAloneDoesNotAuthorizeSomeoneOutsideMatch() {
        var chats=mock(ChatRepository.class);var matches=mock(MatchRepository.class);
        var participants=mock(ChatParticipantRepository.class);var messages=mock(MessageRepository.class);
        var match=new Match();match.userLow=UUID.randomUUID();match.userHigh=UUID.randomUUID();
        var chat=new Chat();chat.matchId=match.id;UUID outsider=UUID.randomUUID();
        when(chats.findById(chat.id)).thenReturn(Optional.of(chat));when(matches.findById(match.id)).thenReturn(Optional.of(match));
        when(participants.existsByChatIdAndUserId(chat.id,outsider)).thenReturn(true);
        var service=new ChatService(chats,matches,participants,messages,mock(UserRepository.class),mock(NotificationService.class),mock(ApplicationEventPublisher.class));
        assertThrows(ResponseStatusException.class,()->service.send(outsider,chat.id,new SendMessage(UUID.randomUUID(),"invader")));
        verifyNoInteractions(messages);
    }
}
