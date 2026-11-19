package ro.upb.traveler.admin.testsupport;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ro.upb.traveler.admin.security.StaffRepository;
import ro.upb.traveler.admin.security.StaffUser;

/** One active staff member per role; any other subject has no staff row. */
public class InMemoryStaffRepository implements StaffRepository {

  public static final String ISSUER = "https://auth.test/application/o/traveler-admin/";

  public static final StaffUser CITY_ADMIN =
      new StaffUser(UUID.randomUUID(), "city@example.ro", StaffUser.CITY_ADMIN);
  public static final StaffUser TRAVELER_ADMIN =
      new StaffUser(UUID.randomUUID(), "admin@example.ro", StaffUser.TRAVELER_ADMIN);
  public static final StaffUser PARTNER =
      new StaffUser(UUID.randomUUID(), "partner@example.ro", StaffUser.PARTNER);

  private static final Map<String, StaffUser> BY_SUBJECT =
      Map.of(
          "city-admin-subject", CITY_ADMIN,
          "traveler-admin-subject", TRAVELER_ADMIN,
          "partner-subject", PARTNER);

  @Override
  public Optional<StaffUser> findActive(String issuer, String subject) {
    return ISSUER.equals(issuer) ? Optional.ofNullable(BY_SUBJECT.get(subject)) : Optional.empty();
  }
}
