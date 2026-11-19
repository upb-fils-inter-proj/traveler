package ro.upb.traveler.admin.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Who is signed in, for the admin web app. */
@RestController
class AdminMeController {

  @GetMapping("/admin/me")
  Me me(StaffAuthentication authentication) {
    StaffUser staff = authentication.getStaff();
    return new Me(staff.email(), staff.role());
  }

  record Me(String email, String role) {}
}
