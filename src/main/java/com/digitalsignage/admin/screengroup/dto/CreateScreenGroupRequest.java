package com.digitalsignage.admin.screengroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScreenGroupRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 512)
    private String location;
}
