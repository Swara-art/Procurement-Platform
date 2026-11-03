package com.procurement.compliance_engine.repository;
import com.procurement.compliance_engine.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String hash);
    List<RefreshSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
