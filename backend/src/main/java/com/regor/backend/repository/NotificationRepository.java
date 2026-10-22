package com.regor.backend.repository;
import com.regor.backend.entity.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
long countByUserIdAndReadAtIsNull(UUID userId);
}
