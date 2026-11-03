package com.procurement.compliance_engine.security;
import org.springframework.security.core.context.SecurityContextHolder;
public final class CurrentUser {
    private CurrentUser(){}
    public static UserPrincipal get(){ Object p=SecurityContextHolder.getContext().getAuthentication().getPrincipal(); return p instanceof UserPrincipal u?u:null; }
}
