package ro.upb.traveler.admin.security;

import java.util.UUID;

/** An active row of staff_users. The role is assigned by Traveler Administration. */
public record StaffUser(UUID id, String email, String role) {

  public static final String PARTNER = "partner";
  public static final String CITY_ADMIN = "city_admin";
  public static final String TRAVELER_ADMIN = "traveler_admin";
}
