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
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
    public ResponseEntity<BigDecimal> getCollected(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(paymentService.getCollectedForMonth(year, month));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/mark-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markOverdue() {
        paymentService.markOverdue();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
