package fr.mtpilates.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByRole(AppRole role);

    boolean existsByUserIdAndRole(UUID userId, AppRole role);

    long deleteByUserIdAndRole(UUID userId, AppRole role);
}
