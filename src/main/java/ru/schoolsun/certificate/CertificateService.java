package ru.schoolsun.certificate;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.Course;
import ru.schoolsun.user.User;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class CertificateService {

    private static final List<String> FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/calibri.ttf",
            "C:/Windows/Fonts/times.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");

    private final CertificateRepository certificates;
    private final SecureRandom random = new SecureRandom();

    public CertificateService(CertificateRepository certificates) {
        this.certificates = certificates;
    }

    /** Выдаёт сертификат, если его ещё нет. */
    public Certificate issueIfNeeded(User student, Course course) {
        if (certificates.existsByCourseIdAndStudentId(course.getId(), student.getId())) {
            return null;
        }
        Certificate c = new Certificate();
        c.setCourse(course);
        c.setStudent(student);
        c.setSerialNumber(nextSerial());
        c.setIssuedAt(LocalDateTime.now());
        return certificates.save(c);
    }

    public Certificate byId(Long id) {
        return certificates.findById(id).orElseThrow(() -> new NotFoundException("Сертификат не найден"));
    }

    public Certificate bySerial(String serial) {
        return certificates.findBySerialNumber(serial)
                .orElseThrow(() -> new NotFoundException("Сертификат не найден"));
    }

    public List<Certificate> ofStudent(Long studentId) {
        return certificates.findByStudentIdOrderByIssuedAtDesc(studentId);
    }

    private String nextSerial() {
        byte[] raw = new byte[3];
        random.nextBytes(raw);
        return "SCHOOLSUN-" + LocalDateTime.now().getYear() + "-"
                + HexFormat.of().withUpperCase().formatHex(raw);
    }

    /** PDF-сертификат с фирменным оформлением SCHOOLSUN. */
    public byte[] renderPdf(Certificate cert) {
        String studentName = cert.getStudent().getFullName();
        String courseTitle = cert.getCourse().getTitle();
        String date = cert.getIssuedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont font = loadFont();
            Font gold = new Font(font, 34, Font.BOLD, new Color(196, 124, 20));
            Font title = new Font(font, 26, Font.BOLD, new Color(27, 42, 74));
            Font body = new Font(font, 16, Font.NORMAL, new Color(60, 74, 106));
            Font small = new Font(font, 11, Font.NORMAL, new Color(122, 134, 160));

            // рамка
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(new Color(255, 179, 71));
            cb.setLineWidth(4);
            cb.rectangle(30, 30, doc.getPageSize().getWidth() - 60, doc.getPageSize().getHeight() - 60);
            cb.stroke();

            // солнце
            cb.setColorFill(new Color(255, 217, 61));
            cb.circle(90, doc.getPageSize().getHeight() - 90, 26);
            cb.fill();
            cb.setColorStroke(new Color(255, 140, 66));
            cb.setLineWidth(3);
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                float x1 = (float) (90 + 34 * Math.cos(angle));
                float y1 = (float) (doc.getPageSize().getHeight() - 90 + 34 * Math.sin(angle));
                float x2 = (float) (90 + 44 * Math.cos(angle));
                float y2 = (float) (doc.getPageSize().getHeight() - 90 + 44 * Math.sin(angle));
                cb.moveTo(x1, y1);
                cb.lineTo(x2, y2);
                cb.stroke();
            }

            Paragraph p = new Paragraph("SCHOOLSUN — школа солнца", gold);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(70);
            doc.add(p);

            p = new Paragraph("СЕРТИФИКАТ", title);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(24);
            doc.add(p);

            p = new Paragraph("награждается", body);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(18);
            doc.add(p);

            p = new Paragraph(studentName, title);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(10);
            doc.add(p);

            Paragraph courseLine = new Paragraph(new Phrase("за успешное прохождение курса\n«" + courseTitle + "»", body));
            courseLine.setAlignment(Element.ALIGN_CENTER);
            courseLine.setSpacingBefore(14);
            doc.add(courseLine);

            p = new Paragraph("Дата выдачи: " + date + "    ·    Серийный номер: " + cert.getSerialNumber(), small);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(40);
            doc.add(p);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сформировать сертификат", e);
        }
    }

    private BaseFont loadFont() throws IOException {
        for (String candidate : FONT_CANDIDATES) {
            Path p = Path.of(candidate);
            if (Files.exists(p)) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        // последний шанс — встроенный шрифт (без кириллицы)
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
    }
}
