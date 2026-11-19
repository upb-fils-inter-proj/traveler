package ro.upb.traveler.admin.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.cityAdmin;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.partner;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.token;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ro.upb.traveler.admin.testsupport.AdminTestBeans;

@WebMvcTest(AdminMeController.class)
@Import({SecurityConfig.class, AdminTestBeans.class})
class AdminMeControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void anyActiveStaffMemberSeesTheirEmailAndRole() throws Exception {
    mvc.perform(get("/admin/me").with(partner()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("partner@example.ro"))
        .andExpect(jsonPath("$.role").value("partner"));
    mvc.perform(get("/admin/me").with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("city_admin"));
  }

  @Test
  void aTokenWithoutAnActiveStaffRowIsForbidden() throws Exception {
    mvc.perform(get("/admin/me").with(token("unknown-subject"))).andExpect(status().isForbidden());
  }

  @Test
  void aRequestWithoutTokenIsUnauthorized() throws Exception {
    mvc.perform(get("/admin/me")).andExpect(status().isUnauthorized());
  }
}
