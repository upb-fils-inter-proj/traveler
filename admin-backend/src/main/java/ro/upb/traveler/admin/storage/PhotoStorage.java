package ro.upb.traveler.admin.storage;

import java.util.Optional;

/**
 * Object storage of the attraction photos (WebP). Every method throws {@link
 * StorageUnavailableException} while the storage is not configured or cannot be reached.
 */
public interface PhotoStorage {

  void put(String key, byte[] webp);

  /** The object, or empty when there is none under this key. */
  Optional<byte[]> get(String key);

  void delete(String key);
}
