package ro.upb.traveler.admin.attraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.cityAdmin;
import static ro.upb.traveler.admin.testsupport.AdminTestBeans.partner;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.upb.traveler.admin.qr.QrTokens;
import ro.upb.traveler.admin.security.SecurityConfig;
import ro.upb.traveler.admin.testsupport.AdminTestBeans;
import ro.upb.traveler.admin.testsupport.InMemoryAttractionRepository;
import ro.upb.traveler.admin.testsupport.InMemoryAuditLog;
import ro.upb.traveler.admin.testsupport.InMemoryStaffRepository;

@WebMvcTest(AdminAttractionController.class)
@Import({SecurityConfig.class, AttractionService.class, AdminTestBeans.class})
class QrCodeEndpointsTest {

  private static final String RO_NAME = "Mănăstirea Sfântul Ștefan și Țara Bârsei";
  private static final String EN_NAME = "Saint Stephen's Monastery";

  @Autowired private MockMvc mvc;
  @Autowired private AttractionService service;
  @Autowired private InMemoryAttractionRepository attractions;
  @Autowired private InMemoryAuditLog audit;

  private Attraction attraction;

  @BeforeEach
  void createAttraction() {
    attractions.reset();
    audit.reset();
    attraction =
        service.create(
            new AttractionInput(
                new AttractionInput.Location(45.36009, 25.54273),
                10,
                null,
                new AttractionInput.Translations(
                    new AttractionInput.Text(RO_NAME, "Descriere", null, null),
                    new AttractionInput.Text(EN_NAME, "Description", null, null))),
            InMemoryStaffRepository.CITY_ADMIN);
  }

  @Test
  void thePdfIsProducedInUnderTwoSecondsAndCarriesTheSignedToken() throws Exception {
    long started = System.nanoTime();
    MvcResult result =
        mvc.perform(get("/admin/attractions/" + attraction.id() + "/qr-code.pdf").with(cityAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"qr-" + attraction.id() + "-v1.pdf\""))
            .andReturn();
    Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
    byte[] pdf = result.getResponse().getContentAsByteArray();

    assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertThat(document.getNumberOfPages()).isEqualTo(1);
      assertThat(document.getPage(0).getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getWidth());
      String text = new PDFTextStripper().getText(document);
      assertThat(text)
          .contains("Traveler", RO_NAME, EN_NAME, "Version 1", attraction.id().toString());

      String token = decodeQr(document);
      assertThat(token).startsWith("TRV1.");
      assertThat(QrTokens.verify(token, AdminTestBeans.QR_KEYS.getPublic(), this::currentQrVersion))
          .contains(attraction.id());
    }
  }

  @Test
  void regeneratingMakesTheOldCodeInvalid() throws Exception {
    String oldToken = tokenOfPdf();

    mvc.perform(
            post("/admin/attractions/" + attraction.id() + "/qr-code/regenerate").with(cityAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qrVersion").value(2))
        .andExpect(jsonPath("$.version").value(attraction.version() + 1));
    assertThat(audit.last().action()).isEqualTo("attraction.qr-regenerate");
    assertThat(audit.last().details())
        .containsOnlyKeys("qrVersion")
        .containsEntry("qrVersion", Map.of("from", 1, "to", 2));

    String newToken = tokenOfPdf();
    assertThat(
            QrTokens.verify(oldToken, AdminTestBeans.QR_KEYS.getPublic(), this::currentQrVersion))
        .isEmpty();
    assertThat(
            QrTokens.verify(newToken, AdminTestBeans.QR_KEYS.getPublic(), this::currentQrVersion))
        .contains(attraction.id());
  }

  @Test
  void onlyAdministratorsPrintOrRegenerate() throws Exception {
    mvc.perform(get("/admin/attractions/" + attraction.id() + "/qr-code.pdf").with(partner()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/admin/attractions/" + attraction.id() + "/qr-code/regenerate"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/admin/attractions/" + UUID.randomUUID() + "/qr-code.pdf").with(cityAdmin()))
        .andExpect(status().isNotFound());
  }

  private Optional<Integer> currentQrVersion(UUID id) {
    return attractions.findById(id).map(Attraction::qrVersion);
  }

  private String tokenOfPdf() throws Exception {
    byte[] pdf =
        mvc.perform(get("/admin/attractions/" + attraction.id() + "/qr-code.pdf").with(cityAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return decodeQr(document);
    }
  }

  private static String decodeQr(PDDocument document) throws Exception {
    BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 100);
    int width = image.getWidth();
    int height = image.getHeight();
    int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
    BinaryBitmap bitmap =
        new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
    return new QRCodeReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, true)).getText();
  }
}
