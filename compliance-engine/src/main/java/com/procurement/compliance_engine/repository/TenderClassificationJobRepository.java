package com.procurement.compliance_engine.repository;
import com.procurement.compliance_engine.entity.TenderClassificationJob;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface TenderClassificationJobRepository extends JpaRepository<TenderClassificationJob,UUID>{
 Optional<TenderClassificationJob> findTopByTenderIdOrderByCreatedAtDesc(UUID tenderId);
 Optional<TenderClassificationJob> findByTenderIdAndContentHash(UUID tenderId,String contentHash);
}
