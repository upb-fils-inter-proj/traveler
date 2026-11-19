package ro.upb.traveler.admin.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import ro.upb.traveler.admin.attraction.Attraction;

/**
 * The printable A4 page of an attraction's QR code: "Traveler", the Romanian and English names, the
 * QR code (10 cm wide), "Version N" and the attraction id.
 */
final class QrPdf {

  private static final float POINTS_PER_MM = 72f / 25.4f;
  private static final float QR_SIZE = 100 * POINTS_PER_MM;
  private static final PDRectangle PAGE = PDRectangle.A4;
  private static final float MARGIN = 20 * POINTS_PER_MM;

  /**
   * Liberation Sans (SIL Open Font License 1.1), shipped inside the PDFBox jar. It has the Romanian
   * letters ă â î ș ț, and only the glyphs used are embedded.
   */
  private static final byte[] FONT = readFont();

  private QrPdf() {}

  static byte[] render(Attraction attraction, String token) {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage(PAGE);
      document.addPage(page);
      PDType0Font font = PDType0Font.load(document, new ByteArrayInputStream(FONT), true);
      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        float y = PAGE.getHeight() - MARGIN - 28;
        centered(content, font, 28, "Traveler", y);
        y -= 46;
        for (String line : wrap(font, 20, attraction.ro().name())) {
          centered(content, font, 20, line, y);
          y -= 26;
        }
        for (String line : wrap(font, 16, attraction.en().name())) {
          centered(content, font, 16, line, y);
          y -= 21;
        }
        float qrTop = Math.min(y - 8, PAGE.getHeight() - 290);
        drawQr(content, token, (PAGE.getWidth() - QR_SIZE) / 2, qrTop);
        y = qrTop - QR_SIZE - 28;
        centered(content, font, 16, "Version " + attraction.qrVersion(), y);
        centered(content, font, 10, attraction.id().toString(), y - 20);
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Draws the modules as one vector path, so the code stays sharp at any print size. */
  private static void drawQr(PDPageContentStream content, String token, float left, float top)
      throws IOException {
    BitMatrix matrix;
    try {
      matrix =
          new QRCodeWriter()
              .encode(
                  token,
                  BarcodeFormat.QR_CODE,
                  0,
                  0,
                  Map.of(
                      EncodeHintType.ERROR_CORRECTION,
                      ErrorCorrectionLevel.M,
                      EncodeHintType.MARGIN,
                      4));
    } catch (WriterException e) {
      throw new IllegalStateException("Could not encode the QR code", e);
    }
    float module = QR_SIZE / matrix.getWidth();
    for (int row = 0; row < matrix.getHeight(); row++) {
      int column = 0;
      while (column < matrix.getWidth()) {
        if (!matrix.get(column, row)) {
          column++;
          continue;
        }
        int start = column;
        while (column < matrix.getWidth() && matrix.get(column, row)) {
          column++;
        }
        content.addRect(
            left + start * module, top - (row + 1) * module, (column - start) * module, module);
      }
    }
    content.fill();
  }

  private static void centered(
      PDPageContentStream content, PDType0Font font, float size, String text, float y)
      throws IOException {
    String printable = printable(font, text);
    float width = font.getStringWidth(printable) / 1000 * size;
    content.beginText();
    content.setFont(font, size);
    content.newLineAtOffset((PAGE.getWidth() - width) / 2, y);
    content.showText(printable);
    content.endText();
  }

  /** Splits the text into lines that fit between the margins. */
  private static List<String> wrap(PDType0Font font, float size, String text) throws IOException {
    float maxWidth = PAGE.getWidth() - 2 * MARGIN;
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();
    for (String word : printable(font, text).trim().split(" +")) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (!line.isEmpty() && font.getStringWidth(candidate) / 1000 * size > maxWidth) {
        lines.add(line.toString());
        line = new StringBuilder(word);
      } else {
        line = new StringBuilder(candidate);
      }
    }
    lines.add(line.toString());
    return lines;
  }

  /** Replaces line breaks and tabs with spaces, and characters the font lacks with "?". */
  private static String printable(PDType0Font font, String text) {
    StringBuilder out = new StringBuilder();
    text.codePoints()
        .forEach(codePoint -> {
          String character =
              Character.isWhitespace(codePoint) ? " " : Character.toString(codePoint);
          try {
            font.encode(character);
            out.append(character);
          } catch (IOException | IllegalArgumentException e) {
            out.append('?');
          }
        });
    return out.toString();
  }

  private static byte[] readFont() {
    try (InputStream in =
        PDDocument.class.getResourceAsStream(
            "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
      if (in == null) {
        throw new IllegalStateException("Liberation Sans is missing from the PDFBox jar");
      }
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
