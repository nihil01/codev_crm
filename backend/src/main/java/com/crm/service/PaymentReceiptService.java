package com.crm.service;

import com.crm.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {
    public byte[] generate(PaymentTransaction tx) {
        try (PDDocument doc = new PDDocument();
             InputStream fontStream = new ClassPathResource("fonts/Montserrat-Regular.ttf").getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType0Font font = PDType0Font.load(doc, fontStream, true);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                line(cs,font,20,55,760,"CODEV — ÖDƏNİŞ QƏBZİ");
                line(cs,font,12,55,705,"Qəbz №: " + tx.getId());
                line(cs,font,12,55,682,"Tələbə: " + tx.getPayment().getStudent().getFirstName() + " " + tx.getPayment().getStudent().getLastName());
                line(cs,font,12,55,659,"Qrup: " + tx.getPayment().getGroup().getGroupNumber());
                line(cs,font,12,55,636,"Ödəniş: " + tx.getAmount() + " AZN");
                line(cs,font,12,55,613,"Üsul: " + tx.getMethod().name());
                line(cs,font,12,55,590,"Tarix: " + tx.getPaidAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception ex) { throw new IllegalStateException("Qəbz yaratmaq mümkün olmadı", ex); }
    }
    private void line(PDPageContentStream cs, PDType0Font font, int size, int x, int y, String text)
        throws java.io.IOException {
        cs.beginText(); cs.setFont(font,size); cs.newLineAtOffset(x,y); cs.showText(text); cs.endText();
    }
}
