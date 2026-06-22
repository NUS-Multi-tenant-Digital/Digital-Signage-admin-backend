package com.digitalsignage.admin.auth.service;

import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;
import com.digitalsignage.admin.auth.dto.VerifyEmailResponse;

public interface RegistrationService {

    RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request);

    VerifyEmailResponse verifyEmail(VerifyEmailRequest request);
}
