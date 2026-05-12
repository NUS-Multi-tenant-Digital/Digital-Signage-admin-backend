package com.digitalsignage.admin.device.service;

import com.digitalsignage.admin.common.enums.ScheduleStatus;
import com.digitalsignage.admin.device.dto.ActiveConfigResponse;
import com.digitalsignage.admin.entity.Layout;
import com.digitalsignage.admin.entity.LayoutRegion;
import com.digitalsignage.admin.entity.LayoutRegionComponent;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.entity.Schedule;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.layout.repository.LayoutRegionComponentRepository;
import com.digitalsignage.admin.layout.repository.LayoutRegionRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActiveConfigService {

    private final ScheduleRepository scheduleRepository;
    private final LayoutRegionRepository layoutRegionRepository;
    private final LayoutRegionComponentRepository layoutRegionComponentRepository;
    private final PlaylistItemRepository playlistItemRepository;

    public Optional<ActiveConfigResponse> resolve(Screen screen, LocalDateTime at) {
        Long orgId = screen.getOrganization().getId();
        List<Schedule> schedules = scheduleRepository.fetchAllForOrganization(orgId);
        Optional<Schedule> best = schedules.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.ACTIVE)
                .filter(s -> !at.isBefore(s.getStartDatetime()) && !at.isAfter(s.getEndDatetime()))
                .filter(s -> matchesTarget(s, screen))
                .max(Comparator.comparing(Schedule::getPriority)
                        .thenComparing(Schedule::getId));
        if (best.isEmpty()) {
            return Optional.empty();
        }
        Schedule schedule = best.get();
        Layout layout = schedule.getLayout();
        List<LayoutRegion> regions = layoutRegionRepository.findByLayoutIdOrderBySort(layout.getId());
        List<PlaylistItem> items = playlistItemRepository.findWithMediaByPlaylist_Id(schedule.getPlaylist().getId());
        return Optional.of(ActiveConfigResponse.builder()
                .scheduleId(schedule.getId())
                .resolvedAt(at)
                .scheduleStart(schedule.getStartDatetime())
                .scheduleEnd(schedule.getEndDatetime())
                .priority(schedule.getPriority())
                .layout(toLayoutPayload(layout, regions))
                .playlist(toPlaylistPayload(schedule.getPlaylist().getId(), schedule.getPlaylist().getName(), items))
                .build());
    }

    private boolean matchesTarget(Schedule schedule, Screen screen) {
        return switch (schedule.getTargetType()) {
            case SCREEN -> schedule.getScreen() != null && schedule.getScreen().getId().equals(screen.getId());
            case GROUP -> schedule.getScreenGroup() != null
                    && screen.getScreenGroup() != null
                    && schedule.getScreenGroup().getId().equals(screen.getScreenGroup().getId());
            case DEFAULT -> true;
        };
    }

    private ActiveConfigResponse.LayoutPayload toLayoutPayload(Layout layout, List<LayoutRegion> regions) {
        Map<Long, List<LayoutRegionComponent>> byRegionId = Map.of();
        if (!regions.isEmpty()) {
            List<Long> regionIds = regions.stream().map(LayoutRegion::getId).toList();
            List<LayoutRegionComponent> all =
                    layoutRegionComponentRepository.findByRegion_IdIn(regionIds);
            byRegionId = all.stream().collect(Collectors.groupingBy(c -> c.getRegion().getId()));
            byRegionId.replaceAll((id, list) -> list.stream()
                    .sorted(Comparator.comparing(LayoutRegionComponent::getSortOrder).thenComparing(LayoutRegionComponent::getId))
                    .toList());
        }

        Map<Long, List<LayoutRegionComponent>> componentsByRegion = byRegionId;
        List<ActiveConfigResponse.RegionPayload> regionPayloads = regions.stream()
                .map(r -> {
                    List<ActiveConfigResponse.RegionComponentPayload> comps = componentsByRegion
                            .getOrDefault(r.getId(), List.of())
                            .stream()
                            .map(c -> ActiveConfigResponse.RegionComponentPayload.builder()
                                    .id(c.getId())
                                    .componentType(c.getComponentType())
                                    .configJson(c.getConfigJson())
                                    .sortOrder(c.getSortOrder())
                                    .build())
                            .toList();
                    return ActiveConfigResponse.RegionPayload.builder()
                            .id(r.getId())
                            .regionName(r.getRegionName())
                            .x(r.getX())
                            .y(r.getY())
                            .width(r.getWidth())
                            .height(r.getHeight())
                            .zIndex(r.getZIndex())
                            .components(comps)
                            .build();
                })
                .toList();
        return ActiveConfigResponse.LayoutPayload.builder()
                .id(layout.getId())
                .name(layout.getName())
                .templateType(layout.getTemplateType())
                .resolutionWidth(layout.getResolutionWidth())
                .resolutionHeight(layout.getResolutionHeight())
                .regions(regionPayloads)
                .build();
    }

    private ActiveConfigResponse.PlaylistPayload toPlaylistPayload(
            Long playlistId,
            String playlistName,
            List<PlaylistItem> items) {
        List<ActiveConfigResponse.ItemPayload> itemPayloads = items.stream()
                .map(pi -> {
                    Media media = pi.getMedia();
                    return ActiveConfigResponse.ItemPayload.builder()
                            .mediaId(media.getId())
                            .mediaType(media.getMediaType().name())
                            .name(media.getName())
                            .fileUrl(media.getFileUrl())
                            .thumbnailUrl(media.getThumbnailUrl())
                            .durationSeconds(pi.getDurationSeconds() != null ? pi.getDurationSeconds() : media.getDurationSeconds())
                            .orderIndex(pi.getOrderIndex())
                            .build();
                })
                .toList();
        return ActiveConfigResponse.PlaylistPayload.builder()
                .id(playlistId)
                .name(playlistName)
                .items(itemPayloads)
                .build();
    }
}
