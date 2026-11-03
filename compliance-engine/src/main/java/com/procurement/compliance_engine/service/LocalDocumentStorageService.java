package com.procurement.compliance_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name="app.storage.provider", havingValue="local", matchIfMissing=true)
public class LocalDocumentStorageService implements DocumentStorageService {
    private final Path root;
    public LocalDocumentStorageService(@Value("${app.storage.upload-dir:./private-uploads}") String root) {
        this.root=Path.of(root).toAbsolutePath().normalize();
    }
    public String provider(){return "LOCAL";}
    public String bucketIdentifier(){return "local-private";}
    public void put(String key,byte[] bytes,String contentType,String checksum){Path path=resolve(key);try{Files.createDirectories(path.getParent());Files.write(path,bytes,java.nio.file.StandardOpenOption.CREATE_NEW);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not store document",e);}}
    public byte[] get(String key){Path path=resolve(key);try{return Files.readAllBytes(path);}catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document content not found",e);}}
    public void delete(String key){Path path=resolve(key);try{Files.deleteIfExists(path);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not remove document content",e);}}
    private Path resolve(String key){Path p=root.resolve(key).normalize();if(!p.startsWith(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid object key");return p;}
}
