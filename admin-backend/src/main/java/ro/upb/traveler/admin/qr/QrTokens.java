package ro.upb.traveler.admin.qr;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The token printed in an attraction's QR code: {@code TRV1.<payload>.<signature>}. The payload is
 * the base64url (no padding) of the 16 bytes of the attraction id and the 4 big-endian bytes of its
 * QR version; the signature is the base64url (no padding) of the Ed25519 signature of the ASCII
 * string {@code TRV1.<payload>}.
 */
public final class QrTokens {

  private static final String PREFIX = "TRV1.";
  private static final int PAYLOAD_BYTES = 20;
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private QrTokens() {}

  public static String sign(PrivateKey key, UUID attractionId, int qrVersion) {
    byte[] payload =
        ByteBuffer.allocate(PAYLOAD_BYTES)
            .putLong(attractionId.getMostSignificantBits())
            .putLong(attractionId.getLeastSignificantBits())
            .putInt(qrVersion)
            .array();
    String signed = PREFIX + ENCODER.encodeToString(payload);
    try {
      Signature signature = Signature.getInstance("Ed25519");
      signature.initSign(key);
      signature.update(signed.getBytes(StandardCharsets.US_ASCII));
      return signed + "." + ENCODER.encodeToString(signature.sign());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Could not sign the QR token", e);
    }
  }

  /**
   * The attraction of a valid token: well formed, signed with the key matching {@code publicKey},
   * and of the attraction's current QR version. A token of an older version is invalid.
   *
   * @param currentQrVersion the current QR version of an attraction, empty when it does not exist
   */
  public static Optional<UUID> verify(
      String token, PublicKey publicKey, Function<UUID, Optional<Integer>> currentQrVersion) {
    if (token == null || !token.startsWith(PREFIX)) {
      return Optional.empty();
    }
    int dot = token.lastIndexOf('.');
    if (dot < PREFIX.length()) {
      return Optional.empty();
    }
    String signed = token.substring(0, dot);
    String encodedPayload = signed.substring(PREFIX.length());
    byte[] payload;
    byte[] signatureBytes;
    try {
      payload = DECODER.decode(encodedPayload);
      signatureBytes = DECODER.decode(token.substring(dot + 1));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    if (payload.length != PAYLOAD_BYTES
        || !ENCODER.encodeToString(payload).equals(encodedPayload)
        || !hasValidSignature(signed, signatureBytes, publicKey)) {
      return Optional.empty();
    }
    ByteBuffer buffer = ByteBuffer.wrap(payload);
    UUID attractionId = new UUID(buffer.getLong(), buffer.getLong());
    int qrVersion = buffer.getInt();
    return currentQrVersion
        .apply(attractionId)
        .filter(current -> current == qrVersion)
        .map(current -> attractionId);
  }

  private static boolean hasValidSignature(String signed, byte[] signature, PublicKey key) {
    try {
      Signature verifier = Signature.getInstance("Ed25519");
      verifier.initVerify(key);
      verifier.update(signed.getBytes(StandardCharsets.US_ASCII));
      return verifier.verify(signature);
    } catch (GeneralSecurityException e) {
      return false;
    }
  }
}
