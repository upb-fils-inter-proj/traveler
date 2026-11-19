package ro.upb.traveler.admin;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ro.upb.traveler.admin.attraction.Attraction;
import ro.upb.traveler.admin.attraction.AttractionStatus;
import ro.upb.traveler.admin.attraction.GeoPoint;
import ro.upb.traveler.admin.attraction.Translation;
import ro.upb.traveler.admin.security.SecurityConfig;
import ro.upb.traveler.admin.testsupport.AdminTestBeans;
import ro.upb.traveler.admin.testsupport.InMemoryAttractionRepository;

/**
 * Provider side of the contract between the backends: every answer of the internal API must be
 * valid for api/internal-api.yaml.
 */
@Tag("contract")
@WebMvcTest(InternalApiController.class)
@Import({SecurityConfig.class, AdminTestBeans.class})
class InternalApiContractTest {

  @Autowired private MockMvc mvc;
  @Autowired private InMemoryAttractionRepository attractions;

  private final UUID open = UUID.randomUUID();
  private final UUID removed = UUID.randomUUID();

  @BeforeEach
  void storeAttractions() {
    attractions.reset();
    attractions.insert(
        new Attraction(
            open,
            AttractionStatus.OPEN,
            new GeoPoint(45.36009, 25.54273),
            10,
            15,
            100,
            false,
            null,
            1,
            null,
            3,
            new Translation("Castelul Peleș", "Castel în Sinaia", "Istoric", "Două surse"),
            new Translation("Peleș Castle", "A castle in Sinaia", null, "Two sources")),
        null);
    attractions.insert(
        new Attraction(
            removed,
            AttractionStatus.REMOVED,
            new GeoPoint(45.5155, 25.3673),
            20,
            30,
            150,
            true,
            "Drum închis",
            2,
            "attractions/" + removed + "/photo.webp",
            7,
            new Translation("Castelul Bran", "Castel", null, null),
            new Translation("Bran Castle", "Castle", null, null)),
        null);
  }

  @Test
  void statusAnswerMatchesTheContract() throws Exception {
    mvc.perform(get("/internal/v1/status"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid("internal-api.yaml"));
  }

  @Test
  void allAttractionsAnswerMatchesTheContractAndLeavesOutTheSources() throws Exception {
    mvc.perform(get("/internal/v1/attractions"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid("internal-api.yaml"))
        .andExpect(jsonPath("$.version").value(7))
        .andExpect(jsonPath("$.attractions", hasSize(2)))
        .andExpect(jsonPath("$.attractions[0].id").value(open.toString()))
        .andExpect(jsonPath("$.attractions[0].translations.ro.history").value("Istoric"))
        .andExpect(jsonPath("$.attractions[1].status").value("removed"))
        .andExpect(content().string(not(Matchers.containsString("sources"))));
  }

  @Test
  void onlyAttractionsChangedSinceTheGivenVersionAreReturned() throws Exception {
    mvc.perform(get("/internal/v1/attractions").param("since", "3"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid("internal-api.yaml"))
        .andExpect(jsonPath("$.version").value(7))
        .andExpect(jsonPath("$.attractions", hasSize(1)))
        .andExpect(jsonPath("$.attractions[0].id").value(removed.toString()))
        .andExpect(
            jsonPath("$.attractions[0].photoKey").value("attractions/" + removed + "/photo.webp"));

    mvc.perform(get("/internal/v1/attractions").param("since", "7"))
        .andExpect(openApi().isValid("internal-api.yaml"))
        .andExpect(jsonPath("$.version").value(7))
        .andExpect(jsonPath("$.attractions", hasSize(0)));
  }

  @Test
  void anEmptyTableHasVersionZero() throws Exception {
    attractions.reset();

    mvc.perform(get("/internal/v1/attractions"))
        .andExpect(openApi().isValid("internal-api.yaml"))
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.attractions", hasSize(0)));
  }
}
