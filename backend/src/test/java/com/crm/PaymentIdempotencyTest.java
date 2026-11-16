package com.crm;

import com.crm.dto.PaymentDto;
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
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentIdempotencyTest {
    @Mock PaymentRepository invoices;
    @Mock PaymentTransactionRepository transactions;
    @Mock StudentRepository students;
    @Mock CourseGroupRepository groups;
    @Mock StudentFreezePeriodRepository freezePeriods;
    @Mock GroupStudentRepository memberships;
    @Mock ChangeAuditService audit;
    @Mock GroupTeacherRepository groupTeachers;
    @Mock TeacherShareAllocationRepository allocations;
    @InjectMocks PaymentService service;

    @Test
    void sameRequestKeyCannotChargeAgain() {
        Student student=Student.builder().id(1L).firstName("A").lastName("B")
            .status(StudentStatus.AKTIV).build();
        CourseGroup group=CourseGroup.builder().id(2L).groupNumber("G-1").build();
        Payment invoice=Payment.builder().id(3L).student(student).group(group)
            .paymentYear(2026).paymentMonth(11)
            .amount(new BigDecimal("300.00")).discountAmount(BigDecimal.ZERO)
            .dueDate(LocalDate.now()).status(PaymentStatus.GOZLENILIR).build();
        PaymentTransaction existing=PaymentTransaction.builder().id(12L).payment(invoice)
            .amount(new BigDecimal("100.00")).method(PaymentMethod.NAGD).build();
        when(invoices.lockById(3L)).thenReturn(Optional.of(invoice));
        when(transactions.findByPaymentIdAndIdempotencyKey(3L,"request-0123456789"))
            .thenReturn(Optional.of(existing));
        when(transactions.sumForInvoice(3L)).thenReturn(new BigDecimal("100.00"));
        PaymentDto result=service.recordPartial(3L,new BigDecimal("100.00"),
            PaymentMethod.NAGD,"request-0123456789");
        assertEquals(new BigDecimal("100.00"),result.getPaidAmount());
        assertEquals(new BigDecimal("200.00"),result.getRemainingAmount());
        verify(transactions,never()).saveAndFlush(any());
        verifyNoInteractions(allocations);
    }
}
