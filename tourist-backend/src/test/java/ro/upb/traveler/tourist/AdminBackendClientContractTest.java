package ro.upb.traveler.tourist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ro.upb.traveler.tourist.adminclient.ApiClient;
import ro.upb.traveler.tourist.adminclient.api.InternalApi;
import ro.upb.traveler.tourist.adminclient.model.AttractionChanges;
import ro.upb.traveler.tourist.adminclient.model.InternalAttraction;
import ro.upb.traveler.tourist.adminclient.model.ServiceStatus;

/**
 * Consumer side of the contract between the backends. The client is generated from
 * api/internal-api.yaml, and the answer it is tested against must itself be valid for that
 * contract.
 */
@Tag("contract")
class AdminBackendClientContractTest {

  private static final String STATUS_PATH = "/internal/v1/status";
  private static final String STATUS_BODY = "{\"service\":\"admin-backend\",\"status\":\"UP\"}";

  private static final String ATTRACTIONS_PATH = "/internal/v1/attractions";
  private static final String ATTRACTIONS_BODY =
      """
      {"version": 42, "attractions": [
        {"id": "0f8fad5b-d9cb-469f-a165-70867728950e", "status": "removed",
         "location": {"lat": 45.36009, "lon": 25.54273},
         "pointValue": 10, "visitMinutes": 15, "dangerRadiusM": 100,
         "dangerFlag": true, "dangerMessage": "Drum închis", "qrVersion": 2,
         "photoKey": null, "version": 42,
         "translations": {
           "ro": {"name": "Castelul Peleș", "description": "Castel în Sinaia", "history": null},
           "en": {"name": "Peleș Castle", "description": "A castle in Sinaia", "history": null}}}
      ]}
      """;

  private final OpenApiInteractionValidator validator =
      OpenApiInteractionValidator.createFor("internal-api.yaml").build();

  @Test
  void clientCallsTheDocumentedStatusOperation() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://admin-backend");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://admin-backend" + STATUS_PATH))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(STATUS_BODY, MediaType.APPLICATION_JSON));

    InternalApi api = new InternalApi(new ApiClient(builder.build()));
    ServiceStatus status = api.getStatus();

    assertThat(status.getService()).isEqualTo("admin-backend");
    assertThat(status.getStatus()).isEqualTo(ServiceStatus.StatusEnum.UP);
    server.verify();
  }

  @Test
  void clientCallsTheDocumentedAttractionsOperation() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://admin-backend");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://admin-backend" + ATTRACTIONS_PATH + "?since=41"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(ATTRACTIONS_BODY, MediaType.APPLICATION_JSON));

    InternalApi api = new InternalApi(new ApiClient(builder.build()));
    AttractionChanges changes = api.getAttractions(41L);

    assertThat(changes.getVersion()).isEqualTo(42L);
    assertThat(changes.getAttractions()).hasSize(1);
    InternalAttraction attraction = changes.getAttractions().get(0);
    assertThat(attraction.getId())
        .isEqualTo(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"));
    assertThat(attraction.getStatus()).isEqualTo(InternalAttraction.StatusEnum.REMOVED);
    assertThat(attraction.getLocation().getLat()).isEqualTo(45.36009);
    assertThat(attraction.getDangerMessage()).isEqualTo("Drum închis");
    assertThat(attraction.getPhotoKey()).isNull();
    assertThat(attraction.getTranslations().getRo().getName()).isEqualTo("Castelul Peleș");
    server.verify();
  }

  @Test
  void attractionsRequestAndAnswerUsedInTheTestAreValidForTheContract() {
    ValidationReport report =
        validator.validate(
            SimpleRequest.Builder.get(ATTRACTIONS_PATH).withQueryParam("since", "41").build(),
            SimpleResponse.Builder.status(200)
                .withContentType(MediaType.APPLICATION_JSON_VALUE)
                .withBody(ATTRACTIONS_BODY)
                .build());

    assertThat(report.hasErrors()).as(report.toString()).isFalse();
  }

  @Test
  void requestAndAnswerUsedInTheTestAreValidForTheContract() {
    ValidationReport report =
        validator.validate(
            SimpleRequest.Builder.get(STATUS_PATH).build(),
            SimpleResponse.Builder.status(200)
                .withContentType(MediaType.APPLICATION_JSON_VALUE)
                .withBody(STATUS_BODY)
                .build());

    assertThat(report.hasErrors()).as(report.toString()).isFalse();
  }
}
