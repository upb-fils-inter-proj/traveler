package ro.upb.traveler.admin.attraction;

import java.util.UUID;

/**
 * An attraction with its Romanian and English texts. {@code version} comes from one sequence for
 * all attractions and changes with every change.
 */
public record Attraction(
    UUID id,
    AttractionStatus status,
    GeoPoint location,
    int pointValue,
    int visitMinutes,
    int dangerRadiusM,
    boolean dangerFlag,
    String dangerMessage,
    int qrVersion,
    String photoKey,
    long version,
    Translation ro,
    Translation en) {

  Attraction withContent(
      GeoPoint location, int pointValue, int visitMinutes, Translation ro, Translation en) {
    return new Attraction(
        id,
        status,
        location,
        pointValue,
        visitMinutes,
        dangerRadiusM,
        dangerFlag,
        dangerMessage,
        qrVersion,
        photoKey,
        version,
        ro,
        en);
  }

  Attraction withStatus(AttractionStatus status) {
    return new Attraction(
        id,
        status,
        location,
        pointValue,
        visitMinutes,
        dangerRadiusM,
        dangerFlag,
        dangerMessage,
        qrVersion,
        photoKey,
        version,
        ro,
        en);
  }

  Attraction withPhotoKey(String photoKey) {
    return new Attraction(
        id,
        status,
        location,
        pointValue,
        visitMinutes,
        dangerRadiusM,
        dangerFlag,
        dangerMessage,
        qrVersion,
        photoKey,
        version,
        ro,
        en);
  }

  Attraction withQrVersion(int qrVersion) {
    return new Attraction(
        id,
        status,
        location,
        pointValue,
        visitMinutes,
        dangerRadiusM,
        dangerFlag,
        dangerMessage,
        qrVersion,
        photoKey,
        version,
        ro,
        en);
  }

  Attraction withVersion(long version) {
    return new Attraction(
        id,
        status,
        location,
        pointValue,
        visitMinutes,
        dangerRadiusM,
        dangerFlag,
        dangerMessage,
        qrVersion,
        photoKey,
        version,
        ro,
        en);
  }
}
