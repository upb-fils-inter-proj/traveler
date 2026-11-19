package ro.upb.traveler.admin.qr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The Ed25519 private key that signs QR tokens, PKCS#8 DER in base64. */
@ConfigurationProperties("traveler.qr")
public record QrProperties(String privateKey) {}
