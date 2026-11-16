package com.crm;

import com.crm.repository.*;
import com.crm.service.CourseGroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCommissionLedgerTest {
    @Mock CourseGroupRepository groups;
    @Mock GroupStudentRepository enrollments;
    @Mock GroupTeacherRepository groupTeachers;
    @Mock StudentRepository students;
    @Mock TeacherRepository teachers;
    @Mock PaymentRepository invoices;
    @Mock com.crm.service.PaymentService payments;
    @Mock CourseRepository courses;
    @Mock StudentFreezePeriodRepository freezes;
    @Mock com.crm.service.ChangeAuditService audit;
    @Mock TeacherShareAllocationRepository allocations;
    @Mock PaymentTransactionRepository transactions;
    @InjectMocks CourseGroupService service;

    @Test
    void pastSalaryUsesReceiptLedgerNotCurrentPercentage() {
        YearMonth month=YearMonth.of(2026,11);
        when(allocations.sumForGroup(4L,month.atDay(1).atStartOfDay(),
            month.plusMonths(1).atDay(1).atStartOfDay()))
            .thenReturn(new BigDecimal("37.50"));
        assertEquals(new BigDecimal("37.50"),service.calculateTeacherSalary(4L,2026,11));
        verifyNoInteractions(groupTeachers);
    }
}
