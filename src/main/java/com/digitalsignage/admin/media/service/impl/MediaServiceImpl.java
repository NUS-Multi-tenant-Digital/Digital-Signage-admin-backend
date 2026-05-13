package com.digitalsignage.admin.media.service.impl;

import com.digitalsignage.admin.common.enums.MediaType;
import com.digitalsignage.admin.common.exception.BusinessException;
import com.digitalsignage.admin.entity.Media;
import com.digitalsignage.admin.entity.Organization;
import com.digitalsignage.admin.media.config.MediaStorageProperties;
import com.digitalsignage.admin.media.dto.ConfirmMediaRequest;
import com.digitalsignage.admin.media.dto.MediaResponse;
import com.digitalsignage.admin.media.dto.UploadPolicyRequest;
import com.digitalsignage.admin.media.dto.UploadPolicyResponse;
import com.digitalsignage.admin.media.oss.OssPresignedUrlFactory;
import com.digitalsignage.admin.media.repository.MediaRepository;
import com.digitalsignage.admin.media.service.MediaService;
import com.digitalsignage.admin.playlist.repository.PlaylistItemRepository;
import com.digitalsignage.admin.security.AdminPrincipal;
import com.digitalsignage.admin.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final OrganizationRepository organizationRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final MediaStorageProperties storageProperties;
    private final OssPresignedUrlFactory ossPresignedUrlFactory;

    @Override
    @Transactional(readOnly = true)
    public UploadPolicyResponse uploadPolicy(UploadPolicyRequest request) {
        Long orgId = currentPrincipal().getOrganizationId();
        String objectKey = buildObjectKey(orgId, request.getMediaType(), request.getOriginalFilename());
        Instant expiresAt = Instant.now().plusSeconds(storageProperties.getUploadPolicyExpireMinutes() * 60L);

        Map<String, String> requiredHeaders = new LinkedHashMap<>();
        String uploadUrl = ossPresignedUrlFactory
                .buildPresignedPut(objectKey, request.getContentType(), expiresAt)
                .map(p -> {
                    requiredHeaders.putAll(p.requiredHeaders());
                    return p.uploadUrl();
                })
                .orElse(null);

        if (uploadUrl == null && StringUtils.hasText(storageProperties.getPresignedPutUrlTemplate())) {
            uploadUrl = storageProperties.getPresignedPutUrlTemplate().replace("{objectKey}", objectKey);
        }

        return UploadPolicyResponse.builder()
                .objectKey(objectKey)
                .uploadMethod(StringUtils.hasText(uploadUrl) ? "PUT" : "DEFERRED")
                .uploadUrl(uploadUrl)
                .expiresAt(expiresAt)
                .requiredHeaders(requiredHeaders.isEmpty()
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(requiredHeaders))
                .build();
    }

    @Override
    @Transactional
    public MediaResponse confirm(ConfirmMediaRequest request) {
        AdminPrincipal principal = currentPrincipal();
        Long orgId = principal.getOrganizationId();
        String objectKey = request.getObjectKey().trim();
        assertObjectKeyBelongsToOrg(objectKey, orgId);

        if (request.getFileSizeBytes() != null && request.getFileSizeBytes() > 0) {
            ossPresignedUrlFactory.fetchContentLength(objectKey).ifPresent(len -> {
                if (!Objects.equals(len, request.getFileSizeBytes())) {
                    throw new BusinessException(400, "object size mismatch with OSS");
                }
            });
        }

        if (mediaRepository.existsByOrganization_IdAndObjectKey(orgId, objectKey)) {
            throw new BusinessException(400, "media already exists for this object key");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(400, "organization not found"));

        String fileUrl = request.getFileUrl();
        if (!StringUtils.hasText(fileUrl) && StringUtils.hasText(storageProperties.getPublicBaseUrl())) {
            String base = storageProperties.getPublicBaseUrl().replaceAll("/+$", "");
            fileUrl = base + "/" + objectKey;
        }

        Media media = new Media();
        media.setOrganization(organization);
        media.setMediaType(request.getMediaType());
        media.setName(request.getName().trim());
        media.setObjectKey(objectKey);
        media.setFileUrl(StringUtils.hasText(fileUrl) ? fileUrl : null);
        media.setThumbnailUrl(StringUtils.hasText(request.getThumbnailUrl()) ? request.getThumbnailUrl() : null);
        media.setFileSizeBytes(request.getFileSizeBytes());
        media.setDurationSeconds(request.getDurationSeconds());
        media.setChecksumSha256(StringUtils.hasText(request.getChecksumSha256())
                ? request.getChecksumSha256().trim()
                : null);

        LocalDateTime now = LocalDateTime.now();
        media.setCreatedAt(now);
        media.setUpdatedAt(now);

        Media saved = mediaRepository.save(media);
        return MediaResponse.fromEntity(
                mediaRepository.findByIdAndOrganizationId(saved.getId(), orgId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> listMedia() {
        Long orgId = currentPrincipal().getOrganizationId();
        return mediaRepository.findAllByOrganizationId(orgId).stream()
                .map(MediaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMedia(Long id) {
        Long orgId = currentPrincipal().getOrganizationId();
        Media media = mediaRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new BusinessException(404, "media not found"));
        return MediaResponse.fromEntity(media);
    }

    @Override
    @Transactional
    public void deleteMedia(Long id) {
        Long orgId = currentPrincipal().getOrganizationId();
        Media media = mediaRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new BusinessException(404, "media not found"));
        if (playlistItemRepository.existsByMedia_Id(id)) {
            throw new BusinessException(400, "media is referenced by a playlist");
        }
        mediaRepository.delete(media);
    }

    private void assertObjectKeyBelongsToOrg(String objectKey, Long orgId) {
        if (objectKey.contains("..")) {
            throw new BusinessException(400, "invalid object key");
        }
        String prefix = orgId + "/";
        if (!objectKey.startsWith(prefix)) {
            throw new BusinessException(400, "object key does not belong to this organization");
        }
    }

    private String buildObjectKey(Long orgId, MediaType mediaType, String originalFilename) {
        String typeFolder = switch (mediaType) {
            case IMAGE -> "image";
            case VIDEO -> "video";
            case YOUTUBE -> "youtube";
        };
        String dateFolder = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        String ext = extractExtension(originalFilename, mediaType);
        String id = UUID.randomUUID().toString();
        return orgId + "/" + typeFolder + "/" + dateFolder + "/" + id + "." + ext;
    }

    private static String extractExtension(String originalFilename, MediaType mediaType) {
        String name = originalFilename == null ? "" : originalFilename.trim();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!ext.isEmpty() && ext.length() <= 16) {
                return ext;
            }
        }
        return switch (mediaType) {
            case VIDEO -> "mp4";
            case IMAGE -> "jpg";
            case YOUTUBE -> "txt";
        };
    }

    private AdminPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new BusinessException(401, "unauthorized");
        }
        return principal;
    }
}
