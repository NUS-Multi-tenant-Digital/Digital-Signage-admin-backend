package com.digitalsignage.admin.emergency.service.impl;

import com.digitalsignage.admin.common.enums.LayoutStatus;
import com.digitalsignage.admin.common.enums.PlaylistStatus;
import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.emergency.EmergencyOverrideConstants;
import com.digitalsignage.admin.emergency.dto.ActivateEmergencyOverrideRequest;
import com.digitalsignage.admin.emergency.dto.EmergencyOverrideResponse;
import com.digitalsignage.admin.emergency.service.EmergencyOverrideService;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.digitalsignage.admin.websocket.ConfigPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmergencyOverrideServiceImpl implements EmergencyOverrideService {

    private static final String LAYOUT_TEMPLATE = "SINGLE_FULL";
    private static final String COMPONENT_TYPE = "PLAYLIST";
    private static final String EMPTY_CONFIG_JSON = "{}";

    private final ScheduleRepository scheduleRepository;
    private final LayoutRepository layoutRepository;
    private final LayoutRegionRepository layoutRegionRepository;
    private final LayoutRegionComponentRepository layoutRegionComponentRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final MediaRepository mediaRepository;
    private final ScreenRepository screenRepository;
    private final ScreenGroupRepository screenGroupRepository;
    private final OrganizationRepository organizationRepository;
    private final ConfigPushService configPushService;

    @Override
    @Transactional
    public EmergencyOverrideResponse activate(ActivateEmergencyOverrideRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        validateTargets(request.getTargetType(), request.getScreenId(), request.getScreenGroupId());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));
        Media media = mediaRepository.findByIdAndOrganization_Id(request.getMediaId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "media not found"));

        cancelActiveEmergenciesForTarget(
                organizationId,
                request.getTargetType(),
                request.getScreenId(),
                request.getScreenGroupId());

        LocalDateTime start = LocalDateTime.now();
        int durationMinutes = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : EmergencyOverrideConstants.DEFAULT_DURATION_MINUTES;
        LocalDateTime end = start.plusMinutes(durationMinutes);

        String scheduleLabel = EmergencyOverrideConstants.scheduleName(request.getName());
        String resourceSuffix = String.valueOf(System.currentTimeMillis());

        Playlist playlist = createEmergencyPlaylist(organization, media, resourceSuffix);
        Layout layout = createEmergencyLayout(organization, resourceSuffix);
        Schedule schedule = createEmergencySchedule(
                organization,
                scheduleLabel,
                request.getTargetType(),
                request.getScreenId(),
                request.getScreenGroupId(),
                layout,
                playlist,
                start,
                end);

        configPushService.notifyLayoutChanged(layout.getId());
        return EmergencyOverrideResponse.fromSchedule(schedule, media.getId());
    }

    @Override
    @Transactional
    public void cancel(Long scheduleId) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Schedule schedule = scheduleRepository.findByIdAndOrganization_Id(scheduleId, organizationId)
                .orElseThrow(() -> new BusinessException(404, "emergency override not found"));
        if (!EmergencyOverrideConstants.isEmergencyScheduleName(schedule.getName())) {
            throw new BusinessException(400, "not an emergency override schedule");
        }
        if (schedule.getStatus() != ScheduleStatus.ACTIVE) {
            throw new BusinessException(400, "emergency override is not active");
        }
        schedule.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepository.save(schedule);
        configPushService.notifyLayoutChanged(schedule.getLayout().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyOverrideResponse> listActive() {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        LocalDateTime now = LocalDateTime.now();
        return scheduleRepository
                .findActiveEmergencySchedules(organizationId, EmergencyOverrideConstants.SCHEDULE_NAME_PREFIX, now)
                .stream()
                .map(schedule -> {
                    Long mediaId = playlistItemRepository.findWithMediaByPlaylist_Id(schedule.getPlaylist().getId())
                            .stream()
                            .findFirst()
                            .map(item -> item.getMedia().getId())
                            .orElse(null);
                    return EmergencyOverrideResponse.fromSchedule(schedule, mediaId);
                })
                .toList();
    }

    private void cancelActiveEmergenciesForTarget(
            Long organizationId,
            ScheduleTargetType targetType,
            Long screenId,
            Long screenGroupId) {
        LocalDateTime now = LocalDateTime.now();
        List<Schedule> active = scheduleRepository.findActiveEmergencySchedules(
                organizationId, EmergencyOverrideConstants.SCHEDULE_NAME_PREFIX, now);
        for (Schedule existing : active) {
            if (sameTarget(existing, targetType, screenId, screenGroupId)) {
                existing.setStatus(ScheduleStatus.CANCELLED);
                scheduleRepository.save(existing);
                configPushService.notifyLayoutChanged(existing.getLayout().getId());
            }
        }
    }

    private Playlist createEmergencyPlaylist(Organization organization, Media media, String suffix) {
        Playlist playlist = new Playlist();
        playlist.setOrganization(organization);
        playlist.setName("EMERGENCY-PL-" + suffix);
        playlist.setStatus(PlaylistStatus.ACTIVE);
        Playlist saved = playlistRepository.save(playlist);

        PlaylistItem item = new PlaylistItem();
        item.setPlaylist(saved);
        item.setMedia(media);
        item.setOrderIndex(0);
        item.setDurationSeconds(media.getDurationSeconds());
        playlistItemRepository.save(item);
        return saved;
    }

    private Layout createEmergencyLayout(Organization organization, String suffix) {
        Layout layout = new Layout();
        layout.setOrganization(organization);
        layout.setName("EMERGENCY-LY-" + suffix);
        layout.setTemplateType(LAYOUT_TEMPLATE);
        layout.setResolutionWidth(EmergencyOverrideConstants.DEFAULT_RESOLUTION_WIDTH);
        layout.setResolutionHeight(EmergencyOverrideConstants.DEFAULT_RESOLUTION_HEIGHT);
        layout.setStatus(LayoutStatus.PUBLISHED);
        Layout saved = layoutRepository.save(layout);

        LayoutRegion region = new LayoutRegion();
        region.setLayout(saved);
        region.setRegionName("main");
        region.setX(0);
        region.setY(0);
        region.setWidth(EmergencyOverrideConstants.DEFAULT_RESOLUTION_WIDTH);
        region.setHeight(EmergencyOverrideConstants.DEFAULT_RESOLUTION_HEIGHT);
        region.setZIndex(1);
        LayoutRegion savedRegion = layoutRegionRepository.save(region);

        LayoutRegionComponent component = new LayoutRegionComponent();
        component.setRegion(savedRegion);
        component.setComponentType(COMPONENT_TYPE);
        component.setConfigJson(EMPTY_CONFIG_JSON);
        component.setSortOrder(0);
        layoutRegionComponentRepository.save(component);
        return saved;
    }

    private Schedule createEmergencySchedule(
            Organization organization,
            String scheduleName,
            ScheduleTargetType targetType,
            Long screenId,
            Long screenGroupId,
            Layout layout,
            Playlist playlist,
            LocalDateTime start,
            LocalDateTime end) {
        Schedule schedule = new Schedule();
        schedule.setOrganization(organization);
        schedule.setName(scheduleName);
        schedule.setTargetType(targetType);
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(start);
        schedule.setEndDatetime(end);
        schedule.setPriority(EmergencyOverrideConstants.PRIORITY);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        bindTargets(schedule, organization.getId(), targetType, screenId, screenGroupId);
        return scheduleRepository.save(schedule);
    }

    private void validateTargets(ScheduleTargetType type, Long screenId, Long groupId) {
        switch (type) {
            case SCREEN -> {
                if (screenId == null) {
                    throw new BusinessException(400, "screenId is required");
                }
                if (groupId != null) {
                    throw new BusinessException(400, "screenGroupId must be null");
                }
            }
            case GROUP -> {
                if (groupId == null) {
                    throw new BusinessException(400, "screenGroupId is required");
                }
                if (screenId != null) {
                    throw new BusinessException(400, "screenId must be null");
                }
            }
            case DEFAULT -> {
                if (screenId != null || groupId != null) {
                    throw new BusinessException(400, "screen and screenGroup must be null for DEFAULT target");
                }
            }
        }
    }

    private void bindTargets(
            Schedule schedule,
            Long organizationId,
            ScheduleTargetType type,
            Long screenId,
            Long groupId) {
        schedule.setScreen(null);
        schedule.setScreenGroup(null);
        switch (type) {
            case SCREEN -> {
                Screen screen = screenRepository.findByIdAndOrganization_Id(screenId, organizationId)
                        .orElseThrow(() -> new BusinessException(404, "screen not found"));
                schedule.setScreen(screen);
            }
            case GROUP -> {
                ScreenGroup group = screenGroupRepository.findByIdAndOrganization_Id(groupId, organizationId)
                        .orElseThrow(() -> new BusinessException(404, "screen group not found"));
                schedule.setScreenGroup(group);
            }
            case DEFAULT -> {
                // no references
            }
        }
    }

    private boolean sameTarget(
            Schedule existing,
            ScheduleTargetType type,
            Long screenId,
            Long groupId) {
        if (existing.getTargetType() != type) {
            return false;
        }
        return switch (type) {
            case SCREEN -> existing.getScreen() != null && Objects.equals(existing.getScreen().getId(), screenId);
            case GROUP -> existing.getScreenGroup() != null
                    && Objects.equals(existing.getScreenGroup().getId(), groupId);
            case DEFAULT -> true;
        };
    }
}
