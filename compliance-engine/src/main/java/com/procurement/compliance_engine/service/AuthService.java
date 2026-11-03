package com.procurement.compliance_engine.service;
import com.procurement.compliance_engine.dto.AuthDtos.*;
import com.procurement.compliance_engine.entity.*;
import com.procurement.compliance_engine.repository.*;
import com.procurement.compliance_engine.security.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseCookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AuthService {
 private final AppUserRepository users; private final OrganizationRepository organizations; private final BidderProfileRepository profiles; private final RefreshSessionRepository sessions; private final AccountRegistrationRequestRepository registrationRequests; private final PasswordEncoder encoder; private final JwtService jwt; private final JdbcTemplate jdbc;
 @Value("${app.auth.refresh-secure:false}") private boolean secureCookie;
 @Value("${app.auth.refresh-days:7}") private int refreshDays;
 public AuthService(AppUserRepository u,OrganizationRepository o,BidderProfileRepository p,RefreshSessionRepository s,AccountRegistrationRequestRepository requests,PasswordEncoder e,JwtService j,JdbcTemplate jdbc){users=u;organizations=o;profiles=p;sessions=s;registrationRequests=requests;encoder=e;jwt=j;this.jdbc=jdbc;}
 @Transactional public RegistrationResult register(RegisterRequest r){
  String email=r.email().trim().toLowerCase(Locale.ROOT);
  String actorType=r.actorType()==null?"BIDDER":r.actorType().trim().toUpperCase(Locale.ROOT);
  if(users.existsByEmailIgnoreCase(email)||registrationRequests.existsByEmailIgnoreCaseAndStatus(email,"PENDING")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already has an account or pending request");
  if(!"ADMIN".equals(actorType) && (r.organizationName()==null||r.organizationName().isBlank()||r.registrationNumber()==null||r.registrationNumber().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Organization name and registration number are required");
  if(!"BIDDER".equals(actorType)) {
   AccountRegistrationRequest request=new AccountRegistrationRequest();request.setFullName(r.fullName().trim());request.setEmail(email);request.setPasswordHash(encoder.encode(r.password()));request.setActorType(actorType);
   if("ORGANIZATION".equals(actorType)){request.setOrganizationName(r.organizationName().trim());request.setRegistrationNumber(r.registrationNumber().trim());}
   request=registrationRequests.saveAndFlush(request);audit(null,"ACCOUNT_REQUESTED",request.getId(),"PENDING");
   return new RegistrationResult("PENDING","Your access request was submitted for system administrator approval.",null,request.getId());
  }
  Organization org=new Organization(); org.setName(r.organizationName().trim()); org.setOrganizationType("BIDDER"); org.setRegistrationNumber(r.registrationNumber().trim()); org=organizations.saveAndFlush(org);
  BidderProfile profile=new BidderProfile(); profile.setOrganization(org); profiles.save(profile);
  AppUser u=new AppUser(); u.setFullName(r.fullName().trim());u.setEmail(email);u.setPasswordHash(encoder.encode(r.password()));u.setRole("BIDDER");u.setOrganization(org);
  try { u=users.saveAndFlush(u); } catch(DataIntegrityViolationException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Email or organization is already registered");}
  audit(u.getId(),"ACCOUNT_CREATED",u.getId(),"SUCCESS"); return new RegistrationResult("ACTIVE","Bidder account created.",toResponse(u),null);
 }
 @Transactional public TokenResponse login(LoginRequest r,HttpServletResponse response){
  AppUser u=users.findByEmailIgnoreCase(r.email()).orElse(null);
  if(u==null || !encoder.matches(r.password(),u.getPasswordHash()) || !"ACTIVE".equals(u.getAccountStatus())) {audit(u==null?null:u.getId(),"LOGIN",null,"FAILURE");throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");}
  u.setLastLoginAt(OffsetDateTime.now()); String refresh=randomToken(); RefreshSession session=new RefreshSession();session.setUser(u);session.setTokenHash(hash(refresh));session.setExpiresAt(OffsetDateTime.now().plusDays(refreshDays));sessions.save(session);setRefresh(response,refresh);audit(u.getId(),"LOGIN",u.getId(),"SUCCESS");return new TokenResponse(jwt.issue(UserPrincipal.from(u)),toResponse(u));
 }
 @Transactional(noRollbackFor=ResponseStatusException.class) public TokenResponse refresh(String token,HttpServletResponse response){
  if(token==null||token.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Refresh session is missing");
  String old=hash(token); RefreshSession s=sessions.findByTokenHash(old).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid refresh session"));
  if(s.getRevokedAt()!=null){sessions.findByUserIdAndRevokedAtIsNull(s.getUser().getId()).forEach(x->x.setRevokedAt(OffsetDateTime.now())); throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Refresh token replay detected; sessions revoked");}
  if(s.getExpiresAt().isBefore(OffsetDateTime.now()) || !"ACTIVE".equals(s.getUser().getAccountStatus())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Refresh session expired");
  String next=randomToken(), nextHash=hash(next); s.setRevokedAt(OffsetDateTime.now());s.setReplacedByHash(nextHash); RefreshSession n=new RefreshSession();n.setUser(s.getUser());n.setTokenHash(nextHash);n.setExpiresAt(OffsetDateTime.now().plusDays(refreshDays));sessions.save(n);setRefresh(response,next);return new TokenResponse(jwt.issue(UserPrincipal.from(s.getUser())),toResponse(s.getUser()));
 }
 @Transactional public void logout(String token,HttpServletResponse response){if(token!=null){sessions.findByTokenHash(hash(token)).ifPresent(s->{s.setRevokedAt(OffsetDateTime.now());audit(s.getUser().getId(),"LOGOUT",s.getUser().getId(),"SUCCESS");});}clearRefresh(response);}
 @Transactional public void logoutAll(UUID id,HttpServletResponse response){sessions.findByUserIdAndRevokedAtIsNull(id).forEach(s->s.setRevokedAt(OffsetDateTime.now()));clearRefresh(response);audit(id,"LOGOUT_ALL",id,"SUCCESS");}
 @Transactional public void changePassword(UserPrincipal principal,ChangePasswordRequest r){AppUser u=users.findById(principal.id()).orElseThrow();if(!encoder.matches(r.currentPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");u.setPasswordHash(encoder.encode(r.newPassword()));sessions.findByUserIdAndRevokedAtIsNull(u.getId()).forEach(s->s.setRevokedAt(OffsetDateTime.now()));audit(u.getId(),"PASSWORD_CHANGED",u.getId(),"SUCCESS");}
 @Transactional public UserResponse provision(ProvisionRequest r){if(users.existsByEmailIgnoreCase(r.email()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email is already registered");Organization org=organizations.findById(r.organizationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organization not found"));if("BIDDER".equals(org.getOrganizationType()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Staff accounts must belong to a procurement authority");AppUser u=new AppUser();u.setFullName(r.fullName().trim());u.setEmail(r.email().trim().toLowerCase(Locale.ROOT));u.setPasswordHash(encoder.encode(r.password()));u.setRole(r.role());u.setOrganization(org);u=users.save(u);audit(u.getId(),"ACCOUNT_CREATED",u.getId(),"SUCCESS");return toResponse(u);}
 @Transactional public UserResponse updateRole(UUID id,RoleUpdateRequest r){AppUser u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));Organization org=null;if(!"SYSTEM_ADMIN".equals(r.role())&&r.organizationId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"An organization is required for this role");if(!"SYSTEM_ADMIN".equals(r.role())||r.organizationId()!=null){org=organizations.findById(r.organizationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organization not found"));String requiredType="BIDDER".equals(r.role())?"BIDDER":"PROCUREMENT_AUTHORITY";if(!requiredType.equals(org.getOrganizationType()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role must be linked to a matching organization type");if("BIDDER".equals(r.role())&&!profiles.existsByOrganizationId(org.getId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bidder organization must have a bidder profile");}if("SYSTEM_ADMIN".equals(u.getRole())&&"ACTIVE".equals(u.getAccountStatus())&&!"SYSTEM_ADMIN".equals(r.role())&&users.countByRoleAndAccountStatus("SYSTEM_ADMIN","ACTIVE")<=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot remove the last active system administrator");u.setRole(r.role());u.setOrganization(org);u.setUpdatedAt(OffsetDateTime.now());sessions.findByUserIdAndRevokedAtIsNull(u.getId()).forEach(s->s.setRevokedAt(OffsetDateTime.now()));audit(u.getId(),"ROLE_CHANGED",u.getId(),"SUCCESS");return toResponse(u);}
 @Transactional public void bootstrap(String email,String password,UUID orgId){if(email==null||email.isBlank()||!strongPassword(password)||users.existsByEmailIgnoreCase(email))return;Organization org=orgId==null?null:organizations.findById(orgId).orElse(null);if(orgId!=null&&(org==null||!"PROCUREMENT_AUTHORITY".equals(org.getOrganizationType())))return;AppUser u=new AppUser();u.setFullName("System Administrator");u.setEmail(email.toLowerCase(Locale.ROOT));u.setPasswordHash(encoder.encode(password));u.setRole("SYSTEM_ADMIN");u.setOrganization(org);users.save(u);audit(u.getId(),"ACCOUNT_CREATED",u.getId(),"SUCCESS");}
 public UserResponse me(UserPrincipal p){return toResponse(users.findById(p.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account no longer exists")));}
 @Transactional(readOnly=true) public List<UserResponse> listUsers(){return users.findAll().stream().map(this::toResponse).toList();}
 @Transactional(readOnly=true) public List<RegistrationRequestResponse> listRegistrationRequests(){return registrationRequests.findByStatusOrderBySubmittedAtAsc("PENDING").stream().map(r->new RegistrationRequestResponse(r.getId(),r.getFullName(),r.getEmail(),r.getActorType(),r.getOrganizationName(),r.getRegistrationNumber(),r.getSubmittedAt())).toList();}
 @Transactional public RegistrationResult approveRegistrationRequest(UUID requestId,UUID reviewerId){AccountRegistrationRequest request=registrationRequests.findById(requestId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration request not found"));if(!"PENDING".equals(request.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Request has already been reviewed");if(users.existsByEmailIgnoreCase(request.getEmail()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already has an account");Organization org=null;String role;if("ORGANIZATION".equals(request.getActorType())){org=new Organization();org.setName(request.getOrganizationName());org.setRegistrationNumber(request.getRegistrationNumber());org.setOrganizationType("PROCUREMENT_AUTHORITY");try{org=organizations.saveAndFlush(org);}catch(DataIntegrityViolationException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Organization name or registration number is already registered");}role="PROCUREMENT_OFFICER";}else role="SYSTEM_ADMIN";AppUser u=new AppUser();u.setFullName(request.getFullName());u.setEmail(request.getEmail());u.setPasswordHash(request.getPasswordHash());u.setRole(role);u.setOrganization(org);u=users.saveAndFlush(u);request.setStatus("APPROVED");request.setReviewedBy(reviewerId);request.setReviewedAt(OffsetDateTime.now());request.setProvisionedUserId(u.getId());audit(reviewerId,"ACCOUNT_REQUEST_APPROVED",request.getId(),"SUCCESS");audit(u.getId(),"ACCOUNT_CREATED",u.getId(),"SUCCESS");return new RegistrationResult("APPROVED","Request approved. You can now sign in.",toResponse(u),request.getId());}
 @Transactional public void rejectRegistrationRequest(UUID requestId,UUID reviewerId){AccountRegistrationRequest request=registrationRequests.findById(requestId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration request not found"));if(!"PENDING".equals(request.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Request has already been reviewed");request.setStatus("REJECTED");request.setReviewedBy(reviewerId);request.setReviewedAt(OffsetDateTime.now());audit(reviewerId,"ACCOUNT_REQUEST_REJECTED",request.getId(),"SUCCESS");}
 private UserResponse toResponse(AppUser u){return new UserResponse(u.getId(),u.getFullName(),u.getEmail(),u.getRole(),u.getOrganization()==null?null:u.getOrganization().getId(),u.getOrganization()==null?null:u.getOrganization().getName());}
 private void audit(UUID actor,String event,UUID resource,String outcome){jdbc.update("INSERT INTO audit_logs(actor_id,event_type,resource_id,outcome) VALUES (?,?,?,?)",actor,event,resource,outcome);}
 private static String randomToken(){byte[] b=new byte[48];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private static String hash(String x){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static boolean strongPassword(String p){return p!=null&&p.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,100}$");}
 private void setRefresh(HttpServletResponse r,String value){r.addHeader("Set-Cookie",ResponseCookie.from("refresh_token",value).httpOnly(true).secure(secureCookie).path("/api/v1/auth").sameSite("Strict").maxAge((long)refreshDays*86400).build().toString());}
 private void clearRefresh(HttpServletResponse r){r.addHeader("Set-Cookie",ResponseCookie.from("refresh_token","").httpOnly(true).secure(secureCookie).path("/api/v1/auth").sameSite("Strict").maxAge(0).build().toString());}
}
