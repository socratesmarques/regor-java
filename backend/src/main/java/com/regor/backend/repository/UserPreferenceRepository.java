package com.regor.backend.repository;
import com.regor.backend.entity.UserPreference;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

}
