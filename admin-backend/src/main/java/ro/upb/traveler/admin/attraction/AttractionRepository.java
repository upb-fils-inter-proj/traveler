package ro.upb.traveler.admin.attraction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Attractions and their translations. The write methods run inside the caller's transaction. */
public interface AttractionRepository {

  /**
   * The next value of the global version sequence. Changes are serialized until the transaction
   * ends, so versions become visible in increasing order and "changed since" queries miss nothing.
   */
  long nextVersion();

  Optional<Attraction> findById(UUID id);

  /** Like {@link #findById} and locks the attraction until the transaction ends. */
  Optional<Attraction> findByIdForUpdate(UUID id);

  List<Attraction> findAll(boolean includeRemoved);

  void insert(Attraction attraction, UUID createdBy);

  /** Writes every field of the attraction and both translations. */
  void update(Attraction attraction);
}
