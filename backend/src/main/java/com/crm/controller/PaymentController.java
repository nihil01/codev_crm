package com.crm.controller;

import com.crm.dto.PaymentDto;
import com.crm.enums.PaymentMethod;
import com.crm.enums.PaymentStatus;
import com.crm.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import com.crm.entity.PaymentTransaction;
import com.crm.service.PaymentReceiptService;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentReceiptService receiptService;

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PaymentDto>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(paymentService.getByStudent(studentId));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<PaymentDto>> getByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(paymentService.getByGroup(groupId));
    }

    @GetMapping("/group/{groupId}/year/{year}/month/{month}")
    public ResponseEntity<List<PaymentDto>> getByGroupAndPeriod(
            @PathVariable Long groupId, @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(paymentService.getByGroupAndPeriod(groupId, year, month));
    }

    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<List<PaymentDto>> getByPeriod(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(paymentService.getByPeriod(year, month));
    }

    @GetMapping("/overdue/year/{year}/month/{month}")
    public ResponseEntity<List<PaymentDto>> getOverdue(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(paymentService.getOverdue(year, month));
    }

    @GetMapping("/collected/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getCollected(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(paymentService.getCollectedForMonth(year, month));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentDto> recordPayment(
            @PathVariable Long id,
            @RequestParam PaymentMethod method) {
        return ResponseEntity.ok(paymentService.recordPayment(id, method));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDto> updateStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updateStatus(id, status));
    }

    @PostMapping("/{id}/partial")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentDto> recordPartial(@PathVariable Long id,
            @RequestParam BigDecimal amount, @RequestParam PaymentMethod method) {
        return ResponseEntity.ok(paymentService.recordPartial(id, amount, method));
    }

    @PutMapping("/{id}/discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDto> discount(@PathVariable Long id,
            @RequestParam BigDecimal amount, @RequestParam String reason) {
        return ResponseEntity.ok(paymentService.discount(id, amount, reason));
    }

    @PutMapping("/{id}/freeze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentDto> freeze(@PathVariable Long id, @RequestParam boolean frozen) {
        return ResponseEntity.ok(paymentService.freeze(id, frozen));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Map<String, Object>>> transactions(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.transactions(id).stream().map(t -> Map.<String,Object>of(
            "id", t.getId(), "amount", t.getAmount(), "method", t.getMethod(),
            "paidAt", t.getPaidAt(), "recordedBy", t.getRecordedBy())).toList());
    }

    @GetMapping("/transactions/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        byte[] pdf = receiptService.generate(paymentService.transaction(id));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=odenis-qebzi.pdf").contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @GetMapping("/reminders")
    public ResponseEntity<List<Map<String,Object>>> reminders() {
        LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Baku"));
        return ResponseEntity.ok(paymentService.getAll().stream()
            .filter(p -> p.getDueDate() != null && !p.getDueDate().isAfter(now)
                && p.getRemainingAmount().signum() > 0
                && !p.isFrozen() && p.getStatus() != PaymentStatus.DONDURULUB)
            .map(p -> Map.<String,Object>of(
                "paymentId", p.getId(), "student", p.getStudentFirstName()+" "+p.getStudentLastName(),
                "dueDate", p.getDueDate(), "remaining", p.getRemainingAmount(),
                "message", "Salam! Codev üzrə "+p.getRemainingAmount()+" AZN ödəniş qalığınız var. Zəhmət olmasa ödənişi yoxlayın."
            )).toList());
    }

    @PostMapping("/mark-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markOverdue() {
        paymentService.markOverdue();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentDto> create(@RequestBody PaymentDto dto) {
        return ResponseEntity.ok(paymentService.create(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.ok().build();
    }
}
