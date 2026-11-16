package com.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardDto {
    private Long totalStudents;
    private Long activeStudents;
    private Long totalTeachers;
    private Long activeGroups;

    private BigDecimal currentMonthCollected;
    private BigDecimal currentMonthExpected;
    private BigDecimal currentMonthPaidAgainstExpected;
    private BigDecimal currentMonthOutstanding;
    private BigDecimal totalOverdueDebt;
    private BigDecimal collectionRate;

    private Long overdueCount;
    private List<PaymentDto> recentPayments;
    private List<GroupSummaryDto> groupSummaries;

    @Data
    public static class GroupSummaryDto {
        private Long groupId;
        private String groupNumber;
        private Long studentCount;
        private BigDecimal collected;
        private BigDecimal expected;
        private BigDecimal paidAgainstInvoices;
        private BigDecimal outstanding;
        private BigDecimal teacherShare;
        private BigDecimal netProfit;
    }
}
