package com.digitalsignage.admin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterOrganizationRequest {

    @NotBlank(message = "organizationName is required")
    @Size(max = 255)
    private String organizationName;

    /**
     * URL-safe tenant slug; stored lowercase, unique globally.
     */
    @NotBlank(message = "organizationCode is required")
    @Size(min = 2, max = 64)
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{0,62}[a-z0-9])?$",
            message = "organizationCode must be lowercase letters, digits, optional interior hyphens")
    private String organizationCode;

    @NotBlank(message = "adminUsername is required")
    @Size(min = 2, max = 64)
    private String adminUsername;

    @NotBlank(message = "adminPassword is required")
    @Size(min = 8, max = 128)
    private String adminPassword;

    @NotBlank(message = "adminEmail is required")
    @Email(message = "adminEmail must be a valid email")
    @Size(max = 255)
    private String adminEmail;
}
