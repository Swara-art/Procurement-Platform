package com.procurement.compliance_engine.security;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("unit-test-secret-that-is-at-least-32-bytes-long", JsonMapper.builder().build());

    @Test void signsAndParsesShortLivedToken() {
        UUID userId=UUID.randomUUID(), organizationId=UUID.randomUUID();
        UserPrincipal user=new UserPrincipal(userId,"person@example.in","","BIDDER",organizationId,"ACTIVE");
        UserPrincipal parsed=jwt.parse(jwt.issue(user));
        assertNotNull(parsed);
        assertEquals(userId,parsed.id());
        assertEquals(organizationId,parsed.organizationId());
        assertEquals("BIDDER",parsed.role());
    }

    @Test void rejectsModifiedSignature() {
        UserPrincipal user=new UserPrincipal(UUID.randomUUID(),"person@example.in","","BIDDER",null,"ACTIVE");
        String[] parts=jwt.issue(user).split("\\.");
        parts[2]=(parts[2].startsWith("a")?"b":"a")+parts[2].substring(1);
        String tampered=String.join(".",parts);
        assertNull(jwt.parse(tampered));
    }

    @Test void rejectsShortSigningSecret() {
        assertThrows(IllegalStateException.class,()->new JwtService("too-short",JsonMapper.builder().build()));
    }
}
