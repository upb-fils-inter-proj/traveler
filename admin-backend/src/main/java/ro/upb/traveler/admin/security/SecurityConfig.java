package ro.upb.traveler.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

/**
 * /admin needs an access token of authentik's "Traveler admin" provider and an active staff row.
 * Health, the OpenAPI description and the internal API (reachable only on the private network) are
 * public. No CORS: the gateway serves the admin web app from the same origin.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(TravelerSecurityProperties.class)
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, StaffRepository staff, ObjectMapper objectMapper) throws Exception {
    AuthenticationEntryPoint unauthorized = unauthorized(objectMapper);
    AccessDeniedHandler forbidden =
        (request, response, ex) ->
            writeProblem(
                response,
                objectMapper,
                HttpStatus.FORBIDDEN,
                "Your account may not use this endpoint.");
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/internal/**",
                    "/error")
                .permitAll()
                .requestMatchers("/admin/attractions", "/admin/attractions/**")
                .hasAnyRole("CITY_ADMIN", "TRAVELER_ADMIN")
                .requestMatchers("/admin/**")
                .hasAnyRole("PARTNER", "CITY_ADMIN", "TRAVELER_ADMIN")
                .anyRequest()
                .denyAll())
        .oauth2ResourceServer(oauth ->
            oauth
                .jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        token -> new JwtAuthenticationToken(token, List.of(), token.getSubject())))
                .authenticationEntryPoint(unauthorized)
                .accessDeniedHandler(forbidden))
        .exceptionHandling(exceptions ->
            exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .addFilterAfter(
            new StaffAuthenticationFilter(staff), BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(TravelerSecurityProperties properties) {
    if (!StringUtils.hasText(properties.issuer()) || !StringUtils.hasText(properties.jwkSetUri())) {
      log.warn(
          "traveler.security.issuer or traveler.security.jwk-set-uri is not set:"
              + " every access token is rejected");
      return token -> {
        throw new BadJwtException("Access token validation is not configured");
      };
    }
    String audience =
        StringUtils.hasText(properties.audience()) ? properties.audience() : "traveler-admin";
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
            .jwsAlgorithms(algorithms ->
                algorithms.addAll(
                    List.of(
                        SignatureAlgorithm.RS256,
                        SignatureAlgorithm.RS384,
                        SignatureAlgorithm.RS512,
                        SignatureAlgorithm.ES256,
                        SignatureAlgorithm.ES384,
                        SignatureAlgorithm.ES512)))
            .build();
    decoder.setJwtValidator(tokenValidator(properties.issuer(), audience));
    return decoder;
  }

  /** Expiry, exact issuer and audience. */
  static OAuth2TokenValidator<Jwt> tokenValidator(String issuer, String audience) {
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer),
        new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience)));
  }

  private static AuthenticationEntryPoint unauthorized(ObjectMapper objectMapper) {
    BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    return (request, response, ex) -> {
      // Sets 401 and the WWW-Authenticate header.
      bearer.commence(request, response, ex);
      writeProblem(
          response, objectMapper, HttpStatus.UNAUTHORIZED, "A valid access token is required.");
    };
  }

  private static void writeProblem(
      HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
  }
}
