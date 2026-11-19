package ro.upb.traveler.admin.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** S3-compatible object storage, reached with path-style requests. */
@ConfigurationProperties("traveler.s3")
public record S3Properties(
    String endpoint, String region, String bucket, String accessKey, String secretKey) {

  boolean isConfigured() {
    return StringUtils.hasText(endpoint)
        && StringUtils.hasText(bucket)
        && StringUtils.hasText(accessKey)
        && StringUtils.hasText(secretKey);
  }
}
