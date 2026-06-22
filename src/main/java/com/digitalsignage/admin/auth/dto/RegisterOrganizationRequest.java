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

    private RegistrationType registrationType;

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

    @NotBlank(message = "username is required")
    @Size(min = 2, max = 64)
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email")
    @Size(max = 255)
    private String email;
}
