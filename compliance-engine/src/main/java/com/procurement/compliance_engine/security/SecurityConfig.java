package com.procurement.compliance_engine.security;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;
import java.util.Arrays;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService userDetailsService(){return username->{throw new UsernameNotFoundException("Password authentication is unavailable");};}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtService jwt,com.procurement.compliance_engine.repository.AppUserRepository users,@Value("${app.cors.allowed-origins:http://localhost:3000}") String origins) throws Exception {
  CookieCsrfTokenRepository csrf=CookieCsrfTokenRepository.withHttpOnlyFalse(); csrf.setCookiePath("/");
  http.csrf(c->c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/auth/login","/api/v1/auth/register"))
   .cors(c->c.configurationSource(corsConfigurationSource(origins)))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .formLogin(f->f.disable()).httpBasic(h->h.disable())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Unauthorized")))
   .authorizeHttpRequests(a->a.requestMatchers("/error","/actuator/health","/api/v1/auth/csrf","/api/v1/auth/register","/api/v1/auth/login").permitAll()
      .requestMatchers("/api/v1/auth/refresh","/api/v1/auth/logout").permitAll().anyRequest().authenticated())
   .addFilterBefore(new JwtAuthenticationFilter(jwt,users),UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:3000}") String origins){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isBlank()).toList());c.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","X-XSRF-TOKEN","X-Refresh-CSRF"));c.setAllowCredentials(true);UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
}
