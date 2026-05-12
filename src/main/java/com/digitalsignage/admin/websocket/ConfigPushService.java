package com.digitalsignage.admin.websocket;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfigPushService {

    private final ScheduleRepository scheduleRepository;
    private final ScreenRepository screenRepository;
    private final DeviceSessionRegistry deviceSessionRegistry;

    public void notifyLayoutChanged(Long layoutId) {
        if (layoutId == null) {
            return;
        }
        Set<Long> screenIds = collectScreensForActiveSchedules(layoutId);
        for (Long screenId : screenIds) {
            deviceSessionRegistry.sendConfigUpdated(screenId, layoutId);
        }
    }

    /**
     * When playlist content changes, push all layouts that appear on active schedules using this playlist.
     */
    public void notifyPlaylistChanged(Long playlistId) {
        if (playlistId == null) {
            return;
        }
        scheduleRepository.fetchByPlaylist_IdWithLayout(playlistId).stream()
                .filter(s -> s.getStatus() == ScheduleStatus.ACTIVE)
                .map(s -> s.getLayout().getId())
                .distinct()
                .forEach(this::notifyLayoutChanged);
    }

    private Set<Long> collectScreensForActiveSchedules(Long layoutId) {
        List<Schedule> schedules = scheduleRepository.fetchByLayout_IdWithOrganization(layoutId);
        Set<Long> screenIds = new HashSet<>();
        for (Schedule s : schedules) {
            if (s.getStatus() != ScheduleStatus.ACTIVE) {
                continue;
            }
            expandScheduleTargets(s, screenIds);
        }
        return screenIds;
    }

    private void expandScheduleTargets(Schedule schedule, Set<Long> screenIds) {
        switch (schedule.getTargetType()) {
            case SCREEN -> {
                if (schedule.getScreen() != null) {
                    screenIds.add(schedule.getScreen().getId());
                }
            }
            case GROUP -> {
                if (schedule.getScreenGroup() != null) {
                    screenRepository.findByScreenGroup_Id(schedule.getScreenGroup().getId()).forEach(sc -> screenIds.add(sc.getId()));
                }
            }
            case DEFAULT -> screenRepository.findByOrganization_Id(schedule.getOrganization().getId())
                    .forEach(sc -> screenIds.add(sc.getId()));
        }
    }
}
