package ro.upb.traveler.admin.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/** The application keeps starting without object storage; photo requests then fail with 503. */
class S3PhotoStorageTest {

  @Test
  void startsAndAnswersUnavailableWhenTheStorageCannotBeReached() {
    S3PhotoStorage storage =
        new S3PhotoStorage(
            new S3Properties("http://127.0.0.1:1", "us-east-1", "photos", "key", "secret"));

    assertThatCode(() -> storage.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    assertThatThrownBy(() -> storage.put("attractions/a/b.webp", new byte[] {1}))
        .isInstanceOf(StorageUnavailableException.class);
    storage.destroy();
  }

  @Test
  void startsAndAnswersUnavailableWhenTheStorageIsNotConfigured() {
    S3PhotoStorage storage = new S3PhotoStorage(new S3Properties("", "", "", "", ""));

    assertThatCode(() -> storage.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    assertThatThrownBy(() -> storage.get("attractions/a/b.webp"))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
