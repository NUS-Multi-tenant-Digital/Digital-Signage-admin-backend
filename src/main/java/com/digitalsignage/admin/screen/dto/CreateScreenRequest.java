package com.digitalsignage.admin.screen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScreenRequest {

    @NotBlank
    @Size(max = 64)
    private String deviceCode;

    @NotBlank
    @Size(max = 255)
    private String name;

    private Long screenGroupId;
}
