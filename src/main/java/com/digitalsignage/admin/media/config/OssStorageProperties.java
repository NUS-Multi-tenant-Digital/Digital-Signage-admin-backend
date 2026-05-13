package com.digitalsignage.admin.media.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Aliyun OSS (phase-1: RAM user AK/SK, presigned PUT). Leave fields empty to disable SDK signing.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.storage.oss")
public class OssStorageProperties {

    /**
     * e.g. https://oss-ap-southeast-1.aliyuncs.com
     */
    private String endpoint = "";

    private String bucket = "";

    /**
     * e.g. ap-southeast-1 (used for V4 signing where required)
     */
    private String region = "";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    /**
     * Region is required for V4 signing (e.g. ap-southeast-1).
     */
    public boolean isConfigured() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(bucket)
                && StringUtils.hasText(region)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret);
    }
}
