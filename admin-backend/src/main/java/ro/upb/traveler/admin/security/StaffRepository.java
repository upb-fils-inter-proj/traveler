package ro.upb.traveler.admin.security;

import java.util.Optional;

/** Staff accounts, found by the identity that authentik puts in the access token. */
public interface StaffRepository {

  /** The staff member with this OIDC issuer and subject, if the account exists and is active. */
  Optional<StaffUser> findActive(String issuer, String subject);
}
