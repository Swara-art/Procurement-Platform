package com.procurement.compliance_engine.classification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Component @ConditionalOnProperty(name="app.classification.provider",havingValue="groq")
public class GroqTenderClassifier implements TenderClassifier {
 private final RestClient client;private final ObjectMapper mapper;private final String model;
 public GroqTenderClassifier(RestClient.Builder builder,ObjectMapper mapper,@Value("${app.classification.groq.api-key}") String key,@Value("${app.classification.groq.endpoint:https://api.groq.com/openai/v1}") String endpoint,@Value("${app.classification.groq.model:llama-3.3-70b-versatile}") String model){this.client=builder.baseUrl(endpoint).defaultHeader("Authorization","Bearer "+key).build();this.mapper=mapper;this.model=model;}
 public Result classify(String title,String description){String taxonomy=String.join(",",SectorTaxonomy.codes());Map<String,Object> req=Map.of("model",model,"temperature",0,"response_format",Map.of("type","json_object"),"messages",List.of(Map.of("role","system","content","Classify the purchased goods/services, never the buyer organization. Return only JSON: {\"sectors\":[{\"code\":string,\"score\":number}]}. Allowed codes: "+taxonomy+". Scores express relevance, not calibrated probabilities. Return 1-4 relevant sectors."),Map.of("role","user","content","Title: "+title+"\nDescription and scope: "+description)));
  try{JsonNode root=client.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON).body(req).retrieve().body(JsonNode.class);String json=root.path("choices").path(0).path("message").path("content").asText();JsonNode parsed=mapper.readTree(json);JsonNode arr=parsed.path("sectors");if(!arr.isArray())throw new IllegalArgumentException("Classifier response has no sectors array");List<Prediction> out=new ArrayList<>();for(JsonNode p:arr){String code=p.path("code").asText();double score=p.path("score").asDouble(-1);if(!SectorTaxonomy.contains(code)||!Double.isFinite(score)||score<0||score>1)throw new IllegalArgumentException("Classifier response violates sector schema");out.add(new Prediction(code,score));}if(out.isEmpty()||out.size()>4)throw new IllegalArgumentException("Classifier sector count is invalid");return new Result(List.copyOf(out),"groq:"+model,"MODEL");}catch(Exception e){throw new IllegalStateException("Tender classification provider failed validation or request",e);}}
}
