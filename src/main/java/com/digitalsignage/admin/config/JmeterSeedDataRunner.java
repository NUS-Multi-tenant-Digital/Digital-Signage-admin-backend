package com.digitalsignage.admin.config;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.common.enums.LayoutStatus;
import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.enums.OrganizationStatus;
import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.common.enums.SysUserStatus;
import com.digitalsignage.admin.common.enums.UserRole;
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
import com.digitalsignage.admin.entity.SysUser;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds deterministic users, screens, and schedules for JMeter load/stress runs (profile {@code jmeter}).
 */
@Slf4j
@Component
@Profile("jmeter")
@RequiredArgsConstructor
public class JmeterSeedDataRunner implements ApplicationRunner {

    public static final String PERF_USERNAME = "perf_admin";
    public static final String PERF_PASSWORD = "PerfTest123!";
    public static final int PENDING_ACTIVATION_SCREENS = 100;
    public static final int ACTIVATED_DEVICE_SCREENS = 50;

    private final OrganizationRepository organizationRepository;
    private final SysUserRepository sysUserRepository;
    private final ScreenRepository screenRepository;
    private final LayoutRepository layoutRepository;
    private final LayoutRegionRepository layoutRegionRepository;
    private final LayoutRegionComponentRepository layoutRegionComponentRepository;
    private final MediaRepository mediaRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sysUserRepository.findByUsername(PERF_USERNAME).isPresent()) {
            log.info("JMeter seed data already present (user={})", PERF_USERNAME);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Organization org = new Organization();
        org.setName("Performance Test Org");
        org.setCode("PERF_ORG");
        org.setStatus(OrganizationStatus.ACTIVE);
        organizationRepository.save(org);

        SysUser user = new SysUser();
        user.setOrganization(org);
        user.setUsername(PERF_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(PERF_PASSWORD));
        user.setEmail("perf_admin@example.com");
        user.setRole(UserRole.ADMIN);
        user.setStatus(SysUserStatus.ACTIVE);
        sysUserRepository.save(user);

        Layout layout = new Layout();
        layout.setOrganization(org);
        layout.setName("Perf Layout");
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
        media.setName("Perf Poster");
        media.setObjectKey("perf/poster.jpg");
        media.setFileUrl("https://example.com/perf/poster.jpg");
        media.setDurationSeconds(30);
        mediaRepository.save(media);

        Playlist playlist = new Playlist();
        playlist.setOrganization(org);
        playlist.setName("Perf Playlist");
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
        schedule.setName("Perf Schedule");
        schedule.setTargetType(ScheduleTargetType.DEFAULT);
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(LocalDateTime.of(2020, 1, 1, 0, 0));
        schedule.setEndDatetime(LocalDateTime.of(2030, 12, 31, 23, 59, 59));
        schedule.setPriority(1);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        scheduleRepository.save(schedule);

        for (int i = 1; i <= PENDING_ACTIVATION_SCREENS; i++) {
            Screen pending = new Screen();
            pending.setOrganization(org);
            pending.setDeviceCode(deviceCode(i));
            pending.setName("Pending Screen " + i);
            pending.setActivationCode(activationCode(i));
            pending.setActivationStatus(ActivationStatus.PENDING);
            pending.setStatus(ScreenStatus.OFFLINE);
            pending.setWsStatus(WsStatus.DISCONNECTED);
            screenRepository.save(pending);
        }

        for (int i = 1; i <= ACTIVATED_DEVICE_SCREENS; i++) {
            Screen activated = new Screen();
            activated.setOrganization(org);
            activated.setDeviceCode("DEV-ACTIVE-" + String.format("%03d", i));
            activated.setName("Active Screen " + i);
            activated.setActivationStatus(ActivationStatus.ACTIVATED);
            activated.setDeviceToken(deviceToken(i));
            activated.setStatus(ScreenStatus.OFFLINE);
            activated.setWsStatus(WsStatus.DISCONNECTED);
            screenRepository.save(activated);
        }

        log.info(
                "JMeter seed complete: user={}, pendingActivations={}, activatedDevices={}",
                PERF_USERNAME,
                PENDING_ACTIVATION_SCREENS,
                ACTIVATED_DEVICE_SCREENS);
    }

    static String deviceCode(int index) {
        return "DEV-PERF-" + String.format("%03d", index);
    }

    static String activationCode(int index) {
        return "PERF-ACT-" + String.format("%03d", index);
    }

    /** 64-char hex device token aligned with performance/jmeter/data/device-tokens.csv */
    static String deviceToken(int index) {
        return String.format("perf%060x", index);
    }
}
