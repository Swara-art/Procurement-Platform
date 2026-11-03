package com.procurement.compliance_engine.service;

public interface DocumentStorageService {
    String provider();
    String bucketIdentifier();
    void put(String objectKey, byte[] bytes, String contentType, String checksum);
    byte[] get(String objectKey);
    void delete(String objectKey);
}
