package ro.upb.traveler.admin.qr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QrTokensTest {

  private static final UUID ATTRACTION = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  private final KeyPair keys = keys();

  @Test
  void aTokenHasTheDocumentedFormat() {
    String token = QrTokens.sign(keys.getPrivate(), ATTRACTION, 3);

    String[] parts = token.split("\\.");
    assertThat(parts).hasSize(3);
    assertThat(parts[0]).isEqualTo("TRV1");
    assertThat(token).doesNotContain("=");
    ByteBuffer payload = ByteBuffer.wrap(Base64.getUrlDecoder().decode(parts[1]));
    assertThat(payload.remaining()).isEqualTo(20);
    assertThat(new UUID(payload.getLong(), payload.getLong())).isEqualTo(ATTRACTION);
    assertThat(payload.getInt()).isEqualTo(3);
    assertThat(Base64.getUrlDecoder().decode(parts[2])).hasSize(64);
  }

  @Test
  void aTokenOfTheCurrentVersionIsValid() {
    String token = QrTokens.sign(keys.getPrivate(), ATTRACTION, 1);

    assertThat(QrTokens.verify(token, keys.getPublic(), id -> Optional.of(1))).contains(ATTRACTION);
  }

  @Test
  void aTokenOfAnOlderVersionIsRejected() {
    String old = QrTokens.sign(keys.getPrivate(), ATTRACTION, 1);

    assertThat(QrTokens.verify(old, keys.getPublic(), id -> Optional.of(2))).isEmpty();
  }

  @Test
  void aChangedForeignOrMalformedTokenIsRejected() {
    String token = QrTokens.sign(keys.getPrivate(), ATTRACTION, 1);
    String otherAttraction = QrTokens.sign(keys.getPrivate(), UUID.randomUUID(), 1);
    String swapped =
        token.substring(0, token.lastIndexOf('.'))
            + otherAttraction.substring(otherAttraction.lastIndexOf('.'));

    assertThat(QrTokens.verify(swapped, keys.getPublic(), id -> Optional.of(1))).isEmpty();
    assertThat(QrTokens.verify(token, keys().getPublic(), id -> Optional.of(1))).isEmpty();
    assertThat(QrTokens.verify(token, keys.getPublic(), id -> Optional.empty())).isEmpty();
    assertThat(QrTokens.verify("TRV1.abc", keys.getPublic(), id -> Optional.of(1))).isEmpty();
    assertThat(QrTokens.verify("nonsense", keys.getPublic(), id -> Optional.of(1))).isEmpty();
    assertThat(QrTokens.verify(token + "x", keys.getPublic(), id -> Optional.of(1))).isEmpty();
  }

  private static KeyPair keys() {
    try {
      return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
