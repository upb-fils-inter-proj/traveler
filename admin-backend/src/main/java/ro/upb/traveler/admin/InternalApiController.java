package ro.upb.traveler.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.traveler.admin.internalapi.InternalApi;
import ro.upb.traveler.admin.internalapi.model.ServiceStatus;

/** Implements the internal API contract in api/internal-api.yaml. */
@RestController
class InternalApiController implements InternalApi {

  @Override
  public ResponseEntity<ServiceStatus> getStatus() {
    return ResponseEntity.ok(
        new ServiceStatus().service("admin-backend").status(ServiceStatus.StatusEnum.UP));
  }
}
