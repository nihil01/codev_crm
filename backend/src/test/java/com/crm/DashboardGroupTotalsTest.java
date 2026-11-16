package com.crm;

import com.crm.dto.DashboardDto;
import com.crm.dto.PaymentDto;
import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import com.crm.enums.PaymentStatus;
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
class DashboardGroupTotalsTest {
    @Mock StudentRepository students;
    @Mock TeacherRepository teachers;
    @Mock CourseGroupRepository groups;
    @Mock PaymentRepository invoices;
    @Mock CourseGroupService groupService;
    @Mock PaymentService payments;
    @Mock BillingMetricsService billing;
    @InjectMocks DashboardService dashboard;

    private PaymentDto invoice(String billed,String paid) {
        PaymentDto p=new PaymentDto();
        p.setAmount(new BigDecimal(billed));
        p.setPaidAmount(new BigDecimal(paid));
        p.setDiscountAmount(BigDecimal.ZERO);
        p.setStatus(PaymentStatus.GOZLENILIR);
        return p;
    }

    @Test
    void totalsMatchRowsAndIncludeSuspendedAndFinishedGroups() {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Baku"));
        int y=today.getYear(),m=today.getMonthValue();
        CourseGroup completed=CourseGroup.builder().id(10L)
            .groupNumber("FINISHED").status(GroupStatus.TAMAMLANMIS).build();
        CourseGroup suspended=CourseGroup.builder().id(11L)
            .groupNumber("SUSPENDED").status(GroupStatus.DAYANDIRILMIS).build();
        when(groups.findAll()).thenReturn(List.of(completed,suspended));
        when(payments.getByGroupAndPeriod(10L,y,m))
            .thenReturn(List.of(invoice("300.00","100.00")));
        when(payments.getByGroupAndPeriod(11L,y,m))
            .thenReturn(List.of(invoice("200.00","50.00")));
        when(groupService.calculateGroupCollected(10L,y,m))
            .thenReturn(new BigDecimal("40.00"));
        when(groupService.calculateGroupCollected(11L,y,m))
            .thenReturn(new BigDecimal("70.00"));
        when(groupService.calculateTeacherSalary(10L,y,m))
            .thenReturn(new BigDecimal("20.00"));
        when(groupService.calculateTeacherSalary(11L,y,m))
            .thenReturn(new BigDecimal("10.00"));

        DashboardDto result=dashboard.getDashboard();
        assertEquals(2,result.getGroupSummaries().size());
        assertEquals(new BigDecimal("500.00"),result.getCurrentMonthExpected());
        assertEquals(new BigDecimal("150.00"),result.getCurrentMonthPaidAgainstExpected());
        assertEquals(new BigDecimal("350.00"),result.getCurrentMonthOutstanding());
        assertEquals(new BigDecimal("110.00"),result.getCurrentMonthCollected());
        assertEquals(new BigDecimal("30.00"),result.getCurrentMonthTeacherShare());
        assertEquals(new BigDecimal("80.00"),result.getCurrentMonthAfterTeacherShare());
        assertEquals(new BigDecimal("30.00"),result.getCollectionRate());
        assertEquals(result.getCurrentMonthPaidAgainstExpected(),
            result.getGroupSummaries().stream()
              .map(DashboardDto.GroupSummaryDto::getPaidAgainstInvoices)
              .reduce(BigDecimal.ZERO,BigDecimal::add));
        assertEquals(result.getCurrentMonthAfterTeacherShare(),
            result.getGroupSummaries().stream()
              .map(DashboardDto.GroupSummaryDto::getNetProfit)
              .reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
