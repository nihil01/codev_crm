package com.crm.service;

import com.crm.dto.ForecastDto;
import com.crm.dto.ForecastDto.ForecastPoint;
import com.crm.dto.ForecastDto.TrendSummary;
import com.crm.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private static final int HISTORY_MONTHS = 12;
    private static final int FORECAST_MONTHS = 6;

    private final ReportService reportService;

    public ForecastDto getForecast() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Gather 12 months of historical data using existing ReportService
        List<ForecastPoint> history = new ArrayList<>();
        for (int i = HISTORY_MONTHS - 1; i >= 0; i--) {
            int year = currentYear;
            int month = currentMonth - i;

            while (month <= 0) {
                month += 12;
                year -= 1;
            }

            ReportDto report = reportService.getMonthlyReport(year, month);

            history.add(ForecastPoint.builder()
                    .year(year)
                    .month(month)
                    .income(report.getTotalIncome())
                    .expenses(report.getTotalExpenses())
                    .netProfit(report.getNetProfit())
                    .isProjected(false)
                    .build());
        }

        // Extract values for regression
        List<BigDecimal> incomeValues = history.stream().map(ForecastPoint::getIncome).toList();
        List<BigDecimal> expenseValues = history.stream().map(ForecastPoint::getExpenses).toList();
        List<BigDecimal> profitValues = history.stream().map(ForecastPoint::getNetProfit).toList();

        // Calculate linear regression parameters (OLS)
        BigDecimal incomeSlope = calculateSlope(incomeValues);
        BigDecimal incomeIntercept = calculateIntercept(incomeValues, incomeSlope);
        BigDecimal expenseSlope = calculateSlope(expenseValues);
        BigDecimal expenseIntercept = calculateIntercept(expenseValues, expenseSlope);
        BigDecimal profitSlope = calculateSlope(profitValues);
        BigDecimal profitIntercept = calculateIntercept(profitValues, profitSlope);

        // Project next 6 months
        List<ForecastPoint> forecast = new ArrayList<>();
        for (int i = 1; i <= FORECAST_MONTHS; i++) {
            int year = currentYear;
            int month = currentMonth + i;

            while (month > 12) {
                month -= 12;
                year += 1;
            }

            BigDecimal x = BigDecimal.valueOf(HISTORY_MONTHS + i - 1);
            BigDecimal income = incomeSlope.multiply(x).add(incomeIntercept).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expenses = expenseSlope.multiply(x).add(expenseIntercept).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = profitSlope.multiply(x).add(profitIntercept).setScale(2, RoundingMode.HALF_UP);

            // Clamp negative values to zero for income/expenses
            if (income.compareTo(BigDecimal.ZERO) < 0) income = BigDecimal.ZERO;
            if (expenses.compareTo(BigDecimal.ZERO) < 0) expenses = BigDecimal.ZERO;

            forecast.add(ForecastPoint.builder()
                    .year(year)
                    .month(month)
                    .income(income)
                    .expenses(expenses)
                    .netProfit(profit)
                    .isProjected(true)
                    .build());
        }

        // Build trend summary
        TrendSummary trend = buildTrendSummary(history, profitSlope, forecast);

        return ForecastDto.builder()
                .history(history)
                .forecast(forecast)
                .trend(trend)
                .build();
    }

    /**
     * Ordinary Least Squares: slope = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
     */
    private BigDecimal calculateSlope(List<BigDecimal> yValues) {
        int n = yValues.size();
        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumX2 = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            BigDecimal x = BigDecimal.valueOf(i);
            BigDecimal y = yValues.get(i);
            sumX = sumX.add(x);
            sumY = sumY.add(y);
            sumXY = sumXY.add(x.multiply(y));
            sumX2 = sumX2.add(x.multiply(x));
        }

        BigDecimal numerator = BigDecimal.valueOf(n).multiply(sumXY).subtract(sumX.multiply(sumY));
        BigDecimal denominator = BigDecimal.valueOf(n).multiply(sumX2).subtract(sumX.multiply(sumX));

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIntercept(List<BigDecimal> yValues, BigDecimal slope) {
        int n = yValues.size();
        BigDecimal sumY = yValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumX = BigDecimal.valueOf(n * (n - 1) / 2.0);

        return sumY.subtract(slope.multiply(sumX))
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP);
    }

    private TrendSummary buildTrendSummary(List<ForecastPoint> history, BigDecimal profitSlope,
                                           List<ForecastPoint> forecast) {
        BigDecimal avgIncome = history.stream()
                .map(ForecastPoint::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgExpenses = history.stream()
                .map(ForecastPoint::getExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgProfit = history.stream()
                .map(ForecastPoint::getNetProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyTrend = profitSlope.setScale(2, RoundingMode.HALF_UP);

        // Projected annual profit = sum of forecast 6 months + extrapolated months 7-12
        BigDecimal annualProfit = forecast.stream()
                .map(ForecastPoint::getNetProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal intercept = avgProfit.subtract(profitSlope.multiply(BigDecimal.valueOf(HISTORY_MONTHS / 2.0)));
        for (int i = FORECAST_MONTHS + 1; i <= 12; i++) {
            BigDecimal x = BigDecimal.valueOf(HISTORY_MONTHS + i - 1);
            BigDecimal profit = profitSlope.multiply(x).add(intercept).setScale(2, RoundingMode.HALF_UP);
            annualProfit = annualProfit.add(profit);
        }

        String direction;
        if (monthlyTrend.compareTo(BigDecimal.valueOf(50)) > 0) {
            direction = "GELIŞEN";
        } else if (monthlyTrend.compareTo(BigDecimal.valueOf(-50)) < 0) {
            direction = "AZALAN";
        } else {
            direction = "STABIL";
        }

        return TrendSummary.builder()
                .monthlyProfitTrend(monthlyTrend)
                .projectedAnnualProfit(annualProfit)
                .trendDirection(direction)
                .averageMonthlyIncome(avgIncome)
                .averageMonthlyExpenses(avgExpenses)
                .averageMonthlyProfit(avgProfit)
                .build();
    }
}
