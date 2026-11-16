package com.crm.service;

import com.crm.entity.Payment;
import com.crm.entity.ReminderDispatch;
import com.crm.enums.GroupStatus;
import com.crm.enums.ReminderDispatchStatus;
import com.crm.repository.PaymentRepository;
import com.crm.repository.ReminderDispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReminderQueueService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Baku");
    private static final String OVERDUE_KIND="OVERDUE_DAY6";

    private final PaymentRepository invoices;
    private final PaymentService payments;
    private final ReminderDispatchRepository reminders;
    private final ChangeAuditService audit;

    public record DispatchPayload(Long id,String phone,String message,String key) {}

    @Transactional
    public int enqueueOverdue() {
        LocalDate today=LocalDate.now(ZONE);
        int count=0;
        for(Payment payment:invoices.findAll()) {
            if(payment.getDueDate()==null ||
               !today.isAfter(payment.getDueDate().plusDays(5)) ||
               payment.isFrozen() ||
               payment.getGroup().getStatus()==GroupStatus.DAYANDIRILMIS ||
               !payment.getStudent().isReminderConsent() ||
               payment.getStudent().isArchived()) continue;
            String phone=payment.getStudent().getPhone();
            if(phone==null || phone.isBlank() || payments.remaining(payment).signum()<=0 ||
               reminders.existsByPaymentIdAndKind(payment.getId(),OVERDUE_KIND))continue;
            String message="Salam! Codev üzrə "+payments.remaining(payment)+
                " AZN ödəniş qalığınız var. Zəhmət olmasa ödənişi yoxlayın.";
            ReminderDispatch queued=reminders.saveAndFlush(ReminderDispatch.builder()
                .payment(payment).kind(OVERDUE_KIND).phone(phone.trim())
                .message(message).idempotencyKey(UUID.randomUUID().toString())
                .status(ReminderDispatchStatus.PENDING).attempts(0).build());
            audit.record("QUEUE","PAYMENT_REMINDER",queued.getId(),null,
                Map.of("invoiceId",payment.getId(),"status","PENDING","kind",OVERDUE_KIND));
            count++;
        }
        return count;
    }

    @Transactional
    public void recoverStaleSending() {
        LocalDateTime cutOff=LocalDateTime.now(ZONE).minusMinutes(45);
        for(ReminderDispatch item:reminders.findByStatusAndLastAttemptAtBefore(
            ReminderDispatchStatus.SENDING,cutOff)){
            item.setStatus(ReminderDispatchStatus.FAILED);
            item.setLastError("Delivery interrupted; retry uses same idempotency key");
            reminders.save(item);
        }
    }

    @Transactional(readOnly=true)
    public List<ReminderDispatch> pendingBatch() {
        return reminders.findByStatusInAndAttemptsLessThanOrderByIdAsc(
            List.of(ReminderDispatchStatus.PENDING,ReminderDispatchStatus.FAILED),3,
            PageRequest.of(0,100));
    }

    @Transactional
    public Optional<DispatchPayload> claim(Long id) {
        ReminderDispatch entry=reminders.lockById(id).orElse(null);
        if(entry==null || (entry.getStatus()!=ReminderDispatchStatus.PENDING &&
             entry.getStatus()!=ReminderDispatchStatus.FAILED) || entry.getAttempts()>=3)
            return Optional.empty();
        // Recheck current consent and outstanding debt immediately before dispatch.
        Payment payment=entry.getPayment();
        if(!payment.getStudent().isReminderConsent() || payment.isFrozen() ||
           payment.getGroup().getStatus()==GroupStatus.DAYANDIRILMIS ||
           payments.remaining(payment).signum()<=0) {
            entry.setStatus(ReminderDispatchStatus.SENT);
            entry.setLastError("SKIPPED: no outstanding consented debt");
            reminders.save(entry);
            return Optional.empty();
        }
        entry.setStatus(ReminderDispatchStatus.SENDING);
        entry.setAttempts(entry.getAttempts()+1);
        entry.setLastAttemptAt(LocalDateTime.now(ZONE));
        reminders.saveAndFlush(entry);
        return Optional.of(new DispatchPayload(
            entry.getId(),entry.getPhone(),entry.getMessage(),entry.getIdempotencyKey()));
    }

    @Transactional
    public void finish(Long id,boolean sent,String safeErrorCode) {
        ReminderDispatch entry=reminders.lockById(id)
            .orElseThrow(()->new IllegalArgumentException("Xatırlatma tapılmadı"));
        if(entry.getStatus()!=ReminderDispatchStatus.SENDING)return;
        entry.setStatus(sent?ReminderDispatchStatus.SENT:ReminderDispatchStatus.FAILED);
        entry.setSentAt(sent?LocalDateTime.now(ZONE):null);
        entry.setLastError(sent?null:safeErrorCode);
        reminders.save(entry);
        audit.record(sent?"SENT":"FAILED","PAYMENT_REMINDER",id,
            Map.of("status","SENDING"),
            Map.of("status",entry.getStatus().name(),"attempts",entry.getAttempts()));
    }
}
