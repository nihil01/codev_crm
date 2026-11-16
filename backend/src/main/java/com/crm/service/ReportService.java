package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.dto.ReportDto;
import com.crm.entity.CourseGroup;
import com.crm.enums.ExpenseType;
import com.crm.enums.GroupStatus;
import com.crm.enums.PaymentStatus;
import com.crm.repository.CourseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PaymentService paymentService;
    private final ExpenseService expenseService;
    private final CourseGroupService courseGroupService;
    private final CourseGroupRepository groupRepository;

    public ReportDto getMonthlyReport(Integer year, Integer month) {
        ReportDto report = new ReportDto();
        report.setYear(year);
        report.setMonth(month);

        // Payment lists
        List<PaymentDto> allPayments = paymentService.getByPeriod(year, month);
        report.setPendingPayments(filterByStatus(allPayments, PaymentStatus.GOZLENILIR));
        report.setPaidPayments(filterByStatus(allPayments, PaymentStatus.ODENILIB));
        report.setOverduePayments(filterByStatus(allPayments, PaymentStatus.GECIKIB));

        BigDecimal collected = paymentService.getCollectedForMonth(year, month);
        report.setTotalCollected(collected);

        BigDecimal expected = calculateExpected(year, month);
        report.setTotalExpected(expected);

        if (expected.compareTo(BigDecimal.ZERO) > 0) {
            report.setCollectionRate(collected.multiply(BigDecimal.valueOf(100))
                .divide(expected, 2, RoundingMode.HALF_UP));
        } else {
            report.setCollectionRate(BigDecimal.ZERO);
        }

        // Profit report
        report.setTotalIncome(collected);

        BigDecimal teacherSalaries = calculateTeacherSalaries(year, month);
        report.setTotalTeacherSalaries(teacherSalaries);

        BigDecimal utilityExpenses = expenseService.getTotalExpenses(year, month);
        report.setTotalUtilityExpenses(utilityExpenses);

        BigDecimal fixedExpenses = calculateFixedExpensesForMonth(year, month);
        report.setTotalFixedExpenses(fixedExpenses);

        BigDecimal totalExpenses = teacherSalaries.add(utilityExpenses);
        report.setTotalExpenses(totalExpenses);

        report.setNetProfit(collected.subtract(totalExpenses));

        // Monthly comparison
        report.setMonthlyComparison(getMonthlyComparison(year, month));

        return report;
    }

    private BigDecimal calculateExpected(Integer year, Integer month) {
        List<CourseGroup> activeGroups = groupRepository.findByStatus(GroupStatus.AKTIV);
        BigDecimal total = BigDecimal.ZERO;

        for (CourseGroup group : activeGroups) {
            List<PaymentDto> groupPayments = paymentService.getByGroupAndPeriod(group.getId(), year, month);
            total = total.add(groupPayments.stream()
                .map(PaymentDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return total;
    }

    private BigDecimal calculateTeacherSalaries(Integer year, Integer month) {
        List<CourseGroup> activeGroups = groupRepository.findByStatus(GroupStatus.AKTIV);
        BigDecimal total = BigDecimal.ZERO;

        for (CourseGroup group : activeGroups) {
            total = total.add(courseGroupService.calculateTeacherSalary(group.getId(), year, month));
        }
        return total;
    }

    private BigDecimal calculateFixedExpensesForMonth(Integer year, Integer month) {
        return expenseService.getRecurring().stream()
            .map(e -> e.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ReportDto.MonthlyComparison> getMonthlyComparison(Integer currentYear, Integer currentMonth) {
        List<ReportDto.MonthlyComparison> comparisons = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            int year = currentYear;
            int month = currentMonth - i;

            while (month <= 0) {
                month += 12;
                year -= 1;
            }

            ReportDto.MonthlyComparison mc = new ReportDto.MonthlyComparison();
            mc.setYear(year);
            mc.setMonth(month);
            mc.setIncome(paymentService.getCollectedForMonth(year, month));
            mc.setExpenses(expenseService.getTotalExpenses(year, month)
                .add(calculateTeacherSalaries(year, month)));
            mc.setProfit(mc.getIncome().subtract(mc.getExpenses()));

            comparisons.add(mc);
        }

        return comparisons;
    }

    private List<PaymentDto> filterByStatus(List<PaymentDto> payments, PaymentStatus status) {
        return payments.stream()
            .filter(p -> p.getStatus() == status)
            .collect(Collectors.toList());
    }
}
