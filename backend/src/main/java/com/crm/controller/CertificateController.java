package com.crm.controller;

import com.crm.dto.StudentDto;
import com.crm.entity.Student;
import com.crm.entity.CourseGroup;
import com.crm.entity.CertificateIssuance;
import com.crm.enums.GroupStatus;
import com.crm.repository.StudentRepository;
import com.crm.repository.CourseGroupRepository;
import com.crm.repository.GroupStudentRepository;
import com.crm.repository.PaymentRepository;
import com.crm.repository.CertificateIssuanceRepository;
import com.crm.service.ChangeAuditService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.ZoneId;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final CertificateIssuanceRepository issued;
    private final ChangeAuditService audit;

    @PostMapping("/generate")
    @Transactional
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
        LocalDate issueDate=request.get("issueDate")==null?
            LocalDate.now(ZoneId.of("Asia/Baku")):LocalDate.parse((String)request.get("issueDate"));
        if(issueDate.isAfter(LocalDate.now(ZoneId.of("Asia/Baku"))))
            throw new IllegalArgumentException("Sertifikat tarixi gələcəkdə ola bilməz");
        var previous=issued.findByStudentIdAndGroupId(studentId,groupId);
        boolean firstIssue=previous.isEmpty();
        CertificateIssuance record;
        if(firstIssue) {
            String actor=SecurityContextHolder.getContext().getAuthentication().getName();
            record=issued.saveAndFlush(CertificateIssuance.builder()
                .student(student).group(group).certificateTitle(title.trim())
                .issuedOn(issueDate).issuedBy(actor).build());
            audit.record("ISSUE","CERTIFICATE",record.getId(),null,
                Map.of("studentId",studentId,"groupId",groupId,
                    "title",record.getCertificateTitle(),"issuedOn",record.getIssuedOn(),
                    "issuedBy",record.getIssuedBy()));
        } else {
            record=previous.get();
        }

        StudentDto dto=new StudentDto();
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setIssueDate(record.getIssuedOn());
        String finalTitle=record.getCertificateTitle();
        byte[] pdf=certificates.generate(dto,finalTitle);
        if(firstIssue && student.getEmail()!=null&&!student.getEmail().isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    certificates.sendByEmail(dto,finalTitle,pdf);
                }
            });
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=sertifikat.pdf")
            .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
