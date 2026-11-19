package ro.upb.traveler.admin.testsupport;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ro.upb.traveler.admin.storage.PhotoStorage;
import ro.upb.traveler.admin.storage.StorageUnavailableException;

/** Photos in memory; can be switched to unavailable like an unreachable object storage. */
public class FakePhotoStorage implements PhotoStorage {

  private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
  private volatile boolean available = true;

  public void reset() {
    objects.clear();
    available = true;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public Set<String> keys() {
    return Set.copyOf(objects.keySet());
  }

  @Override
  public void put(String key, byte[] webp) {
    check();
    objects.put(key, webp.clone());
  }

  @Override
  public Optional<byte[]> get(String key) {
    check();
    return Optional.ofNullable(objects.get(key));
  }

  @Override
  public void delete(String key) {
    check();
    objects.remove(key);
  }

  private void check() {
    if (!available) {
      throw new StorageUnavailableException(null);
    }
  }
}
