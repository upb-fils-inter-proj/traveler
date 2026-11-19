package ro.upb.traveler.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAuditLog implements AuditLog {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  JdbcAuditLog(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Override
  public void record(
      UUID actorStaffId,
      String action,
      String entityType,
      String entityId,
      Map<String, Object> details) {
    String json;
    try {
      json = objectMapper.writeValueAsString(details);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Audit details are not serializable", e);
    }
    jdbc.sql(
            """
            insert into audit_log (actor_staff_id, action, entity_type, entity_id, details)
            values (:actor, :action, :entityType, :entityId, cast(:details as jsonb))
            """)
        .param("actor", actorStaffId)
        .param("action", action)
        .param("entityType", entityType)
        .param("entityId", entityId)
        .param("details", json)
        .update();
  }
}
