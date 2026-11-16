package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.entity.*;
import com.crm.enums.PaymentMethod;
import com.crm.enums.PaymentStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository invoices;
    private final PaymentTransactionRepository transactions;
    private final StudentRepository students;
    private final CourseGroupRepository groups;
    private final StudentFreezePeriodRepository freezePeriods;
    private final GroupStudentRepository memberships;
    private final ChangeAuditService audit;
    private final GroupTeacherRepository groupTeachers;
    private final TeacherShareAllocationRepository shareAllocations;

    public List<PaymentDto> getAll() { return invoices.findAll().stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByStudent(Long studentId) { return invoices.findByStudentId(studentId).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByGroup(Long groupId) { return invoices.findByGroupId(groupId).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByPeriod(Integer year, Integer month) { return invoices.findByPaymentYearAndPaymentMonth(year, month).stream().map(this::toDto).toList(); }
    public List<PaymentDto> getByGroupAndPeriod(Long id, Integer year, Integer month) {
        return invoices.findByGroupAndPeriod(id, year, month).stream().map(this::toDto).toList();
    }
    public List<PaymentDto> getOverdue(Integer year, Integer month) {
        return getByPeriod(year, month).stream()
            .filter(p -> p.getStatus() == PaymentStatus.GECIKIB)
            .filter(p -> groups.findById(p.getGroupId())
                .map(g -> g.getStatus() != com.crm.enums.GroupStatus.DAYANDIRILMIS).orElse(false))
            .toList();
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
        return acceptLocked(p, due, method, null);
    }

    @Transactional
    public PaymentDto recordPartial(Long id, BigDecimal amount, PaymentMethod method, String requestKey) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        if(amount==null || method==null)
            throw new IllegalArgumentException("Ödəniş məbləği və üsulu tələb olunur");
        String key = requestKey == null || requestKey.isBlank() ? null : requestKey.trim();
        if(key != null && (key.length()<12 || key.length()>128 || !key.matches("[A-Za-z0-9_-]+")))
            throw new IllegalArgumentException("Idempotency-Key 12–128 simvoldan ibarət olmalıdır");
        if (key != null) {
            var previous=transactions.findByPaymentIdAndIdempotencyKey(id,key);
            if(previous.isPresent()) {
                PaymentTransaction tx=previous.get();
                if(tx.getAmount().compareTo(amount)!=0 || tx.getMethod()!=method)
                    throw new IllegalArgumentException("Bu sorğu açarı başqa məbləğ və ya üsulla işlənib");
                return toDto(p);
            }
        }
        return acceptLocked(p, amount, method, key);
    }

    private PaymentDto acceptLocked(Payment p, BigDecimal amount, PaymentMethod method, String key) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Ödəniş məbləği düzgün deyil");
        if (method == null) throw new IllegalArgumentException("Ödəniş üsulu tələb olunur");
        PaymentDto before = toDto(p);
        if (p.isFrozen() || p.getStatus() == PaymentStatus.BAGISLANIB ||
            freezePeriods.existsByStudentIdAndYearAndMonth(
                p.getStudent().getId(),p.getPaymentYear(),p.getPaymentMonth()))
            throw new IllegalArgumentException("Dondurulmuş/bağışlanmış ödənişi qəbul etmək olmaz");
        BigDecimal rest = remaining(p);
        if (amount.compareTo(rest) > 0) throw new IllegalArgumentException("Ödəniş qalıq borcdan çoxdur");
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentTransaction transaction = transactions.saveAndFlush(PaymentTransaction.builder()
            .payment(p).amount(amount).method(method)
            .paidAt(LocalDateTime.now(ZoneId.of("Asia/Baku")))
            .recordedBy(actor).idempotencyKey(key).build());
        for (GroupTeacher assignment : groupTeachers.findByGroupId(p.getGroup().getId())) {
            YearMonth since=YearMonth.of(assignment.getEffectiveYear(),assignment.getEffectiveMonth());
            YearMonth paid=YearMonth.from(transaction.getPaidAt());
            if(since.isAfter(paid)) continue;
            BigDecimal share=amount.multiply(assignment.getSalaryPercentage())
                .divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
            if(share.signum()<=0) continue;
            TeacherShareAllocation allocation=shareAllocations.save(TeacherShareAllocation.builder()
                .paymentTransaction(transaction).groupTeacher(assignment)
                .teacher(assignment.getTeacher()).group(p.getGroup())
                .percentage(assignment.getSalaryPercentage()).amount(share).build());
            audit.record("ALLOCATE","TEACHER_SHARE",allocation.getId(),null,
                java.util.Map.of("paymentId",p.getId(),"teacherId",assignment.getTeacher().getId(),
                    "percentage",assignment.getSalaryPercentage(),"amount",share));
        }
        p.setPaymentDate(LocalDate.now(ZoneId.of("Asia/Baku")));
        p.setPaymentMethod(method);
        p.setStatus(amount.compareTo(rest) == 0 ? PaymentStatus.ODENILIB : PaymentStatus.GOZLENILIR);
        invoices.save(p);
        PaymentDto updated=toDto(p);
        audit.record("PAYMENT_RECEIVED","PAYMENT",p.getId(),before,updated);
        audit.record("CREATE","PAYMENT_TRANSACTION",transaction.getId(),null,
            java.util.Map.of("paymentId",p.getId(),"amount",amount,"method",method.name(),
                "recordedBy",actor,"paidAt",transaction.getPaidAt()));
        return updated;
    }

    @Transactional
    public PaymentDto discount(Long id, BigDecimal value, String reason) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        PaymentDto before = toDto(p);
        if (p.isFrozen()) throw new IllegalArgumentException("Dondurulmuş ayda endirim olmaz");
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
        PaymentDto updated = toDto(invoices.save(p));
        audit.record("DISCOUNT","PAYMENT",id,before,updated);
        return updated;
    }

    @Transactional
    public PaymentDto freeze(Long id, boolean freeze) {
        Payment p = invoices.lockById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        PaymentDto before = toDto(p);
        if (!freeze && freezePeriods.existsByStudentIdAndYearAndMonth(
            p.getStudent().getId(),p.getPaymentYear(),p.getPaymentMonth()))
            throw new IllegalArgumentException("Tələbənin ayı dondurulub; əvvəl ayın dondurmasını ləğv edin");
        if (freeze && collectedForInvoice(id).signum() > 0)
            throw new IllegalArgumentException("Ödənilmiş ayı dondurmaq olmaz");
        p.setFrozen(freeze);
        p.setStatus(freeze ? PaymentStatus.DONDURULUB : PaymentStatus.GOZLENILIR);
        PaymentDto updated = toDto(invoices.save(p));
        audit.record(freeze?"FREEZE":"UNFREEZE","PAYMENT",id,before,updated);
        return updated;
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
        throw new IllegalArgumentException("Ödəniş statusu avtomatik hesablanır; bu əməliyyat dəstəklənmir");
    }

    @Transactional
    public PaymentDto create(PaymentDto dto) {
        Student student = students.findById(dto.getStudentId()).orElseThrow(() -> new IllegalArgumentException("Tələbə tapılmadı"));
        CourseGroup group = groups.findById(dto.getGroupId()).orElseThrow(() -> new IllegalArgumentException("Qrup tapılmadı"));
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0 || dto.getAmount().scale() > 2)
            throw new IllegalArgumentException("Aylıq məbləğ düzgün deyil");
        if (memberships.findByGroupIdAndStudentId(group.getId(),student.getId()).isEmpty())
            throw new IllegalArgumentException("Tələbə seçilmiş qrupda qeydiyyatda deyil");
        YearMonth period = YearMonth.of(dto.getPaymentYear(), dto.getPaymentMonth());
        boolean exists = invoices.findByStudentIdAndGroupId(student.getId(),group.getId()).stream()
            .anyMatch(x -> x.getPaymentYear().equals(period.getYear()) &&
                x.getPaymentMonth().equals(period.getMonthValue()));
        if (exists) throw new IllegalArgumentException("Bu tələbə və ay üçün hesab artıq mövcuddur");
        LocalDate due = dto.getDueDate() != null ? dto.getDueDate()
            : period.atDay(Math.min(period.lengthOfMonth(), group.getStartDate().getDayOfMonth()));
        boolean isFrozen=freezePeriods.existsByStudentIdAndYearAndMonth(student.getId(),period.getYear(),period.getMonthValue());
        Payment p = Payment.builder().student(student).group(group)
            .paymentYear(period.getYear()).paymentMonth(period.getMonthValue())
            .amount(dto.getAmount()).dueDate(due)
            .discountAmount(BigDecimal.ZERO).frozen(isFrozen)
            .status(isFrozen?PaymentStatus.DONDURULUB:PaymentStatus.GOZLENILIR)
            .note(dto.getNote()).build();
        PaymentDto created=toDto(invoices.save(p));
        audit.record("CREATE","PAYMENT",created.getId(),null,created);
        return created;
    }

    @Transactional
    public void delete(Long id) {
        if (collectedForInvoice(id).signum() != 0)
            throw new IllegalArgumentException("Mədaxili olan hesabı silmək olmaz");
        Payment p = invoices.findById(id).orElseThrow(() -> new IllegalArgumentException("Ödəniş tapılmadı"));
        PaymentDto before = toDto(p);
        invoices.delete(p);
        audit.record("DELETE","PAYMENT",id,before,null);
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
        dto.setStudentPhone(p.getStudent().getPhone());
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
