package ro.upb.traveler.admin.attraction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcAttractionRepository implements AttractionRepository {

  private static final String SELECT =
      """
      select a.id, a.status, ST_Y(a.location::geometry) as lat, ST_X(a.location::geometry) as lon,
             a.point_value, a.visit_minutes, a.danger_radius_m, a.danger_flag, a.danger_message,
             a.qr_version, a.photo_key, a.version,
             ro.name as ro_name, ro.description as ro_description,
             ro.history as ro_history, ro.sources as ro_sources,
             en.name as en_name, en.description as en_description,
             en.history as en_history, en.sources as en_sources
      from attractions a
      join attraction_translations ro on ro.attraction_id = a.id and ro.lang = 'ro'
      join attraction_translations en on en.attraction_id = a.id and en.lang = 'en'
      """;

  private static final String INSERT =
      """
      insert into attractions (id, location, danger_radius_m, photo_key, point_value,
                               visit_minutes, status, danger_flag, danger_message,
                               qr_version, version, created_by)
      values (:id, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :dangerRadiusM,
              :photoKey, :pointValue, :visitMinutes, :status, :dangerFlag, :dangerMessage,
              :qrVersion, :version, :createdBy)
      """;

  private static final String UPDATE =
      """
      update attractions
      set location = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
          danger_radius_m = :dangerRadiusM, photo_key = :photoKey,
          point_value = :pointValue, visit_minutes = :visitMinutes, status = :status,
          danger_flag = :dangerFlag, danger_message = :dangerMessage,
          qr_version = :qrVersion, version = :version
      where id = :id
      """;

  private final JdbcClient jdbc;

  JdbcAttractionRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public long nextVersion() {
    // Held until the transaction ends.
    jdbc.sql("select pg_advisory_xact_lock(hashtext('attraction_version_seq'))")
        .query((rs, row) -> true)
        .list();
    return jdbc.sql("select nextval('attraction_version_seq')").query(Long.class).single();
  }

  @Override
  public Optional<Attraction> findById(UUID id) {
    return jdbc.sql(SELECT + " where a.id = :id").param("id", id).query(this::map).optional();
  }

  @Override
  public Optional<Attraction> findByIdForUpdate(UUID id) {
    return jdbc.sql(SELECT + " where a.id = :id for update of a")
        .param("id", id)
        .query(this::map)
        .optional();
  }

  @Override
  public List<Attraction> findAll(boolean includeRemoved) {
    return jdbc.sql(
            SELECT + " where :includeRemoved or a.status <> 'removed' order by ro.name, a.id")
        .param("includeRemoved", includeRemoved)
        .query(this::map)
        .list();
  }

  /** Both queries see the same snapshot, so the version matches the attractions returned. */
  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ChangedAttractions findChangedSince(long since) {
    long version =
        jdbc.sql("select coalesce(max(version), 0) from attractions").query(Long.class).single();
    List<Attraction> changed =
        jdbc.sql(SELECT + " where a.version > :since order by a.version")
            .param("since", since)
            .query(this::map)
            .list();
    return new ChangedAttractions(version, changed);
  }

  @Override
  public void insert(Attraction attraction, UUID createdBy) {
    bind(jdbc.sql(INSERT), attraction).param("createdBy", createdBy).update();
    saveTranslations(attraction);
  }

  @Override
  public void update(Attraction attraction) {
    bind(jdbc.sql(UPDATE), attraction).update();
    saveTranslations(attraction);
  }

  private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, Attraction a) {
    return spec.param("id", a.id())
        .param("lon", a.location().lon())
        .param("lat", a.location().lat())
        .param("dangerRadiusM", a.dangerRadiusM())
        .param("photoKey", a.photoKey())
        .param("pointValue", a.pointValue())
        .param("visitMinutes", a.visitMinutes())
        .param("status", a.status().value())
        .param("dangerFlag", a.dangerFlag())
        .param("dangerMessage", a.dangerMessage())
        .param("qrVersion", a.qrVersion())
        .param("version", a.version());
  }

  private void saveTranslations(Attraction attraction) {
    saveTranslation(attraction.id(), "ro", attraction.ro());
    saveTranslation(attraction.id(), "en", attraction.en());
  }

  private void saveTranslation(UUID attractionId, String lang, Translation translation) {
    jdbc.sql(
            """
            insert into attraction_translations
                (attraction_id, lang, name, description, history, sources)
            values (:attractionId, :lang, :name, :description, :history, :sources)
            on conflict (attraction_id, lang) do update
            set name = excluded.name, description = excluded.description,
                history = excluded.history, sources = excluded.sources
            """)
        .param("attractionId", attractionId)
        .param("lang", lang)
        .param("name", translation.name())
        .param("description", translation.description())
        .param("history", translation.history())
        .param("sources", translation.sources())
        .update();
  }

  private Attraction map(ResultSet rs, int row) throws SQLException {
    return new Attraction(
        rs.getObject("id", UUID.class),
        AttractionStatus.of(rs.getString("status")),
        new GeoPoint(rs.getDouble("lat"), rs.getDouble("lon")),
        rs.getInt("point_value"),
        rs.getInt("visit_minutes"),
        rs.getInt("danger_radius_m"),
        rs.getBoolean("danger_flag"),
        rs.getString("danger_message"),
        rs.getInt("qr_version"),
        rs.getString("photo_key"),
        rs.getLong("version"),
        translation(rs, "ro_"),
        translation(rs, "en_"));
  }

  private static Translation translation(ResultSet rs, String prefix) throws SQLException {
    return new Translation(
        rs.getString(prefix + "name"),
        rs.getString(prefix + "description"),
        rs.getString(prefix + "history"),
        rs.getString(prefix + "sources"));
  }
}
