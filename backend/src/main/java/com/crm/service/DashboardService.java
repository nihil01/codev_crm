package com.crm.service;

import com.crm.dto.DashboardDto;
import com.crm.dto.PaymentDto;
import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    public DashboardDto getDashboard() {
        DashboardDto dashboard = new DashboardDto();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        dashboard.setTotalStudents(studentRepository.count());
        dashboard.setActiveStudents(studentRepository.countByArchived(false));
        dashboard.setTotalTeachers((long) teacherRepository.count());
        dashboard.setActiveGroups(groupRepository.countByStatus(GroupStatus.AKTIV));

        BigDecimal collected = paymentService.getCollectedForMonth(year, month);
        dashboard.setCurrentMonthCollected(collected);

        BigDecimal expected = calculateExpectedForMonth(year, month);
        dashboard.setCurrentMonthExpected(expected);

        if (expected.compareTo(BigDecimal.ZERO) > 0) {
            dashboard.setCollectionRate(collected.multiply(BigDecimal.valueOf(100))
                .divide(expected, 2, RoundingMode.HALF_UP));
        } else {
            dashboard.setCollectionRate(BigDecimal.ZERO);
        }

        dashboard.setOverdueCount(paymentRepository.countOverduePayments(year, month));

        // Recent payments
        List<PaymentDto> recentPayments = paymentService.getByPeriod(year, month).stream()
            .limit(10)
            .collect(Collectors.toList());
        dashboard.setRecentPayments(recentPayments);

        // Group summaries
        List<DashboardDto.GroupSummaryDto> summaries = new ArrayList<>();
        for (CourseGroup group : groupRepository.findByStatus(GroupStatus.AKTIV)) {
            DashboardDto.GroupSummaryDto gs = new DashboardDto.GroupSummaryDto();
            gs.setGroupId(group.getId());
            gs.setGroupNumber(group.getGroupNumber());
            gs.setStudentCount((long) group.getGroupStudents().size());
            gs.setCollected(courseGroupService.calculateGroupCollected(group.getId(), year, month));
            gs.setTeacherShare(courseGroupService.calculateTeacherSalary(group.getId(), year, month));
            gs.setNetProfit(gs.getCollected().subtract(gs.getTeacherShare()));
            summaries.add(gs);
        }
        dashboard.setGroupSummaries(summaries);

        return dashboard;
    }

    private BigDecimal calculateExpectedForMonth(Integer year, Integer month) {
        return paymentRepository.findByPaymentYearAndPaymentMonth(year, month).stream()
            .map(p -> p.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
