package com.digitalsignage.admin.screen.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateScreenRequest {

    @Size(max = 255)
    private String name;

    private Long screenGroupId;
}
