package ro.upb.traveler.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after the access token is validated and gives it the role of the staff member it belongs to.
 * A token without an active staff row keeps no role, so /admin answers 403.
 */
class StaffAuthenticationFilter extends OncePerRequestFilter {

  private final StaffRepository staff;

  StaffAuthenticationFilter(StaffRepository staff) {
    this.staff = staff;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication current = SecurityContextHolder.getContext().getAuthentication();
    if (current instanceof JwtAuthenticationToken token
        && !(current instanceof StaffAuthentication)) {
      Jwt jwt = token.getToken();
      String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
      if (issuer != null && jwt.getSubject() != null) {
        staff
            .findActive(issuer, jwt.getSubject())
            .ifPresent(member -> {
              SecurityContext context = SecurityContextHolder.createEmptyContext();
              context.setAuthentication(new StaffAuthentication(jwt, member));
              SecurityContextHolder.setContext(context);
            });
      }
    }
    chain.doFilter(request, response);
  }
}
