package ro.upb.traveler.tourist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs every migration on PostgreSQL 16 with PostGIS and checks the tables and constraints of
 * section 3 of the DATA tab.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class TouristSchemaIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private JdbcTemplate jdbc;

  @Test
  void everyTableOfTheDataTabExists() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);

    assertThat(tables)
        .contains(
            "app_users",
            "user_profiles",
            "device_tokens",
            "progress",
            "locations",
            "routes",
            "route_stops",
            "vouchers",
            "points_ledger",
            "outbox_events",
            "processed_requests",
            "data_exports",
            "feature_flags",
            "shedlock",
            "scheduled_tasks");
  }

  @Test
  void oneAccountPerOidcIdentity() {
    String subject = UUID.randomUUID().toString();
    insertUser(subject);

    assertThatThrownBy(() -> insertUser(subject)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void oneAccountPerPhoneNumber() {
    String phone = randomPhone();
    insertProfile(newUser(), phone);

    assertThatThrownBy(() -> insertProfile(newUser(), phone))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void pointsBalanceCannotBeNegative() {
    UUID user = newUser();
    insertProfile(user, randomPhone());

    assertThatThrownBy(() ->
            jdbc.update("update user_profiles set points_balance = -1 where user_id = ?", user))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void oneClaimPerUserAndAttraction() {
    UUID user = newUser();
    UUID attraction = UUID.randomUUID();
    insertClaim(user, attraction);

    assertThatThrownBy(() -> insertClaim(user, attraction))
        .isInstanceOf(DuplicateKeyException.class);
    insertClaim(user, UUID.randomUUID());
  }

  @Test
  void oneActiveRoutePerUser() {
    UUID user = newUser();
    insertRoute(user, "active");

    assertThatThrownBy(() -> insertRoute(user, "active")).isInstanceOf(DuplicateKeyException.class);
    insertRoute(user, "completed");
    insertRoute(user, "dismissed");
    insertRoute(newUser(), "active");
  }

  @Test
  void routeStopsAreNumberedFromOneToSeven() {
    UUID route = insertRoute(newUser(), "active");
    insertStop(route, 1);
    insertStop(route, 7);

    assertThatThrownBy(() -> insertStop(route, 1)).isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() -> insertStop(route, 8))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void positionsAreStoredAsGeographyAndMeasuredInMeters() {
    UUID user = newUser();
    jdbc.update(
        "insert into locations (user_id, position, accuracy_m, recorded_at) "
            + "values (?, ST_SetSRID(ST_MakePoint(25.54273, 45.36009), 4326)::geography, 4.5, now())",
        user);

    // 0.0005 degrees of latitude is about 55.6 meters.
    Double meters =
        jdbc.queryForObject(
            "select ST_Distance(position, ST_SetSRID(ST_MakePoint(25.54273, 45.36059), 4326)::geography) "
                + "from locations where user_id = ?",
            Double.class,
            user);

    assertThat(meters).isBetween(50.0, 60.0);
  }

  @Test
  void deletingAnAccountRemovesAllOfItsData() {
    UUID user = newUser();
    insertProfile(user, randomPhone());
    jdbc.update(
        "insert into device_tokens (user_id, platform, token) values (?, 'ios', ?)",
        user,
        UUID.randomUUID().toString());
    UUID route = insertRoute(user, "active");
    insertStop(route, 1);
    insertClaim(user, UUID.randomUUID());
    jdbc.update(
        "insert into locations (user_id, position, accuracy_m, recorded_at) "
            + "values (?, ST_SetSRID(ST_MakePoint(25.5, 45.3), 4326)::geography, 5, now())",
        user);
    jdbc.update(
        "insert into vouchers (user_id, reward_id, partner_id, token_hash, points_spent, issued_at, expires_at) "
            + "values (?, ?, ?, ?, 20, now(), now() + interval '30 days')",
        user,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID().toString().getBytes());
    jdbc.update("insert into points_ledger (user_id, delta, reason) values (?, 10, 'claim')", user);
    jdbc.update("insert into data_exports (user_id) values (?)", user);
    UUID survivor = newUser();
    insertClaim(survivor, UUID.randomUUID());

    jdbc.update("delete from app_users where id = ?", user);

    for (String table :
        List.of(
            "user_profiles",
            "device_tokens",
            "routes",
            "progress",
            "locations",
            "vouchers",
            "points_ledger",
            "data_exports")) {
      assertThat(count(table, user)).as(table).isZero();
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from route_stops where route_id = ?", Integer.class, route))
        .as("route_stops")
        .isZero();
    assertThat(count("progress", survivor)).as("other accounts are untouched").isEqualTo(1);
  }

  private int count(String table, UUID user) {
    return jdbc.queryForObject(
        "select count(*) from " + table + " where user_id = ?", Integer.class, user);
  }

  private UUID newUser() {
    return insertUser(UUID.randomUUID().toString());
  }

  private UUID insertUser(String subject) {
    return jdbc.queryForObject(
        "insert into app_users (oidc_issuer, oidc_subject) values ('https://auth.example/app/', ?) returning id",
        UUID.class,
        subject);
  }

  private void insertProfile(UUID user, String phone) {
    jdbc.update(
        "insert into user_profiles (user_id, first_name, last_name, phone_e164, phone_verified_at, age_confirmed_at) "
            + "values (?, 'Ana', 'Pop', ?, now(), now())",
        user,
        phone);
  }

  private void insertClaim(UUID user, UUID attraction) {
    jdbc.update(
        "insert into progress (user_id, attraction_id, points_awarded) values (?, ?, 10)",
        user,
        attraction);
  }

  private UUID insertRoute(UUID user, String status) {
    return jdbc.queryForObject(
        "insert into routes (user_id, target_minutes, status, route_date) values (?, 30, ?, current_date) returning id",
        UUID.class,
        user,
        status);
  }

  private void insertStop(UUID route, int position) {
    jdbc.update(
        "insert into route_stops (route_id, position, attraction_id, pinned_points, pinned_name) "
            + "values (?, ?, ?, 10, 'Peles Castle')",
        route,
        position,
        UUID.randomUUID());
  }

  private static String randomPhone() {
    return "+40" + ThreadLocalRandom.current().nextLong(700_000_000L, 800_000_000L);
  }
}
