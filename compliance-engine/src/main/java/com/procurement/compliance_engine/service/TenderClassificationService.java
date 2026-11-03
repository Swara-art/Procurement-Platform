package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.classification.*;
import com.procurement.compliance_engine.entity.*;
import com.procurement.compliance_engine.repository.*;
import com.procurement.compliance_engine.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class TenderClassificationService {
 private final TenderClassificationJobRepository jobs; private final TenderClassificationRepository predictions; private final JdbcTemplate jdbc;
 public TenderClassificationService(TenderClassificationJobRepository j,TenderClassificationRepository p,JdbcTemplate jdbc){this.jobs=j;this.predictions=p;this.jdbc=jdbc;}
 public record JobClaim(UUID id,UUID tenderId,int attempts){}
 @Transactional public UUID enqueue(Tender tender,boolean force){String content=(tender.getTitle()+"\n"+(tender.getDescription()==null?"":tender.getDescription())).trim();String digest=hash(content);String key=force?digest+"-"+UUID.randomUUID():digest;var existing=jobs.findByTenderIdAndContentHash(tender.getId(),key);if(existing.isPresent())return existing.get().getId();TenderClassificationJob j=new TenderClassificationJob();j.setTender(tender);j.setContentHash(key);j.setStatus("PENDING");j.setAttempts(0);j.setNextAttemptAt(OffsetDateTime.now());return jobs.saveAndFlush(j).getId();}
 @Transactional public Optional<JobClaim> claim(){List<Map<String,Object>> rows=jdbc.queryForList("UPDATE tender_classification_jobs SET status='PROCESSING', attempts=attempts+1, updated_at=NOW() WHERE id=(SELECT id FROM tender_classification_jobs WHERE status='PENDING' AND next_attempt_at<=NOW() ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING id,tender_id,attempts");if(rows.isEmpty())return Optional.empty();var r=rows.getFirst();return Optional.of(new JobClaim((UUID)r.get("id"),(UUID)r.get("tender_id"),((Number)r.get("attempts")).intValue()));}
 @Transactional public void complete(JobClaim c,TenderClassifier.Result result){TenderClassificationJob j=jobs.findById(c.id()).orElseThrow();Set<String> seen=new HashSet<>();for(var p:result.predictions()){if(!SectorTaxonomy.contains(p.sectorCode())||p.relevanceScore()<0||p.relevanceScore()>1||!seen.add(p.sectorCode()))throw new IllegalArgumentException("Invalid classifier result");TenderClassification x=new TenderClassification();x.setJob(j);x.setTender(j.getTender());x.setSectorCode(p.sectorCode());x.setRelevanceScore(java.math.BigDecimal.valueOf(p.relevanceScore()));x.setConfirmed(false);x.setSource(result.source());predictions.save(x);}j.setModelVersion(result.modelVersion());j.setStatus("REVIEW_REQUIRED");j.setLastError(null);j.setUpdatedAt(OffsetDateTime.now());audit(j.getTender().getId(),"CLASSIFICATION_PREDICTED","SUCCESS");}
 @Transactional public void fail(JobClaim c,Exception failure){jobs.findById(c.id()).ifPresent(j->{j.setLastError(safeError(failure));j.setUpdatedAt(OffsetDateTime.now());if(c.attempts()>=5)j.setStatus("FAILED");else{j.setStatus("PENDING");j.setNextAttemptAt(OffsetDateTime.now().plusSeconds(Math.min(3600,30L*(1L<<(c.attempts()-1)))));}audit(j.getTender().getId(),"CLASSIFICATION_FAILED",j.getStatus());});}
 @Transactional(readOnly=true) public Map<String,Object> get(UUID id){TenderClassificationJob j=jobs.findTopByTenderIdOrderByCreatedAtDesc(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Classification has not been requested"));return Map.of("jobId",j.getId(),"tenderId",id,"status",j.getStatus(),"attempts",j.getAttempts(),"modelVersion",j.getModelVersion()==null?"":j.getModelVersion(),"lastError",j.getLastError()==null?"":j.getLastError(),"sectors",predictions.findByJobIdOrderByRelevanceScoreDesc(j.getId()).stream().map(p->Map.of("code",p.getSectorCode(),"label",SectorTaxonomy.label(p.getSectorCode()),"relevanceScore",p.getRelevanceScore(),"confirmed",p.getConfirmed(),"source",p.getSource())).toList());}
 @Transactional public Map<String,Object> confirm(UUID id,List<String> codes){if(codes==null||codes.isEmpty()||new HashSet<>(codes).size()!=codes.size()||codes.stream().anyMatch(s->!SectorTaxonomy.contains(s)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select one or more valid sectors");var j=jobs.findTopByTenderIdOrderByCreatedAtDesc(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Classification job not found"));if(!List.of("REVIEW_REQUIRED","COMPLETED").contains(j.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Predictions are not ready for review");for(var old:predictions.findConfirmedByTender(id))old.setConfirmed(false);predictions.deleteAll(predictions.findByJobIdOrderByRelevanceScoreDesc(j.getId()));var actor=CurrentUser.get();for(String code:codes){TenderClassification p=new TenderClassification();p.setJob(j);p.setTender(j.getTender());p.setSectorCode(code);p.setRelevanceScore(java.math.BigDecimal.ONE);p.setConfirmed(true);p.setSource("HUMAN");p.setConfirmedBy(actor.id());p.setConfirmedAt(OffsetDateTime.now());predictions.save(p);}j.setStatus("COMPLETED");j.setUpdatedAt(OffsetDateTime.now());audit(id,"CLASSIFICATION_CONFIRMED","SUCCESS");return get(id);}
 private void audit(UUID id,String event,String outcome){var actor=CurrentUser.get();jdbc.update("INSERT INTO audit_logs(actor_id,event_type,resource_id,outcome) VALUES (?,?,?,?)",actor==null?null:actor.id(),event,id,outcome);}
 private static String safeError(Exception e){String s=e.getClass().getSimpleName()+": "+(e.getMessage()==null?"failed":e.getMessage());return s.length()>990?s.substring(0,990):s;}
 private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
