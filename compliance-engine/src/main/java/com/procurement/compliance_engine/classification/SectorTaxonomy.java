package com.procurement.compliance_engine.classification;
import java.util.*;
public final class SectorTaxonomy {
 private SectorTaxonomy(){}
 private static final Map<String,String> VALUES=Map.ofEntries(Map.entry("IT_SOFTWARE","IT Software"),Map.entry("HEALTHCARE_IT","Healthcare IT"),Map.entry("CLOUD_INFRASTRUCTURE","Cloud Infrastructure"),Map.entry("MEDICAL_EQUIPMENT","Medical Equipment"),Map.entry("PHARMACEUTICALS","Pharmaceuticals"),Map.entry("CIVIL_CONSTRUCTION","Civil Construction"),Map.entry("ELECTRICAL","Electrical"),Map.entry("AGRICULTURE","Agriculture"),Map.entry("EDUCATION_SERVICES","Education Services"),Map.entry("TRANSPORTATION","Transportation"),Map.entry("CONSULTING","Consulting"));
 public static Set<String> codes(){return VALUES.keySet();} public static boolean contains(String code){return VALUES.containsKey(code);} public static String label(String code){return VALUES.get(code);}
}
