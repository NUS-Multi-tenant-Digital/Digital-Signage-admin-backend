package com.digitalsignage.admin.layout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LayoutRegionRequest {

    @NotBlank(message = "regionName is required")
    @Size(max = 128)
    private String regionName;

    @NotNull(message = "x is required")
    private Integer x;

    @NotNull(message = "y is required")
    private Integer y;

    @NotNull(message = "width is required")
    private Integer width;

    @NotNull(message = "height is required")
    private Integer height;

    @NotNull(message = "zIndex is required")
    private Integer zIndex;

    @NotEmpty(message = "components is required")
    @Valid
    private List<LayoutRegionComponentRequest> components;
}
