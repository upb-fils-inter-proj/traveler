package ro.upb.traveler.admin.testsupport;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import ro.upb.traveler.admin.audit.AuditLog;

/** Keeps the audit rows in memory. */
public class InMemoryAuditLog implements AuditLog {

  public record Entry(
      UUID actorStaffId,
      String action,
      String entityType,
      String entityId,
      Map<String, Object> details) {}

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  public void reset() {
    entries.clear();
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }

  public Entry last() {
    return entries.get(entries.size() - 1);
  }

  @Override
  public void record(
      UUID actorStaffId,
      String action,
      String entityType,
      String entityId,
      Map<String, Object> details) {
    entries.add(new Entry(actorStaffId, action, entityType, entityId, details));
  }
}
