package com.regor.backend.repository;
import com.regor.backend.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface UserRepository extends JpaRepository<User, UUID> {
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
@Query("select u from User u where u.id = :id")
Optional<User> lockById(@Param("id") UUID id);
}
