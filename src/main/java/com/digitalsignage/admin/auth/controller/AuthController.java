package com.digitalsignage.admin.auth.controller;

import com.digitalsignage.admin.auth.dto.LoginRequest;
import com.digitalsignage.admin.auth.dto.LoginResponse;
import com.digitalsignage.admin.auth.dto.RefreshTokenRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;
import com.digitalsignage.admin.auth.service.AuthService;
import com.digitalsignage.admin.auth.service.RegistrationService;
import com.digitalsignage.admin.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.admin-prefix}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterOrganizationResponse> register(
            @Valid @RequestBody RegisterOrganizationRequest request) {
        return ApiResponse.ok(registrationService.registerOrganization(request));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        registrationService.verifyEmail(request);
        return ApiResponse.ok();
    }
}
