package com.procurement.compliance_engine.classification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
@Component @ConditionalOnProperty(name="app.classification.provider",havingValue="rules",matchIfMissing=true)
public class RuleBasedTenderClassifier implements TenderClassifier {
 private static final Map<String,List<String>> TERMS=Map.ofEntries(
  Map.entry("IT_SOFTWARE",List.of("software","application","portal","platform","erp","digital","database","hospital management system","information system")),
  Map.entry("HEALTHCARE_IT",List.of("health information","hospital management","ehr","electronic health","pacs","radiology information","telemedicine","healthcare software")),
  Map.entry("CLOUD_INFRASTRUCTURE",List.of("cloud","hosting","data center","kubernetes","compute infrastructure","cloud migration")),
  Map.entry("MEDICAL_EQUIPMENT",List.of("medical equipment","diagnostic equipment","mri scanner","ct scanner","ventilator","ultrasound machine","surgical instrument")),
  Map.entry("PHARMACEUTICALS",List.of("pharmaceutical","medicine","drug supply","vaccine","api bulk drug")),
  Map.entry("CIVIL_CONSTRUCTION",List.of("construction","building works","road works","bridge","civil works","renovation")),
  Map.entry("ELECTRICAL",List.of("electrical","transformer","switchgear","power distribution","solar panel")),
  Map.entry("AGRICULTURE",List.of("agriculture","irrigation","seed","fertilizer","farm equipment","crop")),
  Map.entry("EDUCATION_SERVICES",List.of("education","training","school","curriculum","learning management")),
  Map.entry("TRANSPORTATION",List.of("transport","vehicle","fleet","logistics","bus","ambulance")),
  Map.entry("CONSULTING",List.of("consultancy","consulting","advisory","feasibility study","project management services")));
 public Result classify(String title,String description){String text=((title==null?"":title)+" "+(description==null?"":description)).toLowerCase(Locale.ROOT);List<Prediction> out=new ArrayList<>();for(var e:TERMS.entrySet()){long hits=e.getValue().stream().filter(text::contains).count();if(hits>0)out.add(new Prediction(e.getKey(),Math.min(.95,.55+.12*(hits-1))));}if(out.isEmpty())out.add(new Prediction("CONSULTING",.25));out.sort(Comparator.comparingDouble(Prediction::relevanceScore).reversed().thenComparing(Prediction::sectorCode));return new Result(List.copyOf(out),"keyword-taxonomy-v1","RULES");}
}

