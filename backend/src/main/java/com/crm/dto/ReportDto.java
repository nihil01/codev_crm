package com.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReportDto {
    private Integer year;
    private Integer month;

    // Payment summary
    private List<PaymentDto> pendingPayments;
    private List<PaymentDto> paidPayments;
    private List<PaymentDto> overduePayments;
    private BigDecimal totalCollected;
    private BigDecimal totalExpected;
    private BigDecimal collectionRate;

    // Profit report
    private BigDecimal totalIncome;
    private BigDecimal totalTeacherSalaries;
    private BigDecimal totalFixedExpenses;
    private BigDecimal totalUtilityExpenses;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;

    // Monthly comparison (last 6 months)
    private List<MonthlyComparison> monthlyComparison;

    @Data
    public static class MonthlyComparison {
        private Integer year;
        private Integer month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal profit;
    }
}
