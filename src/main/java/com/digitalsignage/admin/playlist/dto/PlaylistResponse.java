package com.digitalsignage.admin.playlist.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlaylistResponse {

    private Long id;
    private String name;
    private String status;
    private List<PlaylistItemResponse> items;

}
