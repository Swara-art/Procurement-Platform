package com.procurement.compliance_engine.controller;
import com.procurement.compliance_engine.dto.AuthDtos.*;
import com.procurement.compliance_engine.security.CurrentUser;
import com.procurement.compliance_engine.security.UserPrincipal;
import com.procurement.compliance_engine.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthService service;
 public AuthController(AuthService s){service=s;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public RegistrationResult register(@Valid @RequestBody RegisterRequest r){return service.register(r);}
 @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest r,HttpServletResponse response){return service.login(r,response);}
 @PostMapping("/refresh") public TokenResponse refresh(@CookieValue(name="refresh_token",required=false) String token,HttpServletResponse response){return service.refresh(token,response);}
 @PostMapping("/logout") public void logout(@CookieValue(name="refresh_token",required=false) String token,HttpServletResponse response){service.logout(token,response);}
 @PostMapping("/logout-all") public void logoutAll(HttpServletResponse response){service.logoutAll(CurrentUser.get().id(),response);}
 @GetMapping("/me") public UserResponse me(){return service.me(CurrentUser.get());}
 @PostMapping("/change-password") public void changePassword(@Valid @RequestBody ChangePasswordRequest r){service.changePassword(CurrentUser.get(),r);}
 @PostMapping("/provision") @PreAuthorize("hasRole('SYSTEM_ADMIN')") @ResponseStatus(HttpStatus.CREATED) public UserResponse provision(@Valid @RequestBody ProvisionRequest r){return service.provision(r);}
 @GetMapping("/users") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public java.util.List<UserResponse> users(){return service.listUsers();}
 @GetMapping("/registration-requests") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public java.util.List<RegistrationRequestResponse> registrationRequests(){return service.listRegistrationRequests();}
 @PostMapping("/registration-requests/{id}/approve") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public RegistrationResult approveRegistrationRequest(@PathVariable UUID id){return service.approveRegistrationRequest(id,CurrentUser.get().id());}
 @PostMapping("/registration-requests/{id}/reject") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public void rejectRegistrationRequest(@PathVariable UUID id){service.rejectRegistrationRequest(id,CurrentUser.get().id());}
 @PatchMapping("/users/{id}/role") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public UserResponse updateRole(@PathVariable UUID id,@Valid @RequestBody com.procurement.compliance_engine.dto.AuthDtos.RoleUpdateRequest request){return service.updateRole(id,request);}
}
