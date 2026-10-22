package com.regor.backend.repository;
import com.regor.backend.entity.Chat;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface ChatRepository extends JpaRepository<Chat, UUID> {
Optional<Chat> findByMatchId(UUID matchId);
}
