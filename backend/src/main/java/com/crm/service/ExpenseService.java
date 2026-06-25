package com.crm.service;

import com.crm.dto.ExpenseDto;
import com.crm.entity.Expense;
import com.crm.enums.ExpenseType;
import com.crm.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public List<ExpenseDto> getByPeriod(Integer year, Integer month) {
        return expenseRepository.findByExpenseYearAndExpenseMonth(year, month).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<ExpenseDto> getRecurring() {
        return expenseRepository.findByIsRecurring(true).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<ExpenseDto> getByType(ExpenseType type) {
        return expenseRepository.findByType(type).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseDto create(ExpenseDto dto) {
        Expense expense = Expense.builder()
            .type(dto.getType())
            .amount(dto.getAmount())
            .expenseYear(dto.getExpenseYear())
            .expenseMonth(dto.getExpenseMonth())
            .isRecurring(dto.isRecurring())
            .description(dto.getDescription())
            .build();

        return toDto(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseDto dto) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Xərc tapılmadı"));

        expense.setType(dto.getType());
        expense.setAmount(dto.getAmount());
        expense.setExpenseYear(dto.getExpenseYear());
        expense.setExpenseMonth(dto.getExpenseMonth());
        expense.setRecurring(dto.isRecurring());
        expense.setDescription(dto.getDescription());

        return toDto(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    @Transactional
    public void createRecurringExpenses(Integer year, Integer month) {
        List<Expense> recurring = expenseRepository.findByIsRecurring(true);
        for (Expense recurringExpense : recurring) {
            Expense newExpense = Expense.builder()
                .type(recurringExpense.getType())
                .amount(recurringExpense.getAmount())
                .expenseYear(year)
                .expenseMonth(month)
                .isRecurring(true)
                .description(recurringExpense.getDescription())
                .build();
            expenseRepository.save(newExpense);
        }
    }

    public BigDecimal getTotalExpenses(Integer year, Integer month) {
        return expenseRepository.findByExpenseYearAndExpenseMonth(year, month).stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getFixedExpensesTotal() {
        return expenseRepository.findByIsRecurring(true).stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ExpenseDto toDto(Expense e) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(e.getId());
        dto.setType(e.getType());
        dto.setAmount(e.getAmount());
        dto.setExpenseYear(e.getExpenseYear());
        dto.setExpenseMonth(e.getExpenseMonth());
        dto.setRecurring(e.isRecurring());
        dto.setDescription(e.getDescription());
        return dto;
    }
}
