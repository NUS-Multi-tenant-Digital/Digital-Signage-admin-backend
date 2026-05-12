package com.digitalsignage.admin.playlist.dto;

import com.digitalsignage.admin.common.enums.PlaylistStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePlaylistRequest {

    @NotBlank
    private String name;

    @NotNull
    private PlaylistStatus status;

    @NotEmpty
    @Valid
    private List<PlaylistItemRequest> items;
}
