package com.regor.backend.repository;
import com.regor.backend.entity.Photo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
List<Photo> findByUserIdOrderByCreatedAtAsc(UUID userId);
@Query("select p.id from Photo p where p.userId=:user order by p.createdAt")
List<UUID> idsForUser(@Param("user") UUID user);
long countByUserId(UUID userId);
}
