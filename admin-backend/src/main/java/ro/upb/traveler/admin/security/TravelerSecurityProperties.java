package ro.upb.traveler.admin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validation of the access tokens: exact issuer, JWKS URL of the signing keys and required
 * audience.
 */
@ConfigurationProperties("traveler.security")
public record TravelerSecurityProperties(String issuer, String jwkSetUri, String audience) {}
