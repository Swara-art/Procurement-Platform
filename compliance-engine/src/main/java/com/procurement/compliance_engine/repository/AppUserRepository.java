package com.procurement.compliance_engine.repository;
import com.procurement.compliance_engine.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByIdAndAccountStatus(UUID id,String accountStatus);
    @EntityGraph(attributePaths="organization") Optional<AppUser> findOneByIdAndAccountStatus(UUID id,String accountStatus);
    long countByRoleAndAccountStatus(String role,String accountStatus);
}
