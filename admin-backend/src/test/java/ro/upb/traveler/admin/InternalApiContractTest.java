package ro.upb.traveler.admin;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ro.upb.traveler.admin.security.SecurityConfig;
import ro.upb.traveler.admin.testsupport.AdminTestBeans;

/**
 * Provider side of the contract between the backends: every answer of the internal API must be
 * valid for api/internal-api.yaml.
 */
@Tag("contract")
@WebMvcTest(InternalApiController.class)
@Import({SecurityConfig.class, AdminTestBeans.class})
class InternalApiContractTest {

  @Autowired private MockMvc mvc;

  @Test
  void statusAnswerMatchesTheContract() throws Exception {
    mvc.perform(get("/internal/v1/status"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid("internal-api.yaml"));
  }
}
