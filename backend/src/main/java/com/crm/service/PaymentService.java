package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.entity.*;
import com.crm.enums.PaymentMethod;
import com.crm.enums.PaymentStatus;
import com.crm.enums.StudentStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository invoices;
    private final PaymentTransactionRepository transactions;
    private final StudentRepository students;
    private final CourseGroupRepository groups;

    public List<PaymentDto> getAll() { return invoices.findAll().stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByStudent(Long studentId) { return invoices.findByStudentId(studentId).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByGroup(Long groupId) { return invoices.findByGroupId(groupId).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByPeriod(Integer year, Integer month) { return invoices.findByPaymentYearAndPaymentMonth(year, month).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByGroupAndPeriod(Long id, Integer year, Integer month) {
        return invoices.findByGroupAndPeriod(id, year, month).stream().map(this::toDto).toList();
    }
    public List<PaymentDto> getOverdue(Integer year, Integer month) {
        return getByPeriod(year, month).stream().filter(p -> p.getStatus() == PaymentStatus.GECIKIB).toList();
    }
    public BigDecimal getCollectedForMonth(Integer year, Integer month) {
        YearMonth period = YearMonth.of(year, month);
        return transactions.sumCollectedForPeriod(period.atDay(1).atStartOfDay(), period.plusMonths(1).atDay(1).atStartOfDay());
    }
    public BigDecimal getCollectedForGroupMonth(Long groupId, Integer year, Integer month) {
        YearMonth period = YearMonth.of(year, month);
        return transactions.sumCollectedForGroupPeriod(groupId, period.atDay(1).atStartOfDay(), period.plusMonths(1).atDay(1).atStartOfDay());
    }

    public BigDecimal collectedForInvoice(Long id) {
        BigDecimal amount = transactions.sumForInvoice(id);
        return amount == null ? BigDecimal.ZERO : amount;
    }
    public BigDecimal remaining(Payment p) {
        if (p.isFrozen() || p.getStatus() == PaymentStatus.BAGISLANIB) return BigDecimal.ZERO;
        BigDecimal discount = p.getDiscountAmount() == null ? BigDecimal.ZERO : p.getDiscountAmount();
        return p.getAmount().subtract(discount).subtract(collectedForInvoice(p.getId())).max(BigDecimal.ZERO);
    }

    @Transactional
    public PaymentDto recordPayment(Long id, PaymentMethod method) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        BigDecimal due = remaining(p);
        if (due.signum() <= 0) throw new IllegalArgumentException("Qalıq borc yoxdur");
        return acceptLocked(p, due, method);
    }

    @Transactional
    public PaymentDto recordPartial(Long id, BigDecimal amount, PaymentMethod method) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        return acceptLocked(p, amount, method);
    }

    private PaymentDto acceptLocked(Payment p, BigDecimal amount, PaymentMethod method) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Ödəniş məbləği düzgün deyil");
        if (method == null) throw new IllegalArgumentException("Ödəniş üsulu tələb olunur");
        if (p.isFrozen() || p.getStatus() == PaymentStatus.BAGISLANIB ||
            p.getStudent().getStatus() == StudentStatus.DONDURULUB)
            throw new IllegalArgumentException("Dondurulmuş/bağışlanmış ödənişi qəbul etmək olmaz");
        BigDecimal rest = remaining(p);
        if (amount.compareTo(rest) > 0) throw new IllegalArgumentException("Ödəniş qalıq borcdan çoxdur");
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        transactions.saveAndFlush(PaymentTransaction.builder()
            .payment(p).amount(amount).method(method).paidAt(LocalDateTime.now()).recordedBy(actor).build());
        p.setPaymentDate(LocalDate.now());
        p.setPaymentMethod(method);
        p.setStatus(amount.compareTo(rest) == 0 ? PaymentStatus.ODENILIB : PaymentStatus.GOZLENILIR);
        invoices.save(p);
        return toDto(p);
    }

    @Transactional
    public PaymentDto discount(Long id, BigDecimal value, String reason) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        if (value == null || value.signum() < 0 || value.scale() > 2)
            throw new IllegalArgumentException("Endirim düzgün deyil");
        if (value.signum() > 0 && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException("Endirim səbəbi məcburidir");
        if (value.compareTo(p.getAmount().subtract(collectedForInvoice(id))) > 0)
            throw new IllegalArgumentException("Endirim ödənilməmiş məbləğdən böyükdür");
        p.setDiscountAmount(value);
        p.setDiscountReason(value.signum() == 0 ? null : reason.trim());
        if (remaining(p).signum() == 0) p.setStatus(PaymentStatus.ODENILIB);
        else p.setStatus(PaymentStatus.GOZLENILIR);
        return toDto(invoices.save(p));
    }

    @Transactional
    public PaymentDto freeze(Long id, boolean freeze) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        if (freeze && collectedForInvoice(id).signum() > 0)
            throw new IllegalArgumentException("Ödənilmiş ayı dondurmaq olmaz");
        p.setFrozen(freeze);
        p.setStatus(freeze ? PaymentStatus.DONDURULUB : PaymentStatus.GOZLENILIR);
        return toDto(invoices.save(p));
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Baku")
    @Transactional
    public void markOverdue() {
        for (Payment p : invoices.findAll()) {
            if (p.isFrozen() || p.getStatus() == PaymentStatus.BAGISLANIB) continue;
            BigDecimal rest = remaining(p);
            PaymentStatus next = rest.signum() == 0 ? PaymentStatus.ODENILIB
                : overdue(p) ? PaymentStatus.GECIKIB : PaymentStatus.GOZLENILIR;
            if (next != p.getStatus()) { p.setStatus(next); invoices.save(p); }
        }
    }
    public boolean overdue(Payment p) {
        if (p.getDueDate() == null || p.isFrozen()) return false;
        return LocalDate.now(ZoneId.of("Asia/Baku")).isAfter(p.getDueDate().plusDays(5));
    }

    @Transactional
    public PaymentDto updateStatus(Long id, PaymentStatus status) {
        if (status == PaymentStatus.ODENILIB || status == PaymentStatus.BAGISLANIB || status == PaymentStatus.DONDURULUB)
            throw new IllegalArgumentException("Bu statusu birbaşa dəyişmək olmaz. Ödəniş və ya dondurma istifadə edin");
        Payment p = invoices.findById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        p.setStatus(status);
        return toDto(invoices.save(p));
    }

    @Transactional
    public PaymentDto create(PaymentDto dto) {
        Student student = students.findById(dto.getStudentId()).orElseThrow(() -> new IllegalArgumentException("Tələbə tapılmadı"));
        CourseGroup group = groups.findById(dto.getGroupId()).orElseThrow(() -> new IllegalArgumentException("Qrup tapılmadı"));
        YearMonth period = YearMonth.of(dto.getPaymentYear(), dto.getPaymentMonth());
        LocalDate due = dto.getDueDate() != null ? dto.getDueDate()
            : period.atDay(Math.min(period.lengthOfMonth(), group.getStartDate().getDayOfMonth()));
        Payment p = Payment.builder().student(student).group(group)
            .paymentYear(period.getYear()).paymentMonth(period.getMonthValue())
            .amount(dto.getAmount()).dueDate(due)
            .discountAmount(BigDecimal.ZERO).frozen(false).status(PaymentStatus.GOZLENILIR)
            .note(dto.getNote()).build();
        return toDto(invoices.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (collectedForInvoice(id).signum() != 0)
            throw new IllegalArgumentException("Mədaxili olan hesabı silmək olmaz");
        invoices.deleteById(id);
    }

    public PaymentDto toDto(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.setId(p.getId()); dto.setStudentId(p.getStudent().getId()); dto.setGroupId(p.getGroup().getId());
        dto.setPaymentYear(p.getPaymentYear()); dto.setPaymentMonth(p.getPaymentMonth()); dto.setAmount(p.getAmount());
        dto.setDueDate(p.getDueDate()); dto.setPaymentDate(p.getPaymentDate()); dto.setPaymentMethod(p.getPaymentMethod());
        dto.setNote(p.getNote()); dto.setFrozen(p.isFrozen()); dto.setDiscountAmount(p.getDiscountAmount());
        dto.setDiscountReason(p.getDiscountReason());
        BigDecimal paid = collectedForInvoice(p.getId());
        dto.setPaidAmount(paid); dto.setRemainingAmount(remaining(p));
        dto.setStatus(p.isFrozen() ? PaymentStatus.DONDURULUB : dto.getRemainingAmount().signum() == 0
            ? PaymentStatus.ODENILIB : overdue(p) ? PaymentStatus.GECIKIB : PaymentStatus.GOZLENILIR);
        dto.setStudentFirstName(p.getStudent().getFirstName()); dto.setStudentLastName(p.getStudent().getLastName());
        dto.setGroupNumber(p.getGroup().getGroupNumber());
        return dto;
    }

    public List<PaymentTransaction> transactions(Long id) {
        return transactions.findByPaymentIdOrderByPaidAtDesc(id);
    }
    public PaymentTransaction transaction(Long id) {
        return transactions.findById(id).orElseThrow(() -> new IllegalArgumentException("Qəbz tapılmadı"));
    }
}
