package ro.upb.traveler.admin.security;

import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** A validated access token of an active staff member, with the role from staff_users. */
public class StaffAuthentication extends JwtAuthenticationToken {

  private final StaffUser staff;

  StaffAuthentication(Jwt jwt, StaffUser staff) {
    super(
        jwt,
        List.of(new SimpleGrantedAuthority("ROLE_" + staff.role().toUpperCase(Locale.ROOT))),
        jwt.getSubject());
    this.staff = staff;
  }

  public StaffUser getStaff() {
    return staff;
  }
}
