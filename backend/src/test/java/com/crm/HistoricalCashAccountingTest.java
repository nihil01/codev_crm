package com.crm;

import com.crm.dto.ReportDto;
import com.crm.repository.TeacherShareAllocationRepository;
import com.crm.service.ExpenseService;
import com.crm.service.PaymentService;
import com.crm.service.ReportService;
import com.crm.service.BillingMetricsService;
import com.crm.service.CourseGroupService;
import com.crm.repository.CourseGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalCashAccountingTest {
    @Mock PaymentService payments;
    @Mock ExpenseService expenses;
    @Mock CourseGroupService groups;
    @Mock CourseGroupRepository groupRepository;
    @Mock TeacherShareAllocationRepository allocations;
    @Mock BillingMetricsService billingMetrics;
    @InjectMocks ReportService reports;

    @Test
    void teacherCostsRemainInHistoricalReportWhenGroupIsNotActive() {
        when(payments.getByPeriod(2026,11)).thenReturn(List.of());
        when(payments.getCollectedForMonth(anyInt(),anyInt())).thenReturn(BigDecimal.ZERO);
        when(payments.getCollectedForMonth(2026,11)).thenReturn(new BigDecimal("300.00"));
        when(allocations.sumAllForPeriod(any(LocalDateTime.class),any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("90.00"));
        when(expenses.getTotalOneTime(2026,11)).thenReturn(BigDecimal.ZERO);
        when(expenses.getTotalRecurring(2026,11)).thenReturn(BigDecimal.ZERO);
        when(expenses.getTotalExpenses(anyInt(),anyInt())).thenReturn(BigDecimal.ZERO);

        // Other comparison months are stubs with zero cash and expenses.
        ReportDto result=reports.getMonthlyReport(2026,11);
        assertEquals(new BigDecimal("300.00"),result.getTotalIncome());
        assertEquals(new BigDecimal("90.00"),result.getTotalTeacherSalaries());
        assertEquals(new BigDecimal("210.00"),result.getNetProfit());
    }
}
