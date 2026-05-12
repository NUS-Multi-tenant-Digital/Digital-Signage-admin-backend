package com.digitalsignage.admin.playlist.controller;

import com.digitalsignage.admin.common.api.ApiResponse;
import com.digitalsignage.admin.playlist.dto.CreatePlaylistRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistResponse;
import com.digitalsignage.admin.playlist.dto.ReorderPlaylistItemsRequest;
import com.digitalsignage.admin.playlist.dto.UpdatePlaylistRequest;
import com.digitalsignage.admin.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.admin-prefix}")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping("/playlists")
    public ApiResponse<List<PlaylistResponse>> list() {
        return ApiResponse.ok(playlistService.listPlaylists());
    }

    @GetMapping("/playlists/{id}")
    public ApiResponse<PlaylistResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(playlistService.getPlaylist(id));
    }

    @PostMapping("/playlists")
    public ApiResponse<PlaylistResponse> create(@Valid @RequestBody CreatePlaylistRequest request) {
        return ApiResponse.ok(playlistService.createPlaylist(request));
    }

    @PutMapping("/playlists/{id}")
    public ApiResponse<PlaylistResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlaylistRequest request) {
        return ApiResponse.ok(playlistService.updatePlaylist(id, request));
    }

    @DeleteMapping("/playlists/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return ApiResponse.ok();
    }

    @PutMapping("/playlists/{id}/items/order")
    public ApiResponse<PlaylistResponse> reorder(
            @PathVariable Long id,
            @Valid @RequestBody ReorderPlaylistItemsRequest request) {
        return ApiResponse.ok(playlistService.reorderItems(id, request));
    }
}
