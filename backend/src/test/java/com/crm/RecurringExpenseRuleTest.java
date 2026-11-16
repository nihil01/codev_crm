package com.crm;

import com.crm.entity.Expense;
import com.crm.entity.RecurringExpenseRule;
import com.crm.enums.ExpenseType;
import com.crm.repository.ExpenseRepository;
import com.crm.repository.RecurringExpenseRuleRepository;
import com.crm.service.ChangeAuditService;
import com.crm.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecurringExpenseRuleTest {
    @Test
    void futureRecurringRuleDoesNotCreatePastExpense() {
        ExpenseRepository expenses=mock(ExpenseRepository.class);
        RecurringExpenseRuleRepository rules=mock(RecurringExpenseRuleRepository.class);
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        ExpenseService service=new ExpenseService(expenses,rules,jdbc,mock(ChangeAuditService.class));
        RecurringExpenseRule future=RecurringExpenseRule.builder().id(4L)
            .type(ExpenseType.YER).amount(new BigDecimal("500.00"))
            .startYear(2027).startMonth(1).active(true).build();
        when(rules.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(future));
        service.createRecurringExpenses(2026,11);
        verifyNoInteractions(jdbc);
    }

    @Test
    void cancelledRecurringMonthDoesNotEnterExpenseTotals() {
        ExpenseRepository expenses=mock(ExpenseRepository.class);
        ExpenseService service=new ExpenseService(expenses,
            mock(RecurringExpenseRuleRepository.class),mock(JdbcTemplate.class),
            mock(ChangeAuditService.class));
        Expense cancelled=Expense.builder().amount(new BigDecimal("500.00"))
            .recurring(true).cancelled(true).build();
        Expense charged=Expense.builder().amount(new BigDecimal("120.00"))
            .recurring(false).cancelled(false).build();
        when(expenses.findByExpenseYearAndExpenseMonth(2026,11))
            .thenReturn(List.of(cancelled,charged));
        assertEquals(new BigDecimal("120.00"),service.getTotalExpenses(2026,11));
        assertEquals(BigDecimal.ZERO,service.getTotalRecurring(2026,11));
    }
}
