package ru.schoolsun.certificate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.format.DateTimeFormatter;

@Controller
public class CertificateController {

    private final CertificateService certificates;

    public CertificateController(CertificateService certificates) {
        this.certificates = certificates;
    }

    @GetMapping("/certificates/{serial}")
    public String view(@PathVariable String serial, Model model) {
        Certificate cert = certificates.bySerial(serial);
        model.addAttribute("cert", cert);
        model.addAttribute("date", cert.getIssuedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        return "certificate";
    }

    @GetMapping("/certificates/{serial}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String serial) {
        Certificate cert = certificates.bySerial(serial);
        byte[] pdf = certificates.renderPdf(cert);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"SCHOOLSUN-" + cert.getSerialNumber() + ".pdf\"")
                .body(pdf);
    }
}
