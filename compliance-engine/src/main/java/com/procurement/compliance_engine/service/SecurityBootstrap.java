package com.procurement.compliance_engine.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component public class SecurityBootstrap implements ApplicationRunner {
 private final AuthService auth;
 @Value("${app.auth.bootstrap-admin-email:}") String email;
 @Value("${app.auth.bootstrap-admin-password:}") String password;
 @Value("${app.auth.bootstrap-admin-organization-id:}") String organizationId;
 public SecurityBootstrap(AuthService auth){this.auth=auth;}
 @Override public void run(ApplicationArguments args){if(!email.isBlank()&&!password.isBlank())auth.bootstrap(email,password,organizationId.isBlank()?null:UUID.fromString(organizationId));}
}
