package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDto {
    private List<ForecastPoint> history;
    private List<ForecastPoint> forecast;
    private TrendSummary trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private Integer year;
        private Integer month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal netProfit;
        private Boolean isProjected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummary {
        private BigDecimal monthlyProfitTrend;
        private BigDecimal projectedAnnualProfit;
        private String trendDirection;
        private BigDecimal averageMonthlyIncome;
        private BigDecimal averageMonthlyExpenses;
        private BigDecimal averageMonthlyProfit;
    }
}
