package com.regor.backend.repository;
import com.regor.backend.entity.Message;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface MessageRepository extends JpaRepository<Message, UUID> {
List<Message> findByChatIdOrderByCreatedAtDescIdDesc(UUID chatId, Pageable pageable);
Optional<Message> findBySenderIdAndClientId(UUID senderId, UUID clientId);
}
