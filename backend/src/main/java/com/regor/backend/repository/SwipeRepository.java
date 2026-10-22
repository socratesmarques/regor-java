package com.regor.backend.repository;
import com.regor.backend.entity.Swipe;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface SwipeRepository extends JpaRepository<Swipe, UUID> {
Optional<Swipe> findByActorIdAndTargetId(UUID actorId, UUID targetId);
}
