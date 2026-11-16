package com.crm.service;

import com.crm.dto.DashboardDto;
import com.crm.dto.PaymentDto;
import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import com.crm.enums.StudentStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import com.crm.entity.GroupStudent;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseGroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final CourseGroupService courseGroupService;
    private final PaymentService paymentService;
    private final BillingMetricsService billingMetrics;

    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        DashboardDto dashboard = new DashboardDto();

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Baku"));
        int year = now.getYear();
        int month = now.getMonthValue();

        dashboard.setTotalStudents(studentRepository.countByArchived(false));
        dashboard.setActiveStudents(studentRepository.countInActiveGroups(StudentStatus.AKTIV, List.of(GroupStatus.AKTIV,GroupStatus.BASLAYIB)));
        dashboard.setTotalTeachers((long) teacherRepository.count());
        dashboard.setActiveGroups(groupRepository.countByStatusIn(List.of(GroupStatus.AKTIV,GroupStatus.BASLAYIB)));

        var overdue=billingMetrics.allOverdue();
        dashboard.setOverdueCount((long) overdue.size());
        dashboard.setTotalOverdueDebt(overdue.stream().map(PaymentDto::getRemainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Invoices issued in the selected month (not cash receipt transactions)
        List<PaymentDto> recentPayments = paymentService.getByPeriod(year, month).stream()
            .limit(10)
            .collect(Collectors.toList());
        dashboard.setRecentPayments(recentPayments);

        // Group summaries
        List<DashboardDto.GroupSummaryDto> summaries = new ArrayList<>();
        // Preserve historic income and invoices for completed/suspended groups.
        for (CourseGroup group : groupRepository.findAll()) {
            DashboardDto.GroupSummaryDto gs = new DashboardDto.GroupSummaryDto();
            gs.setGroupId(group.getId());
            gs.setGroupNumber(group.getGroupNumber());
            gs.setStudentCount(group.getGroupStudents().stream()
                .filter(m->m.getLeftAt()==null && !m.getStudent().isArchived() &&
                    m.getStudent().getStatus()==StudentStatus.AKTIV)
                .count());
            BillingMetricsService.InvoiceTotals groupBills=BillingMetricsService.fromInvoices(
                paymentService.getByGroupAndPeriod(group.getId(),year,month));
            gs.setExpected(groupBills.expected());
            gs.setPaidAgainstInvoices(groupBills.paidAgainstInvoices());
            gs.setOutstanding(groupBills.remaining());
            gs.setCollected(courseGroupService.calculateGroupCollected(group.getId(), year, month));
            gs.setTeacherShare(courseGroupService.calculateTeacherSalary(group.getId(), year, month));
            gs.setNetProfit(gs.getCollected().subtract(gs.getTeacherShare()));
            summaries.add(gs);
        }
        dashboard.setGroupSummaries(summaries);

        // Summary figures must match the exact columns shown in the group table.
        BigDecimal expected=BigDecimal.ZERO, paidAgainst=BigDecimal.ZERO;
        BigDecimal remaining=BigDecimal.ZERO, cash=BigDecimal.ZERO;
        BigDecimal teacherShare=BigDecimal.ZERO, afterTeacher=BigDecimal.ZERO;
        for (DashboardDto.GroupSummaryDto row : summaries) {
            expected=expected.add(row.getExpected());
            paidAgainst=paidAgainst.add(row.getPaidAgainstInvoices());
            remaining=remaining.add(row.getOutstanding());
            cash=cash.add(row.getCollected());
            teacherShare=teacherShare.add(row.getTeacherShare());
            afterTeacher=afterTeacher.add(row.getNetProfit());
        }
        dashboard.setCurrentMonthExpected(expected);
        dashboard.setCurrentMonthPaidAgainstExpected(paidAgainst);
        dashboard.setCurrentMonthOutstanding(remaining);
        dashboard.setCurrentMonthCollected(cash);
        dashboard.setCurrentMonthTeacherShare(teacherShare);
        dashboard.setCurrentMonthAfterTeacherShare(afterTeacher);
        dashboard.setCollectionRate(expected.signum()==0 ? BigDecimal.ZERO
            : paidAgainst.multiply(BigDecimal.valueOf(100)).divide(expected,2,RoundingMode.HALF_UP));

        return dashboard;
    }

}
