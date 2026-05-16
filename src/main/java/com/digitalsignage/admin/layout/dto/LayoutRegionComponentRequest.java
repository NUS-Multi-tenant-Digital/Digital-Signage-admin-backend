package com.digitalsignage.admin.layout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LayoutRegionComponentRequest {

    @NotBlank(message = "componentType is required")
    @Size(max = 64)
    private String componentType;

    @NotBlank(message = "configJson is required")
    private String configJson;

    /**
     * Display order within the region; when omitted, order follows list index on save.
     */
    private Integer sortOrder;
}
