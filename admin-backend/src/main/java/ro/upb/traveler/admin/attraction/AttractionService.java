package ro.upb.traveler.admin.attraction;

import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;
import ro.upb.traveler.admin.audit.AuditLog;
import ro.upb.traveler.admin.security.StaffUser;
import ro.upb.traveler.admin.storage.PhotoStorage;
import ro.upb.traveler.admin.web.ApiErrors;

/**
 * Changes to attractions. Every change takes the next global version and writes one audit row in
 * the same transaction.
 */
@Service
public class AttractionService {

  static final int DEFAULT_VISIT_MINUTES = 15;
  static final int DEFAULT_DANGER_RADIUS_M = 100;
  static final String ENTITY_TYPE = "attraction";

  private static final Logger log = LoggerFactory.getLogger(AttractionService.class);

  private final AttractionRepository attractions;
  private final AuditLog audit;
  private final PhotoStorage photos;
  private final TransactionOperations tx;

  public AttractionService(
      AttractionRepository attractions,
      AuditLog audit,
      PhotoStorage photos,
      TransactionOperations tx) {
    this.attractions = attractions;
    this.audit = audit;
    this.photos = photos;
    this.tx = tx;
  }

  /** Sorted by Romanian name. */
  public List<Attraction> list(boolean includeRemoved) {
    Collator romanian = Collator.getInstance(Locale.forLanguageTag("ro"));
    return attractions.findAll(includeRemoved).stream()
        .sorted(
            Comparator.comparing((Attraction a) -> a.ro().name(), romanian)
                .thenComparing(Attraction::id))
        .toList();
  }

  public Attraction get(UUID id) {
    return attractions.findById(id).orElseThrow(() -> notFound(id));
  }

  public Attraction create(AttractionInput input, StaffUser actor) {
    return tx.execute(status -> {
      Attraction created =
          new Attraction(
              UUID.randomUUID(),
              AttractionStatus.OPEN,
              location(input),
              input.pointValue(),
              Objects.requireNonNullElse(input.visitMinutes(), DEFAULT_VISIT_MINUTES),
              DEFAULT_DANGER_RADIUS_M,
              false,
              null,
              1,
              null,
              attractions.nextVersion(),
              text(input.translations().ro()),
              text(input.translations().en()));
      attractions.insert(created, actor.id());
      audit.record(
          actor.id(), "attraction.create", ENTITY_TYPE, created.id().toString(), fields(created));
      return created;
    });
  }

  /** Visit minutes stay as they are when the input leaves them out. */
  public Attraction update(UUID id, AttractionInput input, StaffUser actor) {
    return change(
        id,
        "attraction.update",
        actor,
        true,
        current ->
            current.withContent(
                location(input),
                input.pointValue(),
                Objects.requireNonNullElse(input.visitMinutes(), current.visitMinutes()),
                text(input.translations().ro()),
                text(input.translations().en())));
  }

  /** Removed is final: a removed attraction cannot change its status again. */
  public Attraction changeStatus(UUID id, AttractionStatus status, StaffUser actor) {
    return change(id, "attraction.status", actor, true, current -> current.withStatus(status));
  }

  /**
   * Stores the photo under a new key, then points the attraction at it. The previous photo is
   * deleted after the commit.
   */
  public Attraction replacePhoto(UUID id, byte[] webp, StaffUser actor) {
    get(id);
    String key = "attractions/" + id + "/" + UUID.randomUUID() + ".webp";
    photos.put(key, webp);
    AtomicReference<String> previous = new AtomicReference<>();
    Attraction updated;
    try {
      updated =
          change(
              id,
              "attraction.photo",
              actor,
              false,
              current -> {
                previous.set(current.photoKey());
                return current.withPhotoKey(key);
              });
    } catch (RuntimeException e) {
      deleteQuietly(key);
      throw e;
    }
    if (previous.get() != null) {
      deleteQuietly(previous.get());
    }
    return updated;
  }

  public byte[] photo(UUID id) {
    Attraction attraction = get(id);
    if (attraction.photoKey() == null) {
      throw ApiErrors.notFound("The attraction has no photo.");
    }
    return photos
        .get(attraction.photoKey())
        .orElseThrow(() -> ApiErrors.notFound("The attraction has no photo."));
  }

  private Attraction change(
      UUID id,
      String action,
      StaffUser actor,
      boolean refuseRemoved,
      UnaryOperator<Attraction> edit) {
    return tx.execute(status -> {
      Attraction current = attractions.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
      if (refuseRemoved && current.status() == AttractionStatus.REMOVED) {
        throw ApiErrors.conflict("The attraction is removed; removed is final.");
      }
      Attraction updated = edit.apply(current).withVersion(attractions.nextVersion());
      attractions.update(updated);
      audit.record(actor.id(), action, ENTITY_TYPE, id.toString(), changes(current, updated));
      return updated;
    });
  }

  private void deleteQuietly(String key) {
    try {
      photos.delete(key);
    } catch (RuntimeException e) {
      log.warn("Could not delete the photo {}: {}", key, e.toString());
    }
  }

  private static GeoPoint location(AttractionInput input) {
    return new GeoPoint(input.location().lat(), input.location().lon());
  }

  private static Translation text(AttractionInput.Text text) {
    return new Translation(
        text.name(), text.description(), blankToNull(text.history()), blankToNull(text.sources()));
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value : null;
  }

  private static RuntimeException notFound(UUID id) {
    return ApiErrors.notFound("No attraction with id " + id + ".");
  }

  /** The audited fields of an attraction (never the photo itself, only its key). */
  static Map<String, Object> fields(Attraction a) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("status", a.status().value());
    fields.put("location.lat", a.location().lat());
    fields.put("location.lon", a.location().lon());
    fields.put("pointValue", a.pointValue());
    fields.put("visitMinutes", a.visitMinutes());
    fields.put("qrVersion", a.qrVersion());
    fields.put("photoKey", a.photoKey());
    putTranslation(fields, "ro", a.ro());
    putTranslation(fields, "en", a.en());
    return fields;
  }

  /** The fields that differ, as {"field": {"from": old, "to": new}}. */
  static Map<String, Object> changes(Attraction before, Attraction after) {
    Map<String, Object> old = fields(before);
    Map<String, Object> changes = new LinkedHashMap<>();
    fields(after)
        .forEach((field, value) -> {
          Object previous = old.get(field);
          if (!Objects.equals(previous, value)) {
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("from", previous);
            change.put("to", value);
            changes.put(field, change);
          }
        });
    return changes;
  }

  private static void putTranslation(Map<String, Object> fields, String lang, Translation t) {
    String prefix = "translations." + lang + ".";
    fields.put(prefix + "name", t.name());
    fields.put(prefix + "description", t.description());
    fields.put(prefix + "history", t.history());
    fields.put(prefix + "sources", t.sources());
  }
}
