package ro.upb.traveler.admin.attraction;

import java.util.List;

/**
 * The attractions changed since a version, and the highest version of all attractions (0 when there
 * are none), read from one snapshot.
 */
public record ChangedAttractions(long version, List<Attraction> attractions) {}
