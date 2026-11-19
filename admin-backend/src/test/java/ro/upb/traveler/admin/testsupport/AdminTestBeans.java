package ro.upb.traveler.admin.testsupport;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.transaction.support.TransactionOperations;
import ro.upb.traveler.admin.qr.QrCodeService;

/** Fakes for the web slice tests: no database and no object storage. */
@TestConfiguration
public class AdminTestBeans {

  /** QR signing keys made for the tests; the public key verifies the printed tokens. */
  public static final KeyPair QR_KEYS = ed25519();

  @Bean
  public InMemoryAttractionRepository attractionRepository() {
    return new InMemoryAttractionRepository();
  }

  @Bean
  public InMemoryAuditLog auditLog() {
    return new InMemoryAuditLog();
  }

  @Bean
  public FakePhotoStorage photoStorage() {
    return new FakePhotoStorage();
  }

  @Bean
  public InMemoryStaffRepository staffRepository() {
    return new InMemoryStaffRepository();
  }

  @Bean
  public TransactionOperations transactionOperations() {
    return TransactionOperations.withoutTransaction();
  }

  @Bean
  public QrCodeService qrCodeService() {
    return new QrCodeService(QR_KEYS.getPrivate());
  }

  private static KeyPair ed25519() {
    try {
      return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /** A validated access token of authentik with this subject. */
  public static JwtRequestPostProcessor token(String subject) {
    return jwt().jwt(j -> j.issuer(InMemoryStaffRepository.ISSUER).subject(subject));
  }

  public static JwtRequestPostProcessor cityAdmin() {
    return token("city-admin-subject");
  }

  public static JwtRequestPostProcessor travelerAdmin() {
    return token("traveler-admin-subject");
  }

  public static JwtRequestPostProcessor partner() {
    return token("partner-subject");
  }
}
