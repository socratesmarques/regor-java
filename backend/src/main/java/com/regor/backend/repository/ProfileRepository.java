package com.regor.backend.repository;
import com.regor.backend.entity.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
boolean existsByUsername(String username);
}
