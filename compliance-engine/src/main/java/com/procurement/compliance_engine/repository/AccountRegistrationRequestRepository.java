package com.procurement.compliance_engine.repository;
import com.procurement.compliance_engine.entity.AccountRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AccountRegistrationRequestRepository extends JpaRepository<AccountRegistrationRequest,UUID> {
    boolean existsByEmailIgnoreCaseAndStatus(String email,String status);
    List<AccountRegistrationRequest> findByStatusOrderBySubmittedAtAsc(String status);
}
