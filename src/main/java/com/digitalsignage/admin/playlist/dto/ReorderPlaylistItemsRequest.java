package com.digitalsignage.admin.playlist.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderPlaylistItemsRequest {

    @NotEmpty
    private List<Long> itemIdsInOrder;
}
