package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.BusinessLegalTypeResponse;
import com.procurement.compliance_engine.dto.LegalDocumentRequirementResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class BusinessLegalTypeService {
    private final JdbcTemplate jdbc;
    public BusinessLegalTypeService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<BusinessLegalTypeResponse> list() {
        return jdbc.query("SELECT code,display_name,description FROM business_legal_types WHERE active ORDER BY sort_order",
                (rs,n) -> new BusinessLegalTypeResponse(rs.getString(1),rs.getString(2),rs.getString(3)));
    }
    public List<LegalDocumentRequirementResponse> requirements(String code) {
        Boolean exists = jdbc.query("SELECT EXISTS(SELECT 1 FROM business_legal_types WHERE code=? AND active)",rs -> rs.next() && rs.getBoolean(1),code);
        if (!Boolean.TRUE.equals(exists)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown business legal type");
        return jdbc.query("SELECT r.document_type_code,d.display_name,d.description,r.requirement_level,r.condition_expression,r.alternative_group,r.sort_order FROM legal_type_document_requirements r JOIN document_types d ON d.code=r.document_type_code WHERE r.legal_type_code=? AND r.active AND d.active ORDER BY r.sort_order",
                (rs,n) -> new LegalDocumentRequirementResponse(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),(Integer)rs.getObject(7)),code);
    }
}
