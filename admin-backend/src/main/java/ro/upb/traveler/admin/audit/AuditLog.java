package ro.upb.traveler.admin.audit;

import java.util.Map;
import java.util.UUID;

/** Who changed what in the admin web app (table audit_log). */
public interface AuditLog {

  /** Writes one row; {@code details} holds the changed fields and is stored as jsonb. */
  void record(
      UUID actorStaffId,
      String action,
      String entityType,
      String entityId,
      Map<String, Object> details);
}
