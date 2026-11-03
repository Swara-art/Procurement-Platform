package com.procurement.compliance_engine.service;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class S3DocumentStorageServiceTest {
    @Test
    void minioUploadDownloadDeleteRoundTrip() throws Exception {
        String endpoint = System.getenv().getOrDefault("MINIO_TEST_ENDPOINT", "http://localhost:9000");
        String bucket = System.getenv().getOrDefault("MINIO_TEST_BUCKET", "procurement-private");
        try (S3Client client = S3Client.builder().endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        System.getenv().getOrDefault("MINIO_ROOT_USER", "minio-local"),
                        System.getenv().getOrDefault("MINIO_ROOT_PASSWORD", "minio-local-secret-change-me"))))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build()) {
            String key = "integration-tests/" + UUID.randomUUID() + ".pdf";
            byte[] content = "%PDF-1.7\nMinIO storage round trip\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            var storage = new S3DocumentStorageService(client, bucket, "");
            try {
                storage.put(key, content, "application/pdf", checksum);
                assertArrayEquals(content, storage.get(key));
            } finally {
                storage.delete(key);
            }
        }
    }
}
