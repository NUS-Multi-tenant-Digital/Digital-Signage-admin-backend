package com.digitalsignage.admin.auth.service;

import com.digitalsignage.admin.auth.dto.RegisterOrganizationRequest;
import com.digitalsignage.admin.auth.dto.RegisterOrganizationResponse;
import com.digitalsignage.admin.auth.dto.VerifyEmailRequest;

public interface RegistrationService {

    RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request);

    void verifyEmail(VerifyEmailRequest request);
}
