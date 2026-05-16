package com.digitalsignage.admin.testsupport;

import com.digitalsignage.admin.auth.repository.SysUserRepository;
import com.digitalsignage.admin.device.repository.DeviceEventLogRepository;
import com.digitalsignage.admin.device.repository.PlaybackLogRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deletes tenant-scoped rows in FK-safe order for {@code @SpringBootTest} integration tests
 * that share one in-memory database across test classes.
 */
@Component
@RequiredArgsConstructor
public class IntegrationTestDataCleaner {

    private final PlaybackLogRepository playbackLogRepository;
    private final DeviceEventLogRepository deviceEventLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final ScreenRepository screenRepository;
    private final LayoutRegionComponentRepository layoutRegionComponentRepository;
    private final LayoutRegionRepository layoutRegionRepository;
    private final LayoutRepository layoutRepository;
    private final PlaylistRepository playlistRepository;
    private final MediaRepository mediaRepository;
    private final SysUserRepository sysUserRepository;
    private final ScreenGroupRepository screenGroupRepository;
    private final OrganizationRepository organizationRepository;

    public void clearTenantData() {
        playbackLogRepository.deleteAll();
        deviceEventLogRepository.deleteAll();
        scheduleRepository.deleteAll();
        playlistItemRepository.deleteAll();
        screenRepository.deleteAll();
        layoutRegionComponentRepository.deleteAll();
        layoutRegionRepository.deleteAll();
        layoutRepository.deleteAll();
        playlistRepository.deleteAll();
        mediaRepository.deleteAll();
        sysUserRepository.deleteAll();
        screenGroupRepository.deleteAll();
        organizationRepository.deleteAll();
    }
}
