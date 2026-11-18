package ro.upb.traveler.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
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
 * section 4 of the DATA tab.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class AdminSchemaIT {

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
            "staff_users",
            "attractions",
            "attraction_translations",
            "danger_alerts",
            "partners",
            "rewards",
            "reward_assignments",
            "redemptions_log",
            "inbox_events",
            "stats_attraction_daily",
            "stats_reward_daily",
            "audit_log",
            "feature_flags",
            "shedlock",
            "scheduled_tasks");
  }

  @Test
  void staffEmailAndOidcIdentityAreUnique() {
    String subject = UUID.randomUUID().toString();
    String email = subject + "@example.ro";
    insertStaff(subject, email, "city_admin");

    assertThatThrownBy(() -> insertStaff(UUID.randomUUID().toString(), email, "partner"))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() -> insertStaff(subject, "other-" + email, "partner"))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void staffRoleIsOneOfTheThreeRoles() {
    insertStaff("partner");
    insertStaff("city_admin");
    insertStaff("traveler_admin");

    assertThatThrownBy(() -> insertStaff("tourist"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void anAttractionNeedsAPositiveValueAndAWarningWhenFlagged() {
    assertThatThrownBy(() -> insertAttraction(0, false, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertAttraction(10, true, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertAttraction(10, true, ""))
        .isInstanceOf(DataIntegrityViolationException.class);

    insertAttraction(10, true, "Rockfall on the path");
    insertAttraction(10, false, null);
  }

  @Test
  void anAttractionHasOneTranslationPerLanguageAndTheyGoWithIt() {
    UUID attraction = insertAttraction(10, false, null);
    insertTranslation(attraction, "ro");
    insertTranslation(attraction, "en");

    assertThatThrownBy(() -> insertTranslation(attraction, "en"))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() -> insertTranslation(attraction, "fr"))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbc.update("delete from attractions where id = ?", attraction);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from attraction_translations where attraction_id = ?",
                Integer.class,
                attraction))
        .isZero();
  }

  @Test
  void locationsAreStoredAsGeographyAndMeasuredInMeters() {
    UUID attraction = insertAttraction(10, false, null);

    // 0.0005 degrees of latitude is about 55.6 meters.
    Double meters =
        jdbc.queryForObject(
            "select ST_Distance(location, ST_SetSRID(ST_MakePoint(25.54273, 45.36059), 4326)::geography) "
                + "from attractions where id = ?",
            Double.class,
            attraction);

    assertThat(meters).isBetween(50.0, 60.0);
  }

  @Test
  void aRewardIsAssignedOncePerPartnerAndRemovalNeedsAReason() {
    UUID reward = insertReward();
    UUID partner = insertPartner();
    jdbc.update(
        "insert into reward_assignments (reward_id, partner_id) values (?, ?)", reward, partner);

    assertThatThrownBy(() ->
            jdbc.update(
                "insert into reward_assignments (reward_id, partner_id) values (?, ?)",
                reward,
                partner))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() -> removeAssignment(reward, partner, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> removeAssignment(reward, partner, "too short"))
        .isInstanceOf(DataIntegrityViolationException.class);

    removeAssignment(reward, partner, "Out of stock this week");
  }

  @Test
  void aRewardNeedsAPositiveCostAndValidity() {
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into rewards (title, description, point_cost, voucher_valid_days, version) "
                    + "values ('Bad', 'x', 0, 30, 1)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into rewards (title, description, point_cost, voucher_valid_days, version) "
                    + "values ('Bad', 'x', 10, 0, 1)"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void statisticsHaveOneRowPerDay() {
    UUID attraction = insertAttraction(10, false, null);
    UUID reward = insertReward();
    UUID partner = insertPartner();
    jdbc.update(
        "insert into stats_attraction_daily (attraction_id, day, claims) values (?, current_date, 3)",
        attraction);
    jdbc.update(
        "insert into stats_reward_daily (reward_id, partner_id, day, redemptions, points_spent) "
            + "values (?, ?, current_date, 1, 50)",
        reward,
        partner);

    assertThatThrownBy(() ->
            jdbc.update(
                "insert into stats_attraction_daily (attraction_id, day) values (?, current_date)",
                attraction))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() ->
            jdbc.update(
                "insert into stats_reward_daily (reward_id, partner_id, day) values (?, ?, current_date)",
                reward,
                partner))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void aBatchOfEventsIsCountedOnce() {
    UUID event = UUID.randomUUID();
    jdbc.update("insert into inbox_events (event_id, type) values (?, 'claim')", event);

    assertThatThrownBy(() ->
            jdbc.update("insert into inbox_events (event_id, type) values (?, 'claim')", event))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void auditRowsSurviveTheStaffMemberWhoMadeThem() {
    UUID staff = insertStaff("city_admin");
    String entity = UUID.randomUUID().toString();
    jdbc.update(
        "insert into audit_log (actor_staff_id, action, entity_type, entity_id) "
            + "values (?, 'attraction.flag', 'attraction', ?)",
        staff,
        entity);

    jdbc.update("delete from staff_users where id = ?", staff);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_id = ? and actor_staff_id is null",
                Integer.class,
                entity))
        .isEqualTo(1);
  }

  private UUID insertStaff(String role) {
    String subject = UUID.randomUUID().toString();
    return insertStaff(subject, subject + "@example.ro", role);
  }

  private UUID insertStaff(String subject, String email, String role) {
    return jdbc.queryForObject(
        "insert into staff_users (oidc_issuer, oidc_subject, email, role) "
            + "values ('https://auth.example/admin/', ?, ?, ?) returning id",
        UUID.class,
        subject,
        email,
        role);
  }

  private UUID insertAttraction(int pointValue, boolean dangerFlag, String dangerMessage) {
    return jdbc.queryForObject(
        "insert into attractions (location, point_value, version, danger_flag, danger_message) "
            + "values (ST_SetSRID(ST_MakePoint(25.54273, 45.36009), 4326)::geography, ?, 1, ?, ?) returning id",
        UUID.class,
        pointValue,
        dangerFlag,
        dangerMessage);
  }

  private void insertTranslation(UUID attraction, String lang) {
    jdbc.update(
        "insert into attraction_translations (attraction_id, lang, name, description) "
            + "values (?, ?, 'Peles Castle', 'A castle in Sinaia')",
        attraction,
        lang);
  }

  private UUID insertReward() {
    return jdbc.queryForObject(
        "insert into rewards (title, description, point_cost, voucher_valid_days, version) "
            + "values ('Free coffee', 'One coffee', 50, 30, 1) returning id",
        UUID.class);
  }

  private UUID insertPartner() {
    UUID staff = insertStaff("partner");
    return jdbc.queryForObject(
        "insert into partners (staff_user_id, store_name, address, location, contact_email) "
            + "values (?, 'Cafe', 'Strada Principala 1', "
            + "ST_SetSRID(ST_MakePoint(25.55, 45.35), 4326)::geography, 'cafe@example.ro') returning id",
        UUID.class,
        staff);
  }

  private void removeAssignment(UUID reward, UUID partner, String reason) {
    jdbc.update(
        "update reward_assignments set status = 'removed', removal_reason = ?, removed_at = now() "
            + "where reward_id = ? and partner_id = ?",
        reason,
        reward,
        partner);
  }
}
