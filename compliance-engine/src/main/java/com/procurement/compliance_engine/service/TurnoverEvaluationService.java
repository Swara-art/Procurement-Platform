package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.TurnoverEvaluationResponse;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("ai")
public class TurnoverEvaluationService {

    private static final Pattern AVERAGE_PATTERN = Pattern.compile(
            "(?:three|3)[- ]year\\s+average(?:\\s+annual)?(?:\\s+turnover)?\\s*[:=]?\\s*"
                    + "(?:INR|Rs\\.?|₹)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(crore|lakh)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TENDER_REFERENCE_PATTERN = Pattern.compile(
            "(?:tender\\s*(?:reference|number|no\\.?)|reference\\s*(?:number|no\\.?))"
                    + "\\s*[:#]?\\s*([A-Z0-9]+(?:[-/][A-Z0-9]+)+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final BigDecimal CRORE_IN_RUPEES = new BigDecimal("10000000");
    private static final BigDecimal LAKH_IN_RUPEES = new BigDecimal("100000");

    private final JdbcTemplate jdbcTemplate;
    private final EvidenceRetrievalService evidenceService;

    public TurnoverEvaluationService(
            JdbcTemplate jdbcTemplate,
            EvidenceRetrievalService evidenceService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.evidenceService = evidenceService;
    }

    public TurnoverEvaluationResponse evaluate(UUID bidId) {
        List<TurnoverRule> rules = jdbcTemplate.query(
                """
                SELECT t.tender_number, t.title, tr.description, tr.expected_value
                FROM bids b
                JOIN tenders t ON t.id = b.tender_id
                JOIN tender_requirements tr ON tr.tender_id = b.tender_id
                WHERE b.id = ?
                  AND tr.requirement_code = 'MIN_TURNOVER'
                  AND tr.operator = 'GREATER_THAN_OR_EQUAL'
                  AND tr.is_mandatory = TRUE
                """,
                (rs, rowNum) -> new TurnoverRule(
                        rs.getString("tender_number"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("expected_value")
                ),
                bidId
        );

        if (rules.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bid or supported mandatory turnover requirement not found"
            );
        }

        TurnoverRule rule = rules.getFirst();
        BigDecimal requiredAmount = parseRequiredAmount(rule.expectedValue());

        List<Document> financialEvidence = evidenceService.searchEvidence(
                bidId,
                "three-year average annual turnover financial capacity INR"
        );
        List<Document> tenderEvidence = evidenceService.searchEvidence(
                bidId,
                "tender reference number tender number project title"
        );

        String evidenceTenderReference = findTenderReference(tenderEvidence);

        for (Document document : financialEvidence) {
            String content = document.getText();
            if (content == null) {
                continue;
            }

            Matcher matcher = AVERAGE_PATTERN.matcher(content);
            if (!matcher.find()) {
                continue;
            }

            BigDecimal value = new BigDecimal(matcher.group(1).replace(",", ""));
            BigDecimal multiplier = matcher.group(2).equalsIgnoreCase("crore")
                    ? CRORE_IN_RUPEES
                    : LAKH_IN_RUPEES;
            BigDecimal declaredAmount = value.multiply(multiplier);
            boolean numericConditionMet = declaredAmount.compareTo(requiredAmount) >= 0;

            String explanation = buildReviewExplanation(
                    rule,
                    evidenceTenderReference,
                    declaredAmount,
                    numericConditionMet
            );

            return new TurnoverEvaluationResponse(
                    bidId,
                    "MIN_TURNOVER",
                    requiredAmount,
                    declaredAmount,
                    numericConditionMet,
                    "REQUIRES_REVIEW",
                    explanation,
                    metadataUuid(document, "documentId"),
                    metadataInteger(document, "pageNumber")
            );
        }

        return new TurnoverEvaluationResponse(
                bidId,
                "MIN_TURNOVER",
                requiredAmount,
                null,
                null,
                "INSUFFICIENT_EVIDENCE",
                "No explicitly stated three-year average turnover was found in the retrieved evidence. "
                        + "Manual review is required for tender " + rule.tenderNumber() + " (" + rule.tenderTitle() + ").",
                null,
                null
        );
    }

    private static BigDecimal parseRequiredAmount(String expectedValue) {
        try {
            return new BigDecimal(expectedValue.replace(",", "").trim());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "MIN_TURNOVER expected_value must be a numeric INR amount",
                    ex
            );
        }
    }

    private static String findTenderReference(List<Document> documents) {
        for (Document document : documents) {
            if (document.getText() == null) {
                continue;
            }
            Matcher matcher = TENDER_REFERENCE_PATTERN.matcher(document.getText());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String buildReviewExplanation(
            TurnoverRule rule,
            String evidenceTenderReference,
            BigDecimal declaredAmount,
            boolean numericConditionMet
    ) {
        String comparison = numericConditionMet
                ? "satisfies"
                : "does not satisfy";
        String periodConcern = "The PDF reports a three-year average, while the tender requirement states minimum annual turnover; the applicable accounting period needs clarification.";
        String authenticityConcern = "The extracted figure is an unverified bidder declaration; the system has not authenticated original signed financial statements.";
        String tenderConcern;

        if (evidenceTenderReference == null) {
            tenderConcern = "The tender reference in the evidence could not be verified against the database tender.";
        } else if (!evidenceTenderReference.equalsIgnoreCase(rule.tenderNumber())) {
            tenderConcern = "The PDF references tender " + evidenceTenderReference
                    + ", but this bid belongs to tender " + rule.tenderNumber()
                    + " (" + rule.tenderTitle() + ").";
        } else {
            tenderConcern = "The PDF tender reference matches the database tender, but the evidence still requires authenticity and period verification.";
        }

        return "Declared three-year average: INR " + declaredAmount.toPlainString()
                + "; this " + comparison + " the required INR " + rule.expectedValue() + ". "
                + periodConcern + " " + authenticityConcern + " " + tenderConcern
                + " No final compliance decision has been made; procurement officer review is required.";
    }

    private static UUID metadataUuid(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Retrieved evidence is missing " + key + " metadata");
        }
        return UUID.fromString(value.toString());
    }

    private static Integer metadataInteger(Document document, String key) {
        Map<String, Object> metadata = document.getMetadata();
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Retrieved evidence is missing " + key + " metadata");
        }
        return Integer.valueOf(value.toString());
    }

    private record TurnoverRule(
            String tenderNumber,
            String tenderTitle,
            String description,
            String expectedValue
    ) {
    }
}
