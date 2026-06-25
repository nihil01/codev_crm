package com.crm.service;

import com.crm.dto.StudentDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final JavaMailSender mailSender;

    private static final String TEMPLATE_PATH = "certificate_template/certificate-template.pdf";
    private static final String FONT_BOLD_PATH = "fonts/Montserrat-Bold.ttf";
    private static final String FONT_REGULAR_PATH = "fonts/Montserrat-Regular.ttf";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("az-AZ"));

    public byte[] generate(StudentDto studentDto, String certName) {
        validate(studentDto, certName);

        try (
            InputStream templateStream = new ClassPathResource(TEMPLATE_PATH).getInputStream();
            InputStream boldFontStream = new ClassPathResource(FONT_BOLD_PATH).getInputStream();
            InputStream regularFontStream = new ClassPathResource(FONT_REGULAR_PATH).getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PDDocument document = Loader.loadPDF(templateStream.readAllBytes())
        ) {
            PDPage page = document.getPage(0);
            PDRectangle pageSize = page.getMediaBox();
            PDType0Font boldFont = PDType0Font.load(document, boldFontStream, true);
            PDType0Font regularFont = PDType0Font.load(document, regularFontStream, true);

            try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                drawCenteredText(cs, pageSize, boldFont, studentDto.getFirstName() + " " + studentDto.getLastName(), 32, 650, Color.BLACK);
                drawCenteredText(cs, pageSize, boldFont, certName, 24, 520, new Color(0, 93, 170));
                String issueDate = studentDto.getIssueDate().format(DATE_FORMAT);
                drawText(cs, regularFont, issueDate, 14, 140, 330, Color.BLACK);
            }

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Sertifikat yaratmaq mümkün olmadı", e);
        }
    }

    @Async
    public void sendByEmail(StudentDto studentDto, String certName, byte[] pdfBytes) {
        String toEmail = studentDto.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Tələbənin e-poçtu yoxdur, email göndərilmədi: {} {}", studentDto.getFirstName(), studentDto.getLastName());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String studentName = studentDto.getFirstName() + " " + studentDto.getLastName();
            String issueDate = studentDto.getIssueDate() != null
                ? studentDto.getIssueDate().format(DATE_FORMAT)
                : LocalDate.now().format(DATE_FORMAT);

            helper.setFrom("info@codev.az");
            helper.setTo(toEmail);
            helper.setSubject("Sertifikat - Codev");

            String htmlBody = buildEmailHtml(studentName, certName, issueDate);
            helper.setText(htmlBody, true);

            helper.addAttachment("sertifikat.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Sertifikat emaile göndərildi: {} -> {}", studentName, toEmail);

        } catch (Exception e) {
            log.error("Email göndərilmədi: {} {} -> {}", studentDto.getFirstName(), studentDto.getLastName(), toEmail, e);
        }
    }

    private String buildEmailHtml(String studentName, String certName, String issueDate) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f6f9; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; }
                .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 40px 30px; text-align: center; }
                .header h1 { color: #ffffff; margin: 0; font-size: 28px; font-weight: 700; }
                .header p { color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px; }
                .body { padding: 40px 30px; color: #334155; line-height: 1.7; }
                .body h2 { color: #1e293b; font-size: 22px; margin: 0 0 16px; }
                .body p { margin: 0 0 12px; font-size: 15px; }
                .highlight { background: #f0f4ff; border-left: 4px solid #6366f1; padding: 16px 20px; border-radius: 0 8px 8px 0; margin: 24px 0; }
                .highlight strong { color: #6366f1; }
                .footer { background: #f8fafc; padding: 24px 30px; text-align: center; border-top: 1px solid #e2e8f0; }
                .footer p { margin: 0; color: #94a3b8; font-size: 13px; }
                .btn { display: inline-block; background: #6366f1; color: #ffffff; text-decoration: none; padding: 12px 32px; border-radius: 8px; font-weight: 600; margin-top: 16px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🎓 Codev</h1>
                  <p>Təhsil Mərkəzi</p>
                </div>
                <div class="body">
                  <h2>Hörmətli %s,</h2>
                  <p>Sizi təbrik edirik! Siz <strong>%s</strong> kursunu uğurla tamamlamışsınız.</p>
                  <div class="highlight">
                    <p><strong>Sertifikat:</strong> %s</p>
                    <p><strong>Tələbə:</strong> %s</p>
                    <p><strong>Tarix:</strong> %s</p>
                  </div>
                  <p>Sertifikatınız PDF formatında əlavə olunmuşdur. Zəhmət olmasa onu yükləyib saxlayın.</p>
                  <p>Əlavə suallarınız olarsa, bizimlə əlaqə saxlaya bilərsiniz.</p>
                  <p style="margin-top: 24px;">Hörmətlə,<br><strong>Codev Komandası</strong></p>
                </div>
                <div class="footer">
                  <p>© 2026 Codev Təhsil Mərkəzi. Bütün hüquqlar qorunur.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(studentName, certName, certName, studentName, issueDate);
    }

    private void drawText(PDPageContentStream cs, PDFont font, String text, float fontSize, float x, float y, Color color) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawCenteredText(PDPageContentStream cs, PDRectangle pageSize, PDFont font, String text, float fontSize, float y, Color color) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float x = (pageSize.getWidth() - textWidth) / 2f;
        drawText(cs, font, text, fontSize, x, y, color);
    }

    private void validate(StudentDto studentDto, String certificateTitle) {
        if (studentDto == null) throw new IllegalArgumentException("Student məlumatı göndərilməyib");
        if (studentDto.getFirstName() == null || studentDto.getFirstName().isBlank() ||
            studentDto.getLastName() == null || studentDto.getLastName().isBlank()) {
            throw new IllegalArgumentException("Tələbənin adı boş ola bilməz");
        }
        if (certificateTitle == null || certificateTitle.isBlank()) {
            throw new IllegalArgumentException("Sertifikatın adı boş ola bilməz");
        }
    }
}
