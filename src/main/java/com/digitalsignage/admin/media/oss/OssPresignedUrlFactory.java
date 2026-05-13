package com.digitalsignage.admin.media.oss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.digitalsignage.admin.media.config.OssStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OssPresignedUrlFactory {

    private final OssStorageProperties ossProperties;

    /**
     * Presigned PUT URL for direct browser/client upload. Empty when OSS env is not fully configured.
     */
    public Optional<PresignedPut> buildPresignedPut(
            String objectKey, String contentType, Instant expiresAt) {
        if (!ossProperties.isConfigured()) {
            return Optional.empty();
        }

        ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
        clientConfig.setSignatureVersion(SignVersion.V4);

        Date expiration = Date.from(expiresAt);
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(ossProperties.getBucket(), objectKey, HttpMethod.PUT);
        request.setExpiration(expiration);
        if (StringUtils.hasText(contentType)) {
            request.setContentType(contentType.trim());
        }

        Map<String, String> requiredHeaders = new LinkedHashMap<>();
        if (StringUtils.hasText(contentType)) {
            requiredHeaders.put("Content-Type", contentType.trim());
        }

        OSS oss = OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .credentialsProvider(CredentialsProviderFactory.newDefaultCredentialProvider(
                        ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret()))
                .clientConfiguration(clientConfig)
                .region(ossProperties.getRegion())
                .build();
        try {
            URL url = oss.generatePresignedUrl(request);
            return Optional.of(new PresignedPut(url.toString(), requiredHeaders.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(requiredHeaders)));
        } finally {
            oss.shutdown();
        }
    }

    /**
     * Returns content length from OSS if configured and object exists; empty otherwise.
     */
    public Optional<Long> fetchContentLength(String objectKey) {
        if (!ossProperties.isConfigured()) {
            return Optional.empty();
        }
        ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
        clientConfig.setSignatureVersion(SignVersion.V4);
        OSS oss = OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .credentialsProvider(CredentialsProviderFactory.newDefaultCredentialProvider(
                        ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret()))
                .clientConfiguration(clientConfig)
                .region(ossProperties.getRegion())
                .build();
        try {
            ObjectMetadata meta = oss.getObjectMetadata(ossProperties.getBucket(), objectKey);
            return Optional.ofNullable(meta.getContentLength());
        } finally {
            oss.shutdown();
        }
    }

    public record PresignedPut(String uploadUrl, Map<String, String> requiredHeaders) {}
}
