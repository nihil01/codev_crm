package com.crm;

import com.crm.entity.Payment;
import com.crm.enums.PaymentStatus;
import com.crm.repository.PaymentTransactionRepository;
import com.crm.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCalculationTest {
    @Mock PaymentTransactionRepository transactions;
    @Mock com.crm.repository.PaymentRepository invoices;
    @Mock com.crm.repository.StudentRepository students;
    @Mock com.crm.repository.CourseGroupRepository groups;
    @Mock com.crm.repository.StudentFreezePeriodRepository freezePeriods;
    @InjectMocks PaymentService service;

    @Test
    void partialPaymentsLeaveCorrectDebt() {
        Payment payment=Payment.builder().id(42L).amount(new BigDecimal("300.00"))
            .discountAmount(BigDecimal.ZERO).status(PaymentStatus.GOZLENILIR).build();
        when(transactions.sumForInvoice(42L)).thenReturn(new BigDecimal("230.00"));
        assertEquals(new BigDecimal("70.00"),service.remaining(payment));
    }

    @Test
    void discountReducesDebt() {
        Payment payment=Payment.builder().id(10L).amount(new BigDecimal("200.00"))
            .discountAmount(new BigDecimal("20.00")).status(PaymentStatus.GOZLENILIR).build();
        when(transactions.sumForInvoice(10L)).thenReturn(new BigDecimal("80.00"));
        assertEquals(new BigDecimal("100.00"),service.remaining(payment));
    }

    @Test
    void overdueStartsOnlyAfterFiveCompleteDays() {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Baku"));
        Payment p=Payment.builder().dueDate(today.minusDays(5)).build();
        assertFalse(service.overdue(p));
        p.setDueDate(today.minusDays(6));
        assertTrue(service.overdue(p));
    }

    @Test
    void frozenMonthHasNoDebt() {
        Payment p=Payment.builder().id(3L).amount(new BigDecimal("200"))
            .frozen(true).build();
        assertEquals(BigDecimal.ZERO,service.remaining(p));
        verifyNoInteractions(transactions);
    }
}
