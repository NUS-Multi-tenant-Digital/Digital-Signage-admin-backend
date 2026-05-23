package com.digitalsignage.admin.device.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveConfigResponseTest {

    @Test
    void toString_includesNestedPayloadFields() {
        ActiveConfigResponse response = ActiveConfigResponse.builder()
                .scheduleId(1L)
                .resolvedAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .layout(ActiveConfigResponse.LayoutPayload.builder()
                        .id(2L)
                        .name("Main")
                        .regions(List.of(ActiveConfigResponse.RegionPayload.builder()
                                .id(3L)
                                .regionName("R1")
                                .components(List.of(ActiveConfigResponse.RegionComponentPayload.builder()
                                        .id(4L)
                                        .componentType("VIDEO")
                                        .build()))
                                .build()))
                        .build())
                .playlist(ActiveConfigResponse.PlaylistPayload.builder()
                        .id(5L)
                        .name("Loop")
                        .items(List.of(ActiveConfigResponse.ItemPayload.builder()
                                .mediaId(6L)
                                .name("clip")
                                .build()))
                        .build())
                .build();

        String text = response.toString();

        assertThat(text).contains("scheduleId=1");
        assertThat(text).contains("Main");
        assertThat(text).contains("R1");
        assertThat(text).contains("VIDEO");
        assertThat(text).contains("Loop");
        assertThat(text).contains("clip");
    }
}
