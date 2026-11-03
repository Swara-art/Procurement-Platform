package com.procurement.compliance_engine.service;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;import java.nio.file.Path;import static org.junit.jupiter.api.Assertions.*;
class LocalDocumentStorageServiceTest {
 @TempDir Path root;
 @Test void storesReadsAndDeletesObjectsUnderPrivateRoot(){var storage=new LocalDocumentStorageService(root.toString());String key="organizations/tenant-uuid/registration/document-uuid/version-uuid.pdf";byte[] bytes="%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);storage.put(key,bytes,"application/pdf","unused");assertArrayEquals(bytes,storage.get(key));assertTrue(root.resolve(key).normalize().startsWith(root));storage.delete(key);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->storage.get(key));}
}
