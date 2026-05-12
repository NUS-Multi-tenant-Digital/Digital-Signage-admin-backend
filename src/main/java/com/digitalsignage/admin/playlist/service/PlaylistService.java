package com.digitalsignage.admin.playlist.service;

import com.digitalsignage.admin.playlist.dto.CreatePlaylistRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistResponse;
import com.digitalsignage.admin.playlist.dto.ReorderPlaylistItemsRequest;
import com.digitalsignage.admin.playlist.dto.UpdatePlaylistRequest;

import java.util.List;

public interface PlaylistService {

    List<PlaylistResponse> listPlaylists();

    PlaylistResponse getPlaylist(Long id);

    PlaylistResponse createPlaylist(CreatePlaylistRequest request);

    PlaylistResponse updatePlaylist(Long id, UpdatePlaylistRequest request);

    void deletePlaylist(Long id);

    PlaylistResponse reorderItems(Long id, ReorderPlaylistItemsRequest request);
}
