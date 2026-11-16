package com.crm.controller;

import com.crm.dto.StudentDto;
import com.crm.entity.Student;
import com.crm.repository.StudentRepository;
import com.crm.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final StudentRepository studentRepository;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generate(@RequestBody Map<String, Object> body) {
        StudentDto studentDto = new StudentDto();

        // Get student details from DB if studentId provided
        Number studentId = (Number) body.get("studentId");
        String email = null;
        if (studentId != null) {
            Optional<Student> studentOpt = studentRepository.findById(studentId.longValue());
            if (studentOpt.isPresent()) {
                Student s = studentOpt.get();
                studentDto.setFirstName(s.getFirstName());
                studentDto.setLastName(s.getLastName());
                email = s.getEmail();
            }
        } else {
            studentDto.setFirstName((String) body.get("firstName"));
            studentDto.setLastName((String) body.get("lastName"));
            email = (String) body.get("email");
        }
        studentDto.setEmail(email);

        String certName = (String) body.get("certName");
        String issueDateStr = (String) body.get("issueDate");

        if (issueDateStr != null) {
            studentDto.setIssueDate(LocalDate.parse(issueDateStr));
        } else {
            studentDto.setIssueDate(LocalDate.now());
        }

        byte[] pdf = certificateService.generate(studentDto, certName);

        // Send email asynchronously
        certificateService.sendByEmail(studentDto, certName, pdf);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sertifikat.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
