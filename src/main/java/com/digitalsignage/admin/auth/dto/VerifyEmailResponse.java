package com.digitalsignage.admin.auth.dto;

import com.digitalsignage.admin.common.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyEmailResponse {

    private final String username;
    private final UserRole role;
    private final Long organizationId;
    private final String organizationCode;
}
