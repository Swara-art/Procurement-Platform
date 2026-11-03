package com.procurement.compliance_engine.classification;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class RuleBasedTenderClassifierTest {
 private final RuleBasedTenderClassifier classifier=new RuleBasedTenderClassifier();
 @Test void healthcareDepartmentSoftwareTenderIsClassifiedByPurchasedService(){var r=classifier.classify("Hospital Information System", "Supply, implementation and support of a hospital management software platform and electronic health records");var codes=r.predictions().stream().map(TenderClassifier.Prediction::sectorCode).toList();assertTrue(codes.contains("IT_SOFTWARE"));assertTrue(codes.contains("HEALTHCARE_IT"));assertFalse(codes.contains("MEDICAL_EQUIPMENT"));}
 @Test void imagingHardwareIsMedicalEquipmentNotSoftware(){var r=classifier.classify("Supply of MRI scanners", "Procure magnetic resonance imaging diagnostic equipment with installation and warranty");var codes=r.predictions().stream().map(TenderClassifier.Prediction::sectorCode).toList();assertTrue(codes.contains("MEDICAL_EQUIPMENT"));assertFalse(codes.contains("IT_SOFTWARE"));}
}
