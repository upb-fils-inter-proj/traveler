package ro.upb.traveler.admin.qr;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.upb.traveler.admin.attraction.Attraction;
import ro.upb.traveler.admin.web.ApiErrors;

/** Signs QR tokens and prints them. The matching public key is used by the tourist backend. */
@Service
@EnableConfigurationProperties(QrProperties.class)
public class QrCodeService {

  private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);

  private final PrivateKey privateKey;

  @Autowired
  public QrCodeService(QrProperties properties) {
    this(parse(properties.privateKey()));
    if (privateKey == null) {
      log.warn("traveler.qr.private-key is not set: the QR code PDF answers 503");
    }
  }

  /** {@code privateKey} is null when no key is configured. */
  public QrCodeService(PrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  public String token(Attraction attraction) {
    if (privateKey == null) {
      throw ApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "The QR signing key is not configured.");
    }
    return QrTokens.sign(privateKey, attraction.id(), attraction.qrVersion());
  }

  public byte[] pdf(Attraction attraction) {
    return QrPdf.render(attraction, token(attraction));
  }

  private static PrivateKey parse(String base64) {
    if (!StringUtils.hasText(base64)) {
      return null;
    }
    try {
      return KeyFactory.getInstance("Ed25519")
          .generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(base64)));
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException(
          "traveler.qr.private-key must be an Ed25519 private key, PKCS#8 DER in base64", e);
    }
  }
}
