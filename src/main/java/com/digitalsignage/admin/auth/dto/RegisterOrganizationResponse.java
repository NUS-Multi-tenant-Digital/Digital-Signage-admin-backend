package com.digitalsignage.admin.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterOrganizationResponse {

    private final Long organizationId;
    private final String adminUsername;
    private final String message;
}
