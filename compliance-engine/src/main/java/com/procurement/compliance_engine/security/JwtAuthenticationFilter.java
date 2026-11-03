package com.procurement.compliance_engine.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final com.procurement.compliance_engine.repository.AppUserRepository users;
    public JwtAuthenticationFilter(JwtService jwt,com.procurement.compliance_engine.repository.AppUserRepository users){this.jwt=jwt;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String h=req.getHeader("Authorization");
        if(h!=null && h.startsWith("Bearer ")) {
            UserPrincipal tokenPrincipal=jwt.parse(h.substring(7));
            if(tokenPrincipal!=null) users.findOneByIdAndAccountStatus(tokenPrincipal.id(),"ACTIVE").map(UserPrincipal::from).ifPresent(p->SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities())));
        }
        chain.doFilter(req,res);
    }
}
