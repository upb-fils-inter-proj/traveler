package ro.upb.traveler.admin.attraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.cityAdmin;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.partner;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.token;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.travelerAdmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.upb.traveler.admin.security.SecurityConfig;
import ro.upb.traveler.admin.testsupport.AdminTestBeans;
import ro.upb.traveler.admin.testsupport.FakePhotoStorage;
import ro.upb.traveler.admin.testsupport.InMemoryAttractionRepository;
import ro.upb.traveler.admin.testsupport.InMemoryAuditLog;
import ro.upb.traveler.admin.testsupport.InMemoryStaffRepository;

@WebMvcTest(AdminAttractionController.class)
@Import({SecurityConfig.class, AttractionService.class, AdminTestBeans.class})
class AdminAttractionControllerTest {

  private static final MediaType WEBP = MediaType.parseMediaType("image/webp");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private InMemoryAttractionRepository attractions;
  @Autowired private InMemoryAuditLog audit;
  @Autowired private FakePhotoStorage photos;

  @BeforeEach
  void reset() {
    attractions.reset();
    audit.reset();
    photos.reset();
  }

  @Test
  void aRequestWithoutTokenIsUnauthorized() throws Exception {
    mvc.perform(get("/admin/attractions"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().exists("WWW-Authenticate"));
  }

  @Test
  void aPartnerMayNotManageAttractions() throws Exception {
    mvc.perform(get("/admin/attractions").with(partner()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void aTokenWithoutAStaffRowIsForbidden() throws Exception {
    mvc.perform(get("/admin/attractions").with(token("unknown-subject")))
        .andExpect(status().isForbidden());
  }

  @Test
  void cityAndTravelerAdministratorsManageAttractions() throws Exception {
    mvc.perform(get("/admin/attractions").with(cityAdmin())).andExpect(status().isOk());
    mvc.perform(get("/admin/attractions").with(travelerAdmin())).andExpect(status().isOk());
  }

  @Test
  void invalidInputIsAnsweredWithTheInvalidFields() throws Exception {
    Map<String, Object> input = input("Castelul Peleș", 10);
    input.put("location", Map.of("lat", 91, "lon", 25.5));
    input.put("pointValue", 0);
    input.put("translations", Map.of("ro", Map.of("name", " ", "description", "Un castel")));

    mvc.perform(
            post("/admin/attractions")
                .with(cityAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    containsInAnyOrder(
                        "location.lat", "pointValue", "translations.en", "translations.ro.name")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("must not be blank")));
    assertThat(audit.entries()).isEmpty();
  }

  @Test
  void createReturnsTheAttractionAndWritesAnAuditRow() throws Exception {
    JsonNode created =
        body(
            create(input("Castelul Peleș", 10))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/admin/attractions/")))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.location.lat").value(45.36009))
                .andExpect(jsonPath("$.location.lon").value(25.54273))
                .andExpect(jsonPath("$.pointValue").value(10))
                .andExpect(jsonPath("$.visitMinutes").value(15))
                .andExpect(jsonPath("$.dangerRadiusM").value(100))
                .andExpect(jsonPath("$.dangerFlag").value(false))
                .andExpect(jsonPath("$.dangerMessage").isEmpty())
                .andExpect(jsonPath("$.qrVersion").value(1))
                .andExpect(jsonPath("$.hasPhoto").value(false))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.translations.ro.name").value("Castelul Peleș"))
                .andExpect(jsonPath("$.translations.en.sources").isEmpty()));

    String id = created.get("id").asText();
    InMemoryAuditLog.Entry entry = audit.last();
    assertThat(entry.action()).isEqualTo("attraction.create");
    assertThat(entry.actorStaffId()).isEqualTo(InMemoryStaffRepository.CITY_ADMIN.id());
    assertThat(entry.entityType()).isEqualTo("attraction");
    assertThat(entry.entityId()).isEqualTo(id);
    assertThat(entry.details()).containsEntry("pointValue", 10);

    mvc.perform(get("/admin/attractions/" + id).with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void everyChangeTakesANewVersionAndIsAudited() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));

    mvc.perform(
            put("/admin/attractions/" + id)
                .with(travelerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input("Castelul Peleș", 20))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pointValue").value(20))
        .andExpect(jsonPath("$.version").value(2));
    assertThat(audit.last().action()).isEqualTo("attraction.update");
    assertThat(audit.last().actorStaffId()).isEqualTo(InMemoryStaffRepository.TRAVELER_ADMIN.id());
    assertThat(audit.last().details())
        .containsOnlyKeys("pointValue")
        .containsEntry("pointValue", Map.of("from", 10, "to", 20));

    changeStatus(id, "closed")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("closed"))
        .andExpect(jsonPath("$.version").value(3));
    assertThat(audit.last().action()).isEqualTo("attraction.status");
    assertThat(audit.last().details())
        .containsEntry("status", Map.of("from", "open", "to", "closed"));

    assertThat(audit.entries()).hasSize(3);
  }

  @Test
  void removedIsFinal() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));
    changeStatus(id, "removed").andExpect(status().isOk());

    changeStatus(id, "open")
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(
            put("/admin/attractions/" + id)
                .with(cityAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input("Castelul Peleș", 20))))
        .andExpect(status().isConflict());
    assertThat(audit.entries()).hasSize(2);
  }

  @Test
  void anUnknownStatusIsAValidationError() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));

    changeStatus(id, "deleted")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
  }

  @Test
  void theListIsSortedByRomanianNameAndHidesRemovedAttractions() throws Exception {
    createdId(input("Ștrandul", 10));
    String removed = createdId(input("Zăvoiul", 10));
    createdId(input("Sinaia", 10));
    createdId(input("Ăla", 10));
    changeStatus(removed, "removed").andExpect(status().isOk());

    mvc.perform(get("/admin/attractions").with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(3)))
        .andExpect(jsonPath("$.items[0].translations.ro.name").value("Ăla"))
        .andExpect(jsonPath("$.items[1].translations.ro.name").value("Sinaia"))
        .andExpect(jsonPath("$.items[2].translations.ro.name").value("Ștrandul"));
    mvc.perform(get("/admin/attractions?includeRemoved=true").with(cityAdmin()))
        .andExpect(jsonPath("$.items", hasSize(4)))
        .andExpect(jsonPath("$.items[3].status").value("removed"));
  }

  @Test
  void anUnknownAttractionIsNotFound() throws Exception {
    mvc.perform(get("/admin/attractions/" + UUID.randomUUID()).with(cityAdmin()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void aPhotoMustBeWebp() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));

    putPhoto(id, MediaType.IMAGE_JPEG, webp(100)).andExpect(status().isUnsupportedMediaType());
    putPhoto(id, WEBP, "not a webp image".getBytes(StandardCharsets.US_ASCII))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    putPhoto(id, WEBP, webp(5 * 1024 * 1024 + 1)).andExpect(status().is(413));
    assertThat(photos.keys()).isEmpty();
  }

  @Test
  void aPhotoIsStoredAndReplaced() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));
    byte[] first = webp(5 * 1024 * 1024);
    byte[] second = webp(2000);

    mvc.perform(get("/admin/attractions/" + id + "/photo").with(cityAdmin()))
        .andExpect(status().isNotFound());
    putPhoto(id, WEBP, first)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasPhoto").value(true))
        .andExpect(jsonPath("$.version").value(2));
    String firstKey = photos.keys().iterator().next();
    assertThat(firstKey).startsWith("attractions/" + id + "/").endsWith(".webp");
    assertThat(audit.last().action()).isEqualTo("attraction.photo");
    assertThat(audit.last().details()).containsOnlyKeys("photoKey");

    putPhoto(id, WEBP, second).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
    assertThat(photos.keys()).hasSize(1).doesNotContain(firstKey);
    mvc.perform(get("/admin/attractions/" + id + "/photo").with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(WEBP))
        .andExpect(content().bytes(second));
  }

  @Test
  void photoEndpointsAnswer503WhileTheStorageIsUnavailable() throws Exception {
    String id = createdId(input("Castelul Peleș", 10));
    putPhoto(id, WEBP, webp(100)).andExpect(status().isOk());
    photos.setAvailable(false);

    putPhoto(id, WEBP, webp(100))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(get("/admin/attractions/" + id + "/photo").with(cityAdmin()))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/admin/attractions/" + id).with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2));
  }

  private ResultActions create(Map<String, Object> input) throws Exception {
    return mvc.perform(
        post("/admin/attractions")
            .with(cityAdmin())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(input)));
  }

  private String createdId(Map<String, Object> input) throws Exception {
    return body(create(input).andExpect(status().isCreated())).get("id").asText();
  }

  private ResultActions changeStatus(String id, String status) throws Exception {
    return mvc.perform(
        put("/admin/attractions/" + id + "/status")
            .with(cityAdmin())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"" + status + "\"}"));
  }

  private ResultActions putPhoto(String id, MediaType type, byte[] bytes) throws Exception {
    return mvc.perform(
        put("/admin/attractions/" + id + "/photo")
            .with(cityAdmin())
            .contentType(type)
            .content(bytes));
  }

  private JsonNode body(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private static Map<String, Object> input(String roName, int pointValue) {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("location", Map.of("lat", 45.36009, "lon", 25.54273));
    input.put("pointValue", pointValue);
    input.put(
        "translations",
        Map.of(
            "ro", Map.of("name", roName, "description", "Castel în Sinaia"),
            "en", Map.of("name", "Peleș Castle", "description", "A castle in Sinaia")));
    return input;
  }

  /** WebP signature followed by filler bytes, {@code size} bytes in all. */
  private static byte[] webp(int size) {
    byte[] bytes = new byte[size];
    Arrays.fill(bytes, (byte) 7);
    System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
    System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
    return bytes;
  }
}
