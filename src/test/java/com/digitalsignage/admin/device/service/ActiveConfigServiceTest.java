package com.digitalsignage.admin.device.service;

import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveConfigServiceTest {

    private static final Long ORG_ID = 10L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private LayoutRegionRepository layoutRegionRepository;

    @Mock
    private LayoutRegionComponentRepository layoutRegionComponentRepository;

    @Mock
    private PlaylistItemRepository playlistItemRepository;

    @InjectMocks
    private ActiveConfigService activeConfigService;

    @Test
    void resolve_matchingDefaultSchedule_returnsConfig() {
        LocalDateTime at = LocalDateTime.of(2026, 5, 19, 12, 0);

        Organization org = new Organization();
        org.setId(ORG_ID);

        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(org);

        Layout layout = new Layout();
        layout.setId(5L);
        layout.setName("Lobby Layout");
        layout.setTemplateType("SINGLE_FULL");
        layout.setResolutionWidth(1920);
        layout.setResolutionHeight(1080);

        Playlist playlist = new Playlist();
        playlist.setId(7L);
        playlist.setName("Ads");
        playlist.setStatus(PlaylistStatus.ACTIVE);

        Schedule schedule = new Schedule();
        schedule.setId(100L);
        schedule.setOrganization(org);
        schedule.setTargetType(ScheduleTargetType.DEFAULT);
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(LocalDateTime.of(2026, 5, 19, 8, 0));
        schedule.setEndDatetime(LocalDateTime.of(2026, 5, 19, 20, 0));
        schedule.setPriority(1);
        schedule.setStatus(ScheduleStatus.ACTIVE);

        LayoutRegion region = new LayoutRegion();
        region.setId(50L);
        region.setLayout(layout);
        region.setRegionName("main");
        region.setX(0);
        region.setY(0);
        region.setWidth(1920);
        region.setHeight(1080);
        region.setZIndex(1);

        Media media = new Media();
        media.setId(20L);
        media.setName("banner.png");
        media.setMediaType(MediaType.IMAGE);
        media.setFileUrl("https://cdn.example/banner.png");

        PlaylistItem item = new PlaylistItem();
        item.setId(30L);
        item.setPlaylist(playlist);
        item.setMedia(media);
        item.setOrderIndex(0);
        item.setDurationSeconds(10);

        when(scheduleRepository.fetchAllForOrganization(ORG_ID)).thenReturn(List.of(schedule));
        when(layoutRegionRepository.findByLayoutIdOrderBySort(5L)).thenReturn(List.of(region));
        when(layoutRegionComponentRepository.findByRegion_IdIn(List.of(50L))).thenReturn(List.of());
        when(playlistItemRepository.findWithMediaByPlaylist_Id(7L)).thenReturn(List.of(item));

        Optional<ActiveConfigResponse> result = activeConfigService.resolve(screen, at);

        assertThat(result).isPresent();
        assertThat(result.get().getScheduleId()).isEqualTo(100L);
        assertThat(result.get().getLayout().getName()).isEqualTo("Lobby Layout");
        assertThat(result.get().getPlaylist().getItems()).hasSize(1);
        assertThat(result.get().getPlaylist().getItems().get(0).getMediaId()).isEqualTo(20L);
    }

    @Test
    void resolve_noActiveSchedule_returnsEmpty() {
        Organization org = new Organization();
        org.setId(ORG_ID);

        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(org);

        when(scheduleRepository.fetchAllForOrganization(ORG_ID)).thenReturn(List.of());

        Optional<ActiveConfigResponse> result = activeConfigService.resolve(
                screen, LocalDateTime.of(2026, 5, 19, 12, 0));

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_screenTarget_onlyMatchesAssignedScreen() {
        LocalDateTime at = LocalDateTime.of(2026, 5, 19, 12, 0);

        Organization org = new Organization();
        org.setId(ORG_ID);

        Screen screen = new Screen();
        screen.setId(1L);
        screen.setOrganization(org);

        Screen otherScreen = new Screen();
        otherScreen.setId(2L);

        Layout layout = new Layout();
        layout.setId(5L);
        layout.setName("Layout");
        layout.setTemplateType("SINGLE_FULL");
        layout.setResolutionWidth(1920);
        layout.setResolutionHeight(1080);

        Playlist playlist = new Playlist();
        playlist.setId(7L);
        playlist.setName("P");

        Schedule forOther = new Schedule();
        forOther.setId(101L);
        forOther.setTargetType(ScheduleTargetType.SCREEN);
        forOther.setScreen(otherScreen);
        forOther.setLayout(layout);
        forOther.setPlaylist(playlist);
        forOther.setStartDatetime(LocalDateTime.of(2026, 5, 19, 8, 0));
        forOther.setEndDatetime(LocalDateTime.of(2026, 5, 19, 20, 0));
        forOther.setPriority(1);
        forOther.setStatus(ScheduleStatus.ACTIVE);

        when(scheduleRepository.fetchAllForOrganization(ORG_ID)).thenReturn(List.of(forOther));

        Optional<ActiveConfigResponse> result = activeConfigService.resolve(screen, at);

        assertThat(result).isEmpty();
    }
}
