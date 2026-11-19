package ro.upb.traveler.admin.testsupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import ro.upb.traveler.admin.attraction.Attraction;
import ro.upb.traveler.admin.attraction.AttractionRepository;
import ro.upb.traveler.admin.attraction.AttractionStatus;

/** Attractions in memory, with one version counter like the database sequence. */
public class InMemoryAttractionRepository implements AttractionRepository {

  private final Map<UUID, Attraction> rows = new ConcurrentHashMap<>();
  private final AtomicLong versions = new AtomicLong();

  public void reset() {
    rows.clear();
    versions.set(0);
  }

  @Override
  public long nextVersion() {
    return versions.incrementAndGet();
  }

  @Override
  public Optional<Attraction> findById(UUID id) {
    return Optional.ofNullable(rows.get(id));
  }

  @Override
  public Optional<Attraction> findByIdForUpdate(UUID id) {
    return findById(id);
  }

  @Override
  public List<Attraction> findAll(boolean includeRemoved) {
    return rows.values().stream()
        .filter(a -> includeRemoved || a.status() != AttractionStatus.REMOVED)
        .toList();
  }

  @Override
  public void insert(Attraction attraction, UUID createdBy) {
    rows.put(attraction.id(), attraction);
  }

  @Override
  public void update(Attraction attraction) {
    rows.put(attraction.id(), attraction);
  }
}
