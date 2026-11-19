package ro.upb.traveler.admin.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStaffRepository implements StaffRepository {

  private final JdbcClient jdbc;

  JdbcStaffRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<StaffUser> findActive(String issuer, String subject) {
    return jdbc.sql(
            """
            select id, email, role from staff_users
            where oidc_issuer = :issuer and oidc_subject = :subject and status = 'active'
            """)
        .param("issuer", issuer)
        .param("subject", subject)
        .query((rs, row) ->
            new StaffUser(
                rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("role")))
        .optional();
  }
}
