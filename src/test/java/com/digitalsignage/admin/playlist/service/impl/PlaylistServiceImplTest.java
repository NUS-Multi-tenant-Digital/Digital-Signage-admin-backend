package com.digitalsignage.admin.playlist.service.impl;

import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.dto.CreatePlaylistRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistItemRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistResponse;
import com.digitalsignage.admin.playlist.dto.ReorderPlaylistItemsRequest;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.digitalsignage.admin.websocket.ConfigPushService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistItemRepository playlistItemRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ConfigPushService configPushService;

    @InjectMocks
    private PlaylistServiceImpl playlistService;

    private Organization organization;
    private Playlist playlist;
    private Media media;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);

        playlist = new Playlist();
        playlist.setId(1L);
        playlist.setOrganization(organization);
        playlist.setName("Morning");
        playlist.setStatus(PlaylistStatus.ACTIVE);

        media = new Media();
        media.setId(50L);
        media.setOrganization(organization);
        media.setName("clip.mp4");
        media.setMediaType(MediaType.VIDEO);

        AdminPrincipal principal = AdminPrincipal.builder()
                .userId(1L)
                .organizationId(ORG_ID)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPlaylist_success() {
        CreatePlaylistRequest request = new CreatePlaylistRequest();
        request.setName("  New Playlist  ");
        request.setStatus(PlaylistStatus.ACTIVE);
        PlaylistItemRequest itemReq = new PlaylistItemRequest();
        itemReq.setMediaId(50L);
        itemReq.setDurationSeconds(30);
        request.setItems(List.of(itemReq));

        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(inv -> {
            Playlist p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });
        when(mediaRepository.findByIdAndOrganization_Id(50L, ORG_ID)).thenReturn(Optional.of(media));
        when(playlistRepository.findById(2L)).thenAnswer(inv -> {
            Playlist p = new Playlist();
            p.setId(2L);
            p.setName("New Playlist");
            p.setStatus(PlaylistStatus.ACTIVE);
            return Optional.of(p);
        });
        when(playlistItemRepository.findWithMediaByPlaylist_Id(2L)).thenReturn(List.of());

        PlaylistResponse response = playlistService.createPlaylist(request);

        assertThat(response.getName()).isEqualTo("New Playlist");
        verify(configPushService).notifyPlaylistChanged(2L);
    }

    @Test
    void deletePlaylist_referencedBySchedule_throws409() {
        when(playlistRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(playlist));
        when(scheduleRepository.existsByPlaylist_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> playlistService.deletePlaylist(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(playlistRepository, never()).delete(any());
    }

    @Test
    void reorderItems_countMismatch_throws400() {
        when(playlistRepository.findByIdAndOrganization_Id(1L, ORG_ID)).thenReturn(Optional.of(playlist));
        when(playlistItemRepository.findByPlaylist_IdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(new PlaylistItem()));

        ReorderPlaylistItemsRequest request = new ReorderPlaylistItemsRequest();
        request.setItemIdsInOrder(List.of());

        assertThatThrownBy(() -> playlistService.reorderItems(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 400);
    }

    @Test
    void getPlaylist_notFound_throws404() {
        when(playlistRepository.findByIdAndOrganization_Id(99L, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylist(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    @Test
    void listPlaylists_success() {
        when(playlistRepository.findByOrganization_IdOrderByUpdatedAtDesc(ORG_ID))
                .thenReturn(List.of(playlist));
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlist));
        when(playlistItemRepository.findWithMediaByPlaylist_Id(1L)).thenReturn(List.of());

        List<PlaylistResponse> responses = playlistService.listPlaylists();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Morning");
    }
}
