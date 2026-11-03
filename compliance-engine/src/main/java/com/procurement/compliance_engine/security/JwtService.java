package com.procurement.compliance_engine.security;
import tools.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final byte[] secret; private final ObjectMapper mapper;
    public JwtService(@Value("${jwt.secret}") String secret, ObjectMapper mapper) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        this.secret=secret.getBytes(StandardCharsets.UTF_8); this.mapper=mapper;
    }
    public String issue(UserPrincipal p) {
        try {
            long now=Instant.now().getEpochSecond();
            String header=enc(mapper.writeValueAsBytes(Map.of("alg","HS256","typ","JWT")));
            Map<String,Object> claims=new java.util.HashMap<>(); claims.put("sub",p.id().toString()); claims.put("email",p.email()); claims.put("role",p.role()); claims.put("iat",now); claims.put("exp",now+900);
            if(p.organizationId()!=null) claims.put("org",p.organizationId().toString());
            String body=enc(mapper.writeValueAsBytes(claims)); String unsigned=header+"."+body;
            return unsigned+"."+enc(mac(unsigned));
        } catch(Exception e){throw new IllegalStateException("JWT creation failed",e);}
    }
    public UserPrincipal parse(String token) {
        try {
            String[] parts=token.split("\\."); if(parts.length!=3) return null;
            byte[] expected=mac(parts[0]+"."+parts[1]); byte[] provided=Base64.getUrlDecoder().decode(parts[2]);
            if(!java.security.MessageDigest.isEqual(expected,provided)) return null;
            var c=mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if(c.path("exp").asLong(0)<=Instant.now().getEpochSecond()) return null;
            UUID id=UUID.fromString(c.path("sub").asText()); String org=c.path("org").asText(null);
            return new UserPrincipal(id,c.path("email").asText(),"",c.path("role").asText(),org==null?null:UUID.fromString(org),"ACTIVE");
        } catch(Exception e){return null;}
    }
    private byte[] mac(String input) throws Exception { Mac m=Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(secret,"HmacSHA256")); return m.doFinal(input.getBytes(StandardCharsets.UTF_8)); }
    private static String enc(byte[] b){return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
}
