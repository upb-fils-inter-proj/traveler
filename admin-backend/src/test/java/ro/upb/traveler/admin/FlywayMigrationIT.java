package ro.upb.traveler.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Starts PostgreSQL 16 with PostGIS in Docker and lets Flyway run all migrations on it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class FlywayMigrationIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private JdbcTemplate jdbc;

  @Test
  void allMigrationsAreApplied() {
    Integer failed =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where not success", Integer.class);
    Integer applied =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success", Integer.class);

    assertThat(failed).isZero();
    assertThat(applied).isPositive();
  }

  @Test
  void postgisIsEnabled() {
    String version = jdbc.queryForObject("select postgis_version()", String.class);

    assertThat(version).isNotBlank();
  }
}
