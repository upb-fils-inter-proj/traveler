package ro.upb.traveler.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ro.upb.traveler.admin.attraction.Attraction;
import ro.upb.traveler.admin.attraction.AttractionInput;
import ro.upb.traveler.admin.attraction.AttractionRepository;
import ro.upb.traveler.admin.attraction.AttractionService;
import ro.upb.traveler.admin.attraction.AttractionStatus;
import ro.upb.traveler.admin.attraction.ChangedAttractions;
import ro.upb.traveler.admin.security.StaffRepository;
import ro.upb.traveler.admin.security.StaffUser;

/**
 * The SQL of the repositories on PostgreSQL 16 with PostGIS: attractions and translations through
 * the service (real transactions), the version sequence, the staff lookup and the audit rows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class AttractionRepositoryIT {

  private static final String ISSUER = "https://auth.test/application/o/traveler-admin/";

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private AttractionService service;
  @Autowired private AttractionRepository attractions;
  @Autowired private StaffRepository staff;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper json;

  @Test
  void createStoresTheAttractionItsTranslationsAndAnAuditRow() throws Exception {
    StaffUser admin = insertStaff("city_admin", "active");

    Attraction created = service.create(input("Castelul Peleș", 10, "Istoric"), admin);
    Attraction stored = attractions.findById(created.id()).orElseThrow();

    assertThat(stored).isEqualTo(created);
    assertThat(stored.location().lat()).isEqualTo(45.36009);
    assertThat(stored.location().lon()).isEqualTo(25.54273);
    assertThat(stored.ro().name()).isEqualTo("Castelul Peleș");
    assertThat(stored.ro().history()).isEqualTo("Istoric");
    assertThat(stored.en().name()).isEqualTo("Peleș Castle");
    assertThat(stored.visitMinutes()).isEqualTo(15);
    assertThat(stored.dangerRadiusM()).isEqualTo(100);
    assertThat(
            jdbc.queryForObject(
                "select created_by from attractions where id = ?", UUID.class, created.id()))
        .isEqualTo(admin.id());

    JsonNode details = auditDetails(created.id(), "attraction.create", admin.id());
    assertThat(details.get("pointValue").asInt()).isEqualTo(10);
    assertThat(details.get("translations.ro.name").asText()).isEqualTo("Castelul Peleș");
  }

  @Test
  void updateTakesTheNextVersionAndRewritesTheTranslations() throws Exception {
    StaffUser admin = insertStaff("traveler_admin", "active");
    Attraction created = service.create(input("Castelul Peleș", 10, "Istoric"), admin);

    Attraction updated = service.update(created.id(), input("Castelul Pelișor", 20, null), admin);
    Attraction stored = attractions.findById(created.id()).orElseThrow();

    assertThat(updated.version()).isGreaterThan(created.version());
    assertThat(stored).isEqualTo(updated);
    assertThat(stored.pointValue()).isEqualTo(20);
    assertThat(stored.ro().name()).isEqualTo("Castelul Pelișor");
    assertThat(stored.ro().history()).isNull();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from attraction_translations where attraction_id = ?",
                Integer.class,
                created.id()))
        .isEqualTo(2);

    JsonNode details = auditDetails(created.id(), "attraction.update", admin.id());
    assertThat(details.get("pointValue").get("from").asInt()).isEqualTo(10);
    assertThat(details.get("pointValue").get("to").asInt()).isEqualTo(20);
  }

  @Test
  void theListHidesRemovedAttractionsUnlessAsked() {
    StaffUser admin = insertStaff("city_admin", "active");
    Attraction open = service.create(input("Bran", 10, null), admin);
    Attraction removed = service.create(input("Cetatea Rupea", 10, null), admin);
    service.changeStatus(removed.id(), AttractionStatus.REMOVED, admin);

    List<UUID> visible = attractions.findAll(false).stream().map(Attraction::id).toList();
    List<UUID> all = attractions.findAll(true).stream().map(Attraction::id).toList();

    assertThat(visible).contains(open.id()).doesNotContain(removed.id());
    assertThat(all).contains(open.id(), removed.id());
    assertThat(attractions.findById(removed.id()).orElseThrow().status())
        .isEqualTo(AttractionStatus.REMOVED);
  }

  @Test
  void changedSinceReturnsNewerVersionsAndTheHighestVersion() {
    StaffUser admin = insertStaff("city_admin", "active");
    Attraction first = service.create(input("Bran", 10, null), admin);
    Attraction second = service.create(input("Sinaia", 10, null), admin);
    Attraction closed = service.changeStatus(first.id(), AttractionStatus.CLOSED, admin);

    ChangedAttractions changed = attractions.findChangedSince(first.version());

    assertThat(changed.attractions())
        .extracting(Attraction::id)
        .containsExactly(second.id(), first.id());
    assertThat(changed.attractions().get(1).status()).isEqualTo(AttractionStatus.CLOSED);
    assertThat(changed.version()).isEqualTo(closed.version());
    assertThat(attractions.findChangedSince(closed.version()).attractions()).isEmpty();
  }

  @Test
  void versionsComeFromOneIncreasingSequence() {
    long first = attractions.nextVersion();
    long second = attractions.nextVersion();

    assertThat(second).isGreaterThan(first);
  }

  @Test
  void staffAreFoundByIssuerAndSubjectOnlyWhileActive() {
    StaffUser active = insertStaff("partner", "active");
    StaffUser deactivated = insertStaff("city_admin", "deactivated");

    assertThat(staff.findActive(ISSUER, subjectOf(active))).contains(active);
    assertThat(staff.findActive("https://auth.test/other/", subjectOf(active))).isEmpty();
    assertThat(staff.findActive(ISSUER, subjectOf(deactivated))).isEmpty();
  }

  private JsonNode auditDetails(UUID attraction, String action, UUID actor) throws Exception {
    String details =
        jdbc.queryForObject(
            "select details::text from audit_log "
                + "where entity_type = 'attraction' and entity_id = ? and action = ? "
                + "and actor_staff_id = ?",
            String.class,
            attraction.toString(),
            action,
            actor);
    return json.readTree(details);
  }

  /** Inserts a staff row whose OIDC subject is its email's local part. */
  private StaffUser insertStaff(String role, String status) {
    String subject = UUID.randomUUID().toString();
    String email = subject + "@example.ro";
    UUID id =
        jdbc.queryForObject(
            "insert into staff_users (oidc_issuer, oidc_subject, email, role, status) "
                + "values (?, ?, ?, ?, ?) returning id",
            UUID.class,
            ISSUER,
            subject,
            email,
            role,
            status);
    return new StaffUser(id, email, role);
  }

  private static String subjectOf(StaffUser staffUser) {
    return staffUser.email().substring(0, staffUser.email().indexOf('@'));
  }

  private static AttractionInput input(String roName, int pointValue, String history) {
    return new AttractionInput(
        new AttractionInput.Location(45.36009, 25.54273),
        pointValue,
        null,
        new AttractionInput.Translations(
            new AttractionInput.Text(roName, "Castel în Sinaia", history, "Două surse"),
            new AttractionInput.Text("Peleș Castle", "A castle in Sinaia", null, null)));
  }
}
