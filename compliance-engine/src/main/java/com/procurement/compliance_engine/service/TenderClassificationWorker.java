package com.procurement.compliance_engine.service;
import com.procurement.compliance_engine.classification.TenderClassifier;
import com.procurement.compliance_engine.classification.RuleBasedTenderClassifier;
import com.procurement.compliance_engine.repository.TenderRepository;
import com.procurement.compliance_engine.repository.TenderRequirementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class TenderClassificationWorker {
 private final TenderClassificationService jobs;private final TenderRepository tenders;private final TenderRequirementRepository requirements;private final TenderClassifier classifier;
 public TenderClassificationWorker(TenderClassificationService jobs,TenderRepository tenders,TenderRequirementRepository requirements,TenderClassifier classifier){this.jobs=jobs;this.tenders=tenders;this.requirements=requirements;this.classifier=classifier;}
 @Scheduled(fixedDelayString="${app.classification.poll-ms:3000}") public void run(){var next=jobs.claim();if(next.isEmpty())return;var claim=next.get();try{var t=tenders.findById(claim.tenderId()).orElseThrow();String scope=requirements.findByTenderId(t.getId()).stream().map(r->r.getRequirementCode()+" "+r.getDescription()+" "+r.getExpectedValue()).reduce("",(a,b)->a+"\n"+b);TenderClassifier.Result result;try{result=classifier.classify(t.getTitle(),(t.getDescription()==null?"":t.getDescription())+"\n"+scope);}catch(Exception inferenceFailure){result=new RuleBasedTenderClassifier().classify(t.getTitle(),(t.getDescription()==null?"":t.getDescription())+"\n"+scope);result=new TenderClassifier.Result(result.predictions(),"keyword-taxonomy-fallback-v1","RULES");}jobs.complete(claim,result);}catch(Exception e){jobs.fail(claim,e);}}
}
