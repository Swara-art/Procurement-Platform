package com.procurement.compliance_engine.security;
import com.procurement.compliance_engine.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
public record UserPrincipal(UUID id, String email, String password, String role, UUID organizationId, String status) implements UserDetails {
    public static UserPrincipal from(AppUser u) { return new UserPrincipal(u.getId(),u.getEmail(),u.getPasswordHash(),u.getRole(),u.getOrganization()==null?null:u.getOrganization().getId(),u.getAccountStatus()); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){ return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
    @Override public String getUsername(){return email;}
    @Override public String getPassword(){return password;}
    @Override public boolean isEnabled(){return "ACTIVE".equals(status);}
}
