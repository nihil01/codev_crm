package com.crm;

import com.crm.entity.*;
import com.crm.enums.*;
import com.crm.repository.*;
import com.crm.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderConsentAndIdempotencyTest {
    @Mock PaymentRepository invoices;
    @Mock PaymentService payments;
    @Mock ReminderDispatchRepository outbox;
    @Mock ChangeAuditService audit;
    @InjectMocks ReminderQueueService queue;

    private Payment invoice(boolean consent) {
        var student=Student.builder().id(11L).firstName("A")
            .reminderConsent(consent).phone("+994501112233").build();
        var group=CourseGroup.builder().id(8L).status(GroupStatus.BASLAYIB).build();
        return Payment.builder().id(24L).student(student).group(group)
            .amount(new BigDecimal("250.00"))
            .dueDate(LocalDate.now(ZoneId.of("Asia/Baku")).minusDays(8)).build();
    }

    @Test
    void neverQueuesWhenConsentIsMissing() {
        when(invoices.findAll()).thenReturn(List.of(invoice(false)));
        assertEquals(0,queue.enqueueOverdue());
        verifyNoInteractions(outbox,payments,audit);
    }

    @Test
    void existingInvoiceReminderIsNotQueuedTwice() {
        when(invoices.findAll()).thenReturn(List.of(invoice(true)));
        when(payments.remaining(any(Payment.class))).thenReturn(new BigDecimal("50.00"));
        when(outbox.existsByPaymentIdAndKind(24L,"OVERDUE_DAY6")).thenReturn(true);
        assertEquals(0,queue.enqueueOverdue());
        verify(outbox,never()).saveAndFlush(any());
    }
}
