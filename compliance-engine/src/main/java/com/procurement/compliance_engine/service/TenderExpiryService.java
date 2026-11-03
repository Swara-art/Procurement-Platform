package com.procurement.compliance_engine.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component public class TenderExpiryService {
 private final JdbcTemplate jdbc;
 public TenderExpiryService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Transactional @Scheduled(cron="0 0 * * * *") public void closeExpiredTenders(){
  jdbc.update("DELETE FROM tenders t WHERE t.status='PUBLISHED' AND t.submission_deadline <= NOW() AND NOT EXISTS (SELECT 1 FROM bids b WHERE b.tender_id=t.id)");
  jdbc.update("UPDATE tenders t SET status='CLOSED' WHERE t.status='PUBLISHED' AND t.submission_deadline <= NOW() AND EXISTS (SELECT 1 FROM bids b WHERE b.tender_id=t.id)");
 }
}
