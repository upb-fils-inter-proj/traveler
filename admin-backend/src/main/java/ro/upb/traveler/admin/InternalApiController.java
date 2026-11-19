package ro.upb.traveler.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.traveler.admin.attraction.Attraction;
import ro.upb.traveler.admin.attraction.AttractionRepository;
import ro.upb.traveler.admin.attraction.ChangedAttractions;
import ro.upb.traveler.admin.attraction.Translation;
import ro.upb.traveler.admin.internalapi.InternalApi;
import ro.upb.traveler.admin.internalapi.model.AttractionChanges;
import ro.upb.traveler.admin.internalapi.model.GeoPoint;
import ro.upb.traveler.admin.internalapi.model.InternalAttraction;
import ro.upb.traveler.admin.internalapi.model.InternalAttractionTranslation;
import ro.upb.traveler.admin.internalapi.model.InternalAttractionTranslations;
import ro.upb.traveler.admin.internalapi.model.ServiceStatus;

/** Implements the internal API contract in api/internal-api.yaml. */
@RestController
class InternalApiController implements InternalApi {

  private final AttractionRepository attractions;

  InternalApiController(AttractionRepository attractions) {
    this.attractions = attractions;
  }

  @Override
  public ResponseEntity<ServiceStatus> getStatus() {
    return ResponseEntity.ok(
        new ServiceStatus().service("admin-backend").status(ServiceStatus.StatusEnum.UP));
  }

  /** The sources of the texts are never sent to the tourist backend. */
  @Override
  public ResponseEntity<AttractionChanges> getAttractions(Long since) {
    ChangedAttractions changed =
        attractions.findChangedSince(since == null ? Long.MIN_VALUE : since);
    return ResponseEntity.ok(
        new AttractionChanges()
            .version(changed.version())
            .attractions(
                changed.attractions().stream().map(InternalApiController::toContract).toList()));
  }

  private static InternalAttraction toContract(Attraction a) {
    return new InternalAttraction()
        .id(a.id())
        .status(InternalAttraction.StatusEnum.fromValue(a.status().value()))
        .location(new GeoPoint().lat(a.location().lat()).lon(a.location().lon()))
        .pointValue(a.pointValue())
        .visitMinutes(a.visitMinutes())
        .dangerRadiusM(a.dangerRadiusM())
        .dangerFlag(a.dangerFlag())
        .dangerMessage(a.dangerMessage())
        .qrVersion(a.qrVersion())
        .photoKey(a.photoKey())
        .version(a.version())
        .translations(
            new InternalAttractionTranslations().ro(toContract(a.ro())).en(toContract(a.en())));
  }

  private static InternalAttractionTranslation toContract(Translation t) {
    return new InternalAttractionTranslation()
        .name(t.name())
        .description(t.description())
        .history(t.history());
  }
}
