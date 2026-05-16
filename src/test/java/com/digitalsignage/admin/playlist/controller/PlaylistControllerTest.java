package com.digitalsignage.admin.playlist.controller;

import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.exception.GlobalExceptionHandler;
import com.digitalsignage.admin.playlist.dto.CreatePlaylistRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistItemRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistResponse;
import com.digitalsignage.admin.playlist.dto.ReorderPlaylistItemsRequest;
import com.digitalsignage.admin.playlist.dto.UpdatePlaylistRequest;
import com.digitalsignage.admin.playlist.service.PlaylistService;
import com.digitalsignage.admin.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaylistController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaylistService playlistService;

    @MockBean
    private JwtService jwtService;

    private static PlaylistResponse sample() {
        return PlaylistResponse.builder()
                .id(1L)
                .name("Main")
                .status("ACTIVE")
                .items(List.of())
                .build();
    }

    @Test
    void list_returnsOk() throws Exception {
        when(playlistService.listPlaylists()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/admin/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Main"));
    }

    @Test
    void get_returnsOk() throws Exception {
        when(playlistService.getPlaylist(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/admin/playlists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_returnsOk() throws Exception {
        when(playlistService.createPlaylist(any(CreatePlaylistRequest.class))).thenReturn(sample());

        CreatePlaylistRequest req = new CreatePlaylistRequest();
        req.setName("Main");
        req.setStatus(PlaylistStatus.ACTIVE);
        PlaylistItemRequest item = new PlaylistItemRequest();
        item.setMediaId(99L);
        item.setOrderIndex(0);
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/admin/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsOk() throws Exception {
        when(playlistService.updatePlaylist(eq(1L), any(UpdatePlaylistRequest.class))).thenReturn(sample());

        UpdatePlaylistRequest req = new UpdatePlaylistRequest();
        req.setName("Main 2");
        req.setStatus(PlaylistStatus.ACTIVE);
        PlaylistItemRequest item = new PlaylistItemRequest();
        item.setMediaId(1L);
        req.setItems(List.of(item));

        mockMvc.perform(put("/api/admin/playlists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returnsOk() throws Exception {
        doNothing().when(playlistService).deletePlaylist(2L);

        mockMvc.perform(delete("/api/admin/playlists/2"))
                .andExpect(status().isOk());
        verify(playlistService).deletePlaylist(2L);
    }

    @Test
    void reorder_returnsOk() throws Exception {
        when(playlistService.reorderItems(eq(1L), any(ReorderPlaylistItemsRequest.class))).thenReturn(sample());

        ReorderPlaylistItemsRequest req = new ReorderPlaylistItemsRequest();
        req.setItemIdsInOrder(List.of(10L, 11L));

        mockMvc.perform(put("/api/admin/playlists/1/items/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
