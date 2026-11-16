package com.crm.controller;

import com.crm.dto.StudentDto;
import com.crm.entity.Student;
import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import com.crm.repository.StudentRepository;
import com.crm.repository.CourseGroupRepository;
import com.crm.repository.GroupStudentRepository;
import com.crm.repository.PaymentRepository;
import com.crm.service.PaymentService;
import com.crm.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CertificateController {
    private final CertificateService certificates;
    private final StudentRepository students;
    private final CourseGroupRepository groups;
    private final GroupStudentRepository memberships;
    private final PaymentRepository invoices;
    private final PaymentService payments;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody Map<String,Object> request) {
        if(!(request.get("studentId") instanceof Number) ||
           !(request.get("groupId") instanceof Number))
            throw new IllegalArgumentException("Tələbə və tamamlanmış qrup seçilməlidir");
        long studentId=((Number)request.get("studentId")).longValue();
        long groupId=((Number)request.get("groupId")).longValue();
        Student student=students.findById(studentId)
            .orElseThrow(()->new IllegalArgumentException("Tələbə tapılmadı"));
        CourseGroup group=groups.findById(groupId)
            .orElseThrow(()->new IllegalArgumentException("Qrup tapılmadı"));
        if(group.getStatus()!=GroupStatus.TAMAMLANMIS)
            throw new IllegalArgumentException("Qrup hələ tamamlanmayıb");
        if(memberships.findByGroupIdAndStudentId(groupId,studentId).isEmpty())
            throw new IllegalArgumentException("Tələbə bu qrupda deyil");
        BigDecimal debt=invoices.findByStudentIdAndGroupId(studentId,groupId).stream()
            .map(payments::remaining).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(debt.signum()>0)
            throw new IllegalArgumentException("Tələbənin bu qrup üzrə borcu var: "+debt+" AZN");

        String title=(String)request.get("certName");
        if(title==null || title.isBlank())
            title=group.getCourse()==null?group.getGroupNumber():group.getCourse().getName();
        LocalDate issued=request.get("issueDate")==null?LocalDate.now():LocalDate.parse((String)request.get("issueDate"));
        if(issued.isAfter(LocalDate.now()))throw new IllegalArgumentException("Sertifikat tarixi gələcəkdə ola bilməz");

        StudentDto dto=new StudentDto();
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setIssueDate(issued);
        byte[] pdf=certificates.generate(dto,title);
        if(student.getEmail()!=null&&!student.getEmail().isBlank())
            certificates.sendByEmail(dto,title,pdf);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=sertifikat.pdf")
            .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
