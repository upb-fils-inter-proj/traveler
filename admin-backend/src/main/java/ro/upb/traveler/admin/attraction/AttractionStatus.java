package ro.upb.traveler.admin.attraction;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** Removed attractions are kept for history but hidden; removed is final. */
public enum AttractionStatus {
  OPEN,
  CLOSED,
  REMOVED;

  /** The value stored in the database and used in JSON: open, closed or removed. */
  @JsonValue
  public String value() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static AttractionStatus of(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
