package com.digitalsignage.admin.device.integration;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.enums.LayoutStatus;
import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.WsStatus;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceActiveConfigIntegrationTest {

    /** 64 hex chars — use in Postman as Authorization: Bearer {token} */
    static final String KNOWN_DEVICE_TOKEN =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LayoutRepository layoutRepository;

    @Autowired
    private LayoutRegionRepository layoutRegionRepository;

    @Autowired
    private LayoutRegionComponentRepository layoutRegionComponentRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @BeforeEach
    void cleanAndSeed() {
        scheduleRepository.deleteAll();
        playlistItemRepository.deleteAll();
        screenRepository.deleteAll();
        layoutRegionComponentRepository.deleteAll();
        layoutRegionRepository.deleteAll();
        layoutRepository.deleteAll();
        playlistRepository.deleteAll();
        mediaRepository.deleteAll();
        organizationRepository.deleteAll();

        Organization org = new Organization();
        org.setName("Active Config IT");
        org.setCode("aci-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        org.setStatus(OrganizationStatus.ACTIVE);
        organizationRepository.save(org);

        Layout layout = new Layout();
        layout.setOrganization(org);
        layout.setName("IT Layout");
        layout.setTemplateType("FULLSCREEN");
        layout.setResolutionWidth(1920);
        layout.setResolutionHeight(1080);
        layout.setStatus(LayoutStatus.PUBLISHED);
        layoutRepository.save(layout);

        LayoutRegion region = new LayoutRegion();
        region.setLayout(layout);
        region.setRegionName("main");
        region.setX(0);
        region.setY(0);
        region.setWidth(1920);
        region.setHeight(1080);
        region.setZIndex(0);
        layoutRegionRepository.save(region);

        LayoutRegionComponent component = new LayoutRegionComponent();
        component.setRegion(region);
        component.setComponentType("IMAGE");
        component.setConfigJson("{}");
        component.setSortOrder(0);
        layoutRegionComponentRepository.save(component);

        Media media = new Media();
        media.setOrganization(org);
        media.setMediaType(MediaType.IMAGE);
        media.setName("Poster");
        media.setObjectKey("it/active-config/" + UUID.randomUUID() + ".jpg");
        media.setFileUrl("https://example.com/poster.jpg");
        media.setDurationSeconds(30);
        mediaRepository.save(media);

        Playlist playlist = new Playlist();
        playlist.setOrganization(org);
        playlist.setName("IT Playlist");
        playlist.setStatus(PlaylistStatus.ACTIVE);
        playlistRepository.save(playlist);

        PlaylistItem item = new PlaylistItem();
        item.setPlaylist(playlist);
        item.setMedia(media);
        item.setOrderIndex(0);
        item.setDurationSeconds(30);
        playlistItemRepository.save(item);

        Schedule schedule = new Schedule();
        schedule.setOrganization(org);
        schedule.setName("IT Schedule");
        schedule.setTargetType(ScheduleTargetType.DEFAULT);
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(LocalDateTime.of(2020, 1, 1, 0, 0));
        schedule.setEndDatetime(LocalDateTime.of(2030, 12, 31, 23, 59, 59));
        schedule.setPriority(1);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        scheduleRepository.save(schedule);

        Screen screen = new Screen();
        screen.setOrganization(org);
        screen.setDeviceCode("DEV-AC-" + UUID.randomUUID().toString().substring(0, 8));
        screen.setName("IT Screen");
        screen.setActivationStatus(ActivationStatus.ACTIVATED);
        screen.setDeviceToken(KNOWN_DEVICE_TOKEN);
        screen.setStatus(ScreenStatus.OFFLINE);
        screen.setWsStatus(WsStatus.DISCONNECTED);
        screenRepository.save(screen);
        screenRepository.flush();

        assertThat(screenRepository.findByDeviceToken(KNOWN_DEVICE_TOKEN)).isPresent();
    }

    @Test
    void activeConfig_withDeviceBearer_returnsScheduleAndPlaylist() throws Exception {
        mockMvc.perform(get("/api/device/active-config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + KNOWN_DEVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scheduleId").exists())
                .andExpect(jsonPath("$.data.layout.id").exists())
                .andExpect(jsonPath("$.data.layout.regions[0].regionName").value("main"))
                .andExpect(jsonPath("$.data.playlist.items[0].mediaId").exists());
    }
}
