package ro.upb.traveler.admin.attraction;

import java.util.List;
import java.util.UUID;

/** An attraction as the admin web app sees it. */
public record AttractionResponse(
    UUID id,
    AttractionStatus status,
    GeoPoint location,
    int pointValue,
    int visitMinutes,
    int dangerRadiusM,
    boolean dangerFlag,
    String dangerMessage,
    int qrVersion,
    boolean hasPhoto,
    long version,
    Translations translations) {

  public record Translations(Translation ro, Translation en) {}

  public record Page(List<AttractionResponse> items) {}

  static AttractionResponse of(Attraction a) {
    return new AttractionResponse(
        a.id(),
        a.status(),
        a.location(),
        a.pointValue(),
        a.visitMinutes(),
        a.dangerRadiusM(),
        a.dangerFlag(),
        a.dangerMessage(),
        a.qrVersion(),
        a.photoKey() != null,
        a.version(),
        new Translations(a.ro(), a.en()));
  }
}
