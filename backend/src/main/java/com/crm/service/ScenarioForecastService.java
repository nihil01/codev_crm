package com.crm.service;

import com.crm.dto.ReportDto;
import com.crm.enums.GroupStatus;
import com.crm.enums.StudentStatus;
import com.crm.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioForecastService {
    private final ReportService reports;
    private final StudentRepository students;

    public Map<String,Object> scenarios(int months) {
        if(months!=3 && months!=6)throw new IllegalArgumentException("Üfüq yalnız 3 və ya 6 ay ola bilər");
        YearMonth current=YearMonth.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Baku")));
        BigDecimal income=BigDecimal.ZERO,expense=BigDecimal.ZERO;
        for(int i=1;i<=3;i++){
            YearMonth past=current.minusMonths(i);
            ReportDto report=reports.getMonthlyReport(past.getYear(),past.getMonthValue());
            income=income.add(report.getTotalIncome());
            expense=expense.add(report.getTotalExpenses());
        }
        income=income.divide(BigDecimal.valueOf(3),2,RoundingMode.HALF_UP);
        expense=expense.divide(BigDecimal.valueOf(3),2,RoundingMode.HALF_UP);
        long active=students.countInActiveGroups(StudentStatus.AKTIV,List.of(GroupStatus.AKTIV,GroupStatus.BASLAYIB));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("basedOnMonths",3);
        result.put("baselineIncome",income);
        result.put("baselineExpenses",expense);
        result.put("baselineStudents",active);
        result.put("assumptions","Son üç tam ayın faktiki gəlir və xərclərinin orta qiyməti. Ssenari faizləri nümunə fərziyyələridir, zəmanət deyil.");
        Map<String,Object> scenarios=new LinkedHashMap<>();
        scenarios.put("pessimist",build(current,months,income,expense,active,new BigDecimal("-0.03"),new BigDecimal("0.02")));
        scenarios.put("baza",build(current,months,income,expense,active,BigDecimal.ZERO,BigDecimal.ZERO));
        scenarios.put("optimist",build(current,months,income,expense,active,new BigDecimal("0.03"),new BigDecimal("0.01")));
        result.put("scenarios",scenarios);
        return result;
    }
    private List<Map<String,Object>> build(YearMonth current,int months,BigDecimal income,BigDecimal expense,
                                          long active,BigDecimal monthlyGrowth,BigDecimal monthlyExpenseGrowth){
        List<Map<String,Object>> result=new ArrayList<>();
        BigDecimal incomeFactor=BigDecimal.ONE.add(monthlyGrowth);
        BigDecimal expenseFactor=BigDecimal.ONE.add(monthlyExpenseGrowth);
        for(int i=1;i<=months;i++){
            YearMonth m=current.plusMonths(i);
            BigDecimal revenue=income.multiply(incomeFactor.pow(i)).setScale(2,RoundingMode.HALF_UP).max(BigDecimal.ZERO);
            BigDecimal expenses=expense.multiply(expenseFactor.pow(i)).setScale(2,RoundingMode.HALF_UP).max(BigDecimal.ZERO);
            long students=BigDecimal.valueOf(active).multiply(incomeFactor.pow(i)).setScale(0,RoundingMode.HALF_UP).longValue();
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("year",m.getYear());item.put("month",m.getMonthValue());
            item.put("income",revenue);item.put("expenses",expenses);
            item.put("profit",revenue.subtract(expenses));
            item.put("students",students);
            result.add(item);
        }
        return result;
    }
}
