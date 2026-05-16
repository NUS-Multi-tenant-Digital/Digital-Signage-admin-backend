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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deletes tenant-scoped rows in FK-safe order for SpringBootTest integration tests
 * that share one in-memory database across test classes.
 */
@Component
public class IntegrationTestDataCleaner {

    @Autowired
    private PlaybackLogRepository playbackLogRepository;
    @Autowired
    private DeviceEventLogRepository deviceEventLogRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private PlaylistItemRepository playlistItemRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private LayoutRegionComponentRepository layoutRegionComponentRepository;
    @Autowired
    private LayoutRegionRepository layoutRegionRepository;
    @Autowired
    private LayoutRepository layoutRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private SysUserRepository sysUserRepository;
    @Autowired
    private ScreenGroupRepository screenGroupRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

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
