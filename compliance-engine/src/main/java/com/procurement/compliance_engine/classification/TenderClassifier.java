package com.procurement.compliance_engine.classification;
import java.util.List;
public interface TenderClassifier {
 record Prediction(String sectorCode,double relevanceScore){}
 record Result(List<Prediction> predictions,String modelVersion,String source){}
 Result classify(String title,String description);
}
