package com.devrick.pos.user.repository;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);
}
