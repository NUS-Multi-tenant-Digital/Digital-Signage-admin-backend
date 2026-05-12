package com.digitalsignage.admin.screengroup.dto;

import com.digitalsignage.admin.entity.ScreenGroup;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScreenGroupResponse {

    private Long id;
    private String name;
    private String location;

    public static ScreenGroupResponse fromEntity(ScreenGroup group) {
        return ScreenGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .location(group.getLocation())
                .build();
    }
}
