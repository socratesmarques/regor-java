package com.regor.backend.repository;
import com.regor.backend.entity.Match;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface MatchRepository extends JpaRepository<Match, UUID> {
Optional<Match> findByUserLowAndUserHigh(UUID userLow, UUID userHigh);
@Query("select m from Match m where m.userLow = :id or m.userHigh = :id order by m.createdAt desc")
List<Match> forUser(@Param("id") UUID id, Pageable pageable);
}
