package com.digitalsignage.admin.schedule.service.impl;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.common.enums.ScheduleTargetType;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.device.service.ActiveConfigService;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.entity.ScreenGroup;
import com.digitalsignage.admin.layout.repository.LayoutRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.schedule.dto.CreateScheduleRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckRequest;
import com.digitalsignage.admin.schedule.dto.ScheduleConflictCheckResponse;
import com.digitalsignage.admin.schedule.dto.ScheduleResponse;
import com.digitalsignage.admin.schedule.dto.UpdateScheduleRequest;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.schedule.service.ScheduleService;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.screengroup.repository.ScreenGroupRepository;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.digitalsignage.admin.websocket.ConfigPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final LayoutRepository layoutRepository;
    private final PlaylistRepository playlistRepository;
    private final ScreenRepository screenRepository;
    private final ScreenGroupRepository screenGroupRepository;
    private final OrganizationRepository organizationRepository;
    private final ActiveConfigService activeConfigService;
    private final ConfigPushService configPushService;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules() {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        return scheduleRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId).stream()
                .map(ScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Schedule schedule = scheduleRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "schedule not found"));
        return ScheduleResponse.fromEntity(schedule);
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        validateTimeRange(request.getStartDatetime(), request.getEndDatetime());
        validateTargets(request.getTargetType(), request.getScreenId(), request.getScreenGroupId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));
        Layout layout = layoutRepository.findByIdAndOrganization_Id(request.getLayoutId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        Playlist playlist = playlistRepository.findByIdAndOrganization_Id(request.getPlaylistId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        Schedule schedule = new Schedule();
        schedule.setOrganization(organization);
        schedule.setName(request.getName().trim());
        schedule.setTargetType(request.getTargetType());
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(request.getStartDatetime());
        schedule.setEndDatetime(request.getEndDatetime());
        schedule.setPriority(request.getPriority());
        schedule.setStatus(request.getStatus());
        bindTargets(schedule, organizationId, request.getTargetType(), request.getScreenId(), request.getScreenGroupId());
        Schedule saved = scheduleRepository.save(schedule);
        configPushService.notifyLayoutChanged(saved.getLayout().getId());
        return ScheduleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Schedule schedule = scheduleRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "schedule not found"));
        validateTimeRange(request.getStartDatetime(), request.getEndDatetime());
        validateTargets(request.getTargetType(), request.getScreenId(), request.getScreenGroupId());
        Long previousLayoutId = schedule.getLayout().getId();
        Layout layout = layoutRepository.findByIdAndOrganization_Id(request.getLayoutId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "layout not found"));
        Playlist playlist = playlistRepository.findByIdAndOrganization_Id(request.getPlaylistId(), organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        schedule.setName(request.getName().trim());
        schedule.setTargetType(request.getTargetType());
        schedule.setLayout(layout);
        schedule.setPlaylist(playlist);
        schedule.setStartDatetime(request.getStartDatetime());
        schedule.setEndDatetime(request.getEndDatetime());
        schedule.setPriority(request.getPriority());
        schedule.setStatus(request.getStatus());
        bindTargets(schedule, organizationId, request.getTargetType(), request.getScreenId(), request.getScreenGroupId());
        Schedule saved = scheduleRepository.save(schedule);
        configPushService.notifyLayoutChanged(saved.getLayout().getId());
        if (!Objects.equals(previousLayoutId, saved.getLayout().getId())) {
            configPushService.notifyLayoutChanged(previousLayoutId);
        }
        return ScheduleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Schedule schedule = scheduleRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "schedule not found"));
        Long layoutId = schedule.getLayout().getId();
        scheduleRepository.delete(schedule);
        configPushService.notifyLayoutChanged(layoutId);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleConflictCheckResponse checkConflict(ScheduleConflictCheckRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        validateTimeRange(request.getStartDatetime(), request.getEndDatetime());
        validateTargets(request.getTargetType(), request.getScreenId(), request.getScreenGroupId());
        List<Schedule> all = scheduleRepository.findByOrganization_Id(organizationId);
        List<Long> conflicts = new ArrayList<>();
        for (Schedule other : all) {
            if (request.getExcludeScheduleId() != null && Objects.equals(other.getId(), request.getExcludeScheduleId())) {
                continue;
            }
            if (!timeOverlaps(request.getStartDatetime(), request.getEndDatetime(),
                    other.getStartDatetime(), other.getEndDatetime())) {
                continue;
            }
            if (sameTarget(other, request.getTargetType(), request.getScreenId(), request.getScreenGroupId())) {
                conflicts.add(other.getId());
            }
        }
        return ScheduleConflictCheckResponse.builder()
                .conflict(!conflicts.isEmpty())
                .conflictingScheduleIds(conflicts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveConfigResponse resolveForScreen(Long screenId, LocalDateTime at) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Screen loaded = screenRepository.fetchForResolve(screenId)
                .orElseThrow(() -> new BusinessException(404, "screen not found"));
        if (!loaded.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException(404, "screen not found");
        }
        LocalDateTime point = at != null ? at : LocalDateTime.now();
        return activeConfigService.resolve(loaded, point)
                .orElseThrow(() -> new BusinessException(404, "no active configuration"));
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException(400, "endDatetime must be after startDatetime");
        }
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

    private boolean timeOverlaps(LocalDateTime s1, LocalDateTime e1, LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
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
