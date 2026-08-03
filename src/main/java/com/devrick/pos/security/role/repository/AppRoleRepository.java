package com.devrick.pos.security.role.repository;

import com.devrick.pos.security.role.entity.AppRole;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppRole> findByName(String name);
}
