package com.regor.backend.repository;
import com.regor.backend.entity.ChatParticipant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {
boolean existsByChatIdAndUserId(UUID chatId, UUID userId);
List<ChatParticipant> findByChatId(UUID chatId);
}
