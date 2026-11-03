package com.procurement.compliance_engine.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import com.procurement.compliance_engine.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@Profile("ai")
public class EvidenceRetrievalService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public EvidenceRetrievalService(VectorStore vectorStore, JdbcTemplate jdbc) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    public List<Document> searchEvidence(UUID bidId, String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }
        var actor=CurrentUser.get();
        if(actor==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        if(actor!=null && !"SYSTEM_ADMIN".equals(actor.role())) {
            Integer allowed= actor.organizationId()==null ? 0 : jdbc.queryForObject(
                    "SELECT COUNT(*) FROM bids b JOIN tenders t ON t.id=b.tender_id JOIN bidder_profiles bp ON bp.id=b.bidder_id JOIN organizations bo ON bo.id=bp.organization_id WHERE b.id=? AND ((?='BIDDER' AND bo.id=?) OR (? IN ('PROCUREMENT_OFFICER','EVALUATION_OFFICER') AND t.authority_id=? AND t.submission_deadline<=NOW()))",
                    Integer.class,bidId,actor.role(),actor.organizationId(),actor.role(),actor.organizationId());
            if(allowed==null||allowed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found");
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.0)
                .filterExpression("bidId == '" + bidId + "'")
                .build();
        return vectorStore.similaritySearch(request);
    }
}
