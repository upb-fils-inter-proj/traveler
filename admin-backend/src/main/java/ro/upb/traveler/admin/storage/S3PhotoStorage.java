package ro.upb.traveler.admin.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Photos in an S3-compatible object storage. At start it checks the bucket and creates it when it
 * is missing. If the storage cannot be reached the application still starts: the photo endpoints
 * answer 503, and the bucket is checked again on the next photo request.
 */
@Component
@EnableConfigurationProperties(S3Properties.class)
class S3PhotoStorage implements PhotoStorage, ApplicationRunner, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(S3PhotoStorage.class);

  private final S3Client client;
  private final String bucket;
  private volatile boolean ready;

  S3PhotoStorage(S3Properties properties) {
    this.bucket = properties.bucket();
    if (!properties.isConfigured()) {
      this.client = null;
      return;
    }
    this.client =
        S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint()))
            .region(
                Region.of(
                    StringUtils.hasText(properties.region()) ? properties.region() : "us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
            .forcePathStyle(true)
            // Recent SDKs send CRC checksums that some S3-compatible servers reject.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(2))
                    .socketTimeout(Duration.ofSeconds(10)))
            .overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(20)))
            .build();
  }

  @Override
  public void run(ApplicationArguments args) {
    if (client == null) {
      log.warn("Photo storage is not configured (traveler.s3.*): the photo endpoints answer 503");
      return;
    }
    checkBucket();
  }

  @Override
  public void put(String key, byte[] webp) {
    call(() ->
        client.putObject(
            b -> b.bucket(bucket).key(key).contentType("image/webp"), RequestBody.fromBytes(webp)));
  }

  @Override
  public Optional<byte[]> get(String key) {
    return call(() -> {
      try {
        return Optional.of(client.getObjectAsBytes(b -> b.bucket(bucket).key(key)).asByteArray());
      } catch (S3Exception e) {
        if (e instanceof NoSuchKeyException || e.statusCode() == 404) {
          return Optional.empty();
        }
        throw e;
      }
    });
  }

  @Override
  public void delete(String key) {
    call(() -> client.deleteObject(b -> b.bucket(bucket).key(key)));
  }

  @Override
  public void destroy() {
    if (client != null) {
      client.close();
    }
  }

  /** Checks that the bucket exists and creates it when it does not. */
  private boolean checkBucket() {
    try {
      try {
        client.headBucket(b -> b.bucket(bucket));
      } catch (S3Exception e) {
        if (e.statusCode() != 404) {
          throw e;
        }
        client.createBucket(b -> b.bucket(bucket));
        log.info("Created the photo bucket {}", bucket);
      }
      ready = true;
    } catch (SdkException e) {
      log.warn("Photo storage cannot be reached, the photo endpoints answer 503: {}", e.toString());
      ready = false;
    }
    return ready;
  }

  private <T> T call(StorageCall<T> call) {
    if (client == null || (!ready && !checkBucket())) {
      throw new StorageUnavailableException(null);
    }
    try {
      return call.run();
    } catch (SdkException e) {
      if (e instanceof SdkClientException) {
        ready = false;
      }
      log.warn("Photo storage request failed: {}", e.toString());
      throw new StorageUnavailableException(e);
    }
  }

  @FunctionalInterface
  private interface StorageCall<T> {
    T run();
  }
}
