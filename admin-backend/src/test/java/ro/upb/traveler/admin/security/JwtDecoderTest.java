package ro.upb.traveler.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtDecoderTest {

  private static final String ISSUER = "https://auth.test/application/o/traveler-admin/";

  private final OAuth2TokenValidator<Jwt> validator =
      SecurityConfig.tokenValidator(ISSUER, "traveler-admin");

  @Test
  void acceptsTheIssuerAndAudienceOfTheAdminProvider() {
    assertThat(validator.validate(jwt(ISSUER, List.of("traveler-admin"))).hasErrors()).isFalse();
  }

  @Test
  void rejectsAnotherIssuerOrAudience() {
    assertThat(
            validator
                .validate(jwt("https://auth.test/application/o/other/", List.of("traveler-admin")))
                .hasErrors())
        .isTrue();
    assertThat(validator.validate(jwt(ISSUER, List.of("traveler-mobile"))).hasErrors()).isTrue();
  }

  @Test
  void rejectsEveryTokenWhenTokenValidationIsNotConfigured() {
    JwtDecoder decoder =
        new SecurityConfig().jwtDecoder(new TravelerSecurityProperties("", "", "traveler-admin"));

    assertThatThrownBy(() -> decoder.decode("a.b.c")).isInstanceOf(JwtException.class);
  }

  private static Jwt jwt(String issuer, List<String> audience) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .issuer(issuer)
        .audience(audience)
        .subject("subject")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
