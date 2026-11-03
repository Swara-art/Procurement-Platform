package com.procurement.compliance_engine.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class AuthDtos {
 private AuthDtos(){}
 public record RegisterRequest(@NotBlank @Size(max=200) String fullName,@Email @NotBlank @Size(max=320) String email,@NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,100}$",message="Password must be at least 12 characters and include uppercase, lowercase, number, and symbol") String password,@Pattern(regexp="BIDDER|ORGANIZATION|ADMIN") String actorType,@Size(max=255) String organizationName,@Size(max=255) String registrationNumber){}
 public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
 public record ProvisionRequest(@NotBlank String fullName,@Email @NotBlank String email,@NotNull UUID organizationId,@NotBlank @Pattern(regexp="PROCUREMENT_OFFICER|EVALUATION_OFFICER|SYSTEM_ADMIN") String role,@NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,100}$",message="Password must be at least 12 characters and include uppercase, lowercase, number, and symbol") String password){}
 public record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,100}$",message="Password must be at least 12 characters and include uppercase, lowercase, number, and symbol") String newPassword){}
 public record RoleUpdateRequest(@NotBlank @Pattern(regexp="SYSTEM_ADMIN|PROCUREMENT_OFFICER|BIDDER|EVALUATION_OFFICER") String role,UUID organizationId){}
 public record UserResponse(UUID id,String fullName,String email,String role,UUID organizationId,String organizationName){}
 public record TokenResponse(String accessToken,UserResponse user){}
 public record RegistrationResult(String status,String message,UserResponse user,UUID requestId){}
 public record RegistrationRequestResponse(UUID id,String fullName,String email,String actorType,String organizationName,String registrationNumber,java.time.OffsetDateTime submittedAt){}
}
