package com.procurement.compliance_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@ConditionalOnProperty(name="app.storage.provider",havingValue="s3")
public class S3DocumentStorageService implements DocumentStorageService {
    private final S3Client s3; private final String bucket; private final String kmsKey;
    public S3DocumentStorageService(S3Client s3,@Value("${app.storage.s3.bucket}") String bucket,@Value("${app.storage.s3.kms-key-id:}") String kmsKey){this.s3=s3;this.bucket=bucket;this.kmsKey=kmsKey;}
    public String provider(){return "S3";}
    public String bucketIdentifier(){return bucket;}
 public void put(String key,byte[] bytes,String contentType,String checksum){var req=PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).checksumSHA256(java.util.Base64.getEncoder().encodeToString(java.util.HexFormat.of().parseHex(checksum)));if(!kmsKey.isBlank())req.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(kmsKey);s3.putObject(req.build(),RequestBody.fromBytes(bytes));}
    public byte[] get(String key){try{ResponseBytes<GetObjectResponse> b=s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());return b.asByteArray();}catch(NoSuchKeyException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document content not found");}}
    public void delete(String key){s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());}
}
