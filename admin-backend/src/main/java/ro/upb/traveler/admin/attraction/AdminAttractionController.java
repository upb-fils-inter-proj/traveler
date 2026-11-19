package ro.upb.traveler.admin.attraction;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.upb.traveler.admin.qr.QrCodeService;
import ro.upb.traveler.admin.security.StaffAuthentication;
import ro.upb.traveler.admin.web.ApiErrors;

/** Attraction management for City Administrators and Traveler Administration. */
@RestController
@RequestMapping("/admin/attractions")
class AdminAttractionController {

  static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;
  static final MediaType WEBP = MediaType.parseMediaType("image/webp");

  private final AttractionService service;
  private final QrCodeService qrCodes;

  AdminAttractionController(AttractionService service, QrCodeService qrCodes) {
    this.service = service;
    this.qrCodes = qrCodes;
  }

  @GetMapping
  AttractionResponse.Page list(@RequestParam(defaultValue = "false") boolean includeRemoved) {
    return new AttractionResponse.Page(
        service.list(includeRemoved).stream().map(AttractionResponse::of).toList());
  }

  @GetMapping("/{id}")
  AttractionResponse get(@PathVariable UUID id) {
    return AttractionResponse.of(service.get(id));
  }

  @PostMapping
  ResponseEntity<AttractionResponse> create(
      @Valid @RequestBody AttractionInput input, StaffAuthentication auth) {
    Attraction created = service.create(input, auth.getStaff());
    return ResponseEntity.created(URI.create("/admin/attractions/" + created.id()))
        .body(AttractionResponse.of(created));
  }

  @PutMapping("/{id}")
  AttractionResponse update(
      @PathVariable UUID id, @Valid @RequestBody AttractionInput input, StaffAuthentication auth) {
    return AttractionResponse.of(service.update(id, input, auth.getStaff()));
  }

  @PutMapping("/{id}/status")
  AttractionResponse changeStatus(
      @PathVariable UUID id, @Valid @RequestBody StatusInput input, StaffAuthentication auth) {
    return AttractionResponse.of(
        service.changeStatus(id, AttractionStatus.of(input.status()), auth.getStaff()));
  }

  /** The raw body is the photo: Content-Type image/webp, WebP bytes, at most 5 MB. */
  @PutMapping("/{id}/photo")
  AttractionResponse replacePhoto(
      @PathVariable UUID id,
      @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
      InputStream body,
      StaffAuthentication auth)
      throws IOException {
    if (!isWebp(contentType)) {
      throw ApiErrors.of(
          HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The photo must be sent as image/webp.");
    }
    byte[] photo = body.readNBytes(MAX_PHOTO_BYTES + 1);
    if (photo.length > MAX_PHOTO_BYTES) {
      throw ApiErrors.of(HttpStatus.valueOf(413), "The photo must be at most 5 MB.");
    }
    if (!hasWebpSignature(photo)) {
      throw ApiErrors.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The body is not a WebP image.");
    }
    return AttractionResponse.of(service.replacePhoto(id, photo, auth.getStaff()));
  }

  @GetMapping("/{id}/photo")
  ResponseEntity<byte[]> photo(@PathVariable UUID id) {
    return ResponseEntity.ok().contentType(WEBP).body(service.photo(id));
  }

  @PostMapping("/{id}/qr-code/regenerate")
  AttractionResponse regenerateQr(@PathVariable UUID id, StaffAuthentication auth) {
    return AttractionResponse.of(service.regenerateQr(id, auth.getStaff()));
  }

  @GetMapping("/{id}/qr-code.pdf")
  ResponseEntity<byte[]> qrCodePdf(@PathVariable UUID id) {
    Attraction attraction = service.get(id);
    byte[] pdf = qrCodes.pdf(attraction);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("qr-" + id + "-v" + attraction.qrVersion() + ".pdf")
                .build()
                .toString())
        .body(pdf);
  }

  private static boolean isWebp(String contentType) {
    if (contentType == null) {
      return false;
    }
    try {
      return WEBP.equalsTypeAndSubtype(MediaType.parseMediaType(contentType));
    } catch (InvalidMediaTypeException e) {
      return false;
    }
  }

  /** A WebP file starts with "RIFF", four size bytes, then "WEBP". */
  static boolean hasWebpSignature(byte[] bytes) {
    return bytes.length >= 12
        && Arrays.equals(bytes, 0, 4, "RIFF".getBytes(StandardCharsets.US_ASCII), 0, 4)
        && Arrays.equals(bytes, 8, 12, "WEBP".getBytes(StandardCharsets.US_ASCII), 0, 4);
  }
}
