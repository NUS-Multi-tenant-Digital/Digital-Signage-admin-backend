package com.digitalsignage.admin.playlist.service.impl;

import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.entity.Playlist;
import com.digitalsignage.admin.entity.PlaylistItem;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.playlist.dto.CreatePlaylistRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistItemRequest;
import com.digitalsignage.admin.playlist.dto.PlaylistItemResponse;
import com.digitalsignage.admin.playlist.dto.PlaylistResponse;
import com.digitalsignage.admin.playlist.dto.ReorderPlaylistItemsRequest;
import com.digitalsignage.admin.playlist.dto.UpdatePlaylistRequest;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.playlist.repository.PlaylistRepository;
import com.digitalsignage.admin.playlist.service.PlaylistService;
import com.digitalsignage.admin.schedule.repository.ScheduleRepository;
import com.digitalsignage.admin.security.SecurityUtils;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import com.digitalsignage.admin.websocket.ConfigPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final MediaRepository mediaRepository;
    private final ScheduleRepository scheduleRepository;
    private final OrganizationRepository organizationRepository;
    private final ConfigPushService configPushService;

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistResponse> listPlaylists() {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        return playlistRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId).stream()
                .map(p -> toResponse(p.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        playlistRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        return toResponse(id);
    }

    @Override
    @Transactional
    public PlaylistResponse createPlaylist(CreatePlaylistRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));
        Playlist playlist = new Playlist();
        playlist.setOrganization(organization);
        playlist.setName(request.getName().trim());
        playlist.setStatus(request.getStatus());
        Playlist saved = playlistRepository.save(playlist);
        saveItems(saved, organizationId, request.getItems());
        configPushService.notifyPlaylistChanged(saved.getId());
        return toResponse(saved.getId());
    }

    @Override
    @Transactional
    public PlaylistResponse updatePlaylist(Long id, UpdatePlaylistRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Playlist playlist = playlistRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        playlist.setName(request.getName().trim());
        playlist.setStatus(request.getStatus());
        playlistRepository.save(playlist);
        playlistItemRepository.deleteByPlaylist_Id(playlist.getId());
        saveItems(playlist, organizationId, request.getItems());
        configPushService.notifyPlaylistChanged(playlist.getId());
        return toResponse(playlist.getId());
    }

    @Override
    @Transactional
    public void deletePlaylist(Long id) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Playlist playlist = playlistRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        if (scheduleRepository.existsByPlaylist_Id(playlist.getId())) {
            throw new BusinessException(409, "playlist is referenced by schedules");
        }
        playlistItemRepository.deleteByPlaylist_Id(playlist.getId());
        playlistRepository.delete(playlist);
    }

    @Override
    @Transactional
    public PlaylistResponse reorderItems(Long id, ReorderPlaylistItemsRequest request) {
        Long organizationId = SecurityUtils.requireAdmin().getOrganizationId();
        Playlist playlist = playlistRepository.findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new BusinessException(404, "playlist not found"));
        List<PlaylistItem> existing = playlistItemRepository.findByPlaylist_IdOrderByOrderIndexAsc(playlist.getId());
        if (existing.size() != request.getItemIdsInOrder().size()) {
            throw new BusinessException(400, "item count mismatch");
        }
        Set<Long> ids = new HashSet<>(request.getItemIdsInOrder());
        if (ids.size() != request.getItemIdsInOrder().size()) {
            throw new BusinessException(400, "duplicate item ids");
        }
        for (PlaylistItem item : existing) {
            if (!ids.contains(item.getId())) {
                throw new BusinessException(400, "unknown playlist item id");
            }
        }
        int index = 0;
        for (Long itemId : request.getItemIdsInOrder()) {
            PlaylistItem item = existing.stream()
                    .filter(pi -> Objects.equals(pi.getId(), itemId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(400, "invalid item id"));
            item.setOrderIndex(index++);
            playlistItemRepository.save(item);
        }
        configPushService.notifyPlaylistChanged(playlist.getId());
        return toResponse(playlist.getId());
    }

    private void saveItems(Playlist playlist, Long organizationId, List<PlaylistItemRequest> items) {
        for (int i = 0; i < items.size(); i++) {
            PlaylistItemRequest ir = items.get(i);
            Media media = resolveMedia(organizationId, ir.getMediaId());
            PlaylistItem pi = new PlaylistItem();
            pi.setPlaylist(playlist);
            pi.setMedia(media);
            pi.setOrderIndex(ir.getOrderIndex() != null ? ir.getOrderIndex() : i);
            pi.setDurationSeconds(ir.getDurationSeconds());
            playlistItemRepository.save(pi);
        }
    }

    private Media resolveMedia(Long organizationId, Long mediaId) {
        return mediaRepository.findByIdAndOrganization_Id(mediaId, organizationId)
                .orElseThrow(() -> new BusinessException(400, "media not found"));
    }

    private PlaylistResponse toResponse(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow();
        List<PlaylistItem> items = playlistItemRepository.findWithMediaByPlaylist_Id(playlistId);
        List<PlaylistItemResponse> itemResponses = items.stream()
                .map(PlaylistItemResponse::fromEntity)
                .toList();
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .status(playlist.getStatus().name())
                .items(itemResponses)
                .build();
    }
}
