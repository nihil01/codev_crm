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
        return expenseRepository.findByRecurring(true).stream()
            .collect(Collectors.groupingBy(e -> e.getType().name() + "|" + (e.getDescription() != null ? e.getDescription().trim() : "")))
            .values().stream()
            .map(list -> list.get(0))
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
        if (dto.getRecurring()) {
            // Create for all 12 months of the year
            for (int m = 1; m <= 12; m++) {
                Expense expense = Expense.builder()
                    .type(dto.getType())
                    .amount(dto.getAmount())
                    .expenseYear(dto.getExpenseYear())
                    .expenseMonth(m)
                    .recurring(true)
                    .description(dto.getDescription())
                    .build();
                expenseRepository.save(expense);
            }
            // Return the one for the requested month
            return expenseRepository.findByExpenseYearAndExpenseMonth(dto.getExpenseYear(), dto.getExpenseMonth()).stream()
                .filter(e -> e.getType() == dto.getType() && e.getRecurring())
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Xərc yaradılmadı"));
        } else {
            Expense expense = Expense.builder()
                .type(dto.getType())
                .amount(dto.getAmount())
                .expenseYear(dto.getExpenseYear())
                .expenseMonth(dto.getExpenseMonth())
                .recurring(false)
                .description(dto.getDescription())
                .build();
            return toDto(expenseRepository.save(expense));
        }
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseDto dto) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Xərc tapılmadı"));

        boolean wasRecurring = expense.getRecurring();

        if (wasRecurring && dto.getRecurring()) {
            // Update ALL recurring expenses in this group (same type+description+year)
            for (int m = 1; m <= 12; m++) {
                List<Expense> monthExpenses = expenseRepository.findByExpenseYearAndExpenseMonth(expense.getExpenseYear(), m);
                for (Expense related : monthExpenses) {
                    if (related.getRecurring() && related.getType() == expense.getType()
                            && sameDescription(related.getDescription(), expense.getDescription())) {
                        related.setAmount(dto.getAmount());
                        related.setDescription(dto.getDescription());
                        expenseRepository.save(related);
                    }
                }
            }
        } else if (wasRecurring && !dto.getRecurring()) {
            // Was recurring, now single: delete all other months in this group, keep this one
            deleteRecurringGroup(expense.getType(), expense.getExpenseYear(), expense.getDescription());
            expense.setRecurring(false);
            expense.setAmount(dto.getAmount());
            expense.setDescription(dto.getDescription());
            expenseRepository.save(expense);
        } else if (dto.getRecurring()) {
            // Was single, now recurring: delete current, create for all months
            expenseRepository.delete(expense);
            for (int m = 1; m <= 12; m++) {
                Expense newExpense = Expense.builder()
                    .type(dto.getType())
                    .amount(dto.getAmount())
                    .expenseYear(dto.getExpenseYear())
                    .expenseMonth(m)
                    .recurring(true)
                    .description(dto.getDescription())
                    .build();
                expenseRepository.save(newExpense);
            }
            return expenseRepository.findByExpenseYearAndExpenseMonth(dto.getExpenseYear(), dto.getExpenseMonth()).stream()
                .filter(e -> e.getType() == dto.getType() && e.getRecurring())
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Xərc yenilənmədi"));
        } else {
            // Both single: just update this one
            expense.setType(dto.getType());
            expense.setAmount(dto.getAmount());
            expense.setExpenseYear(dto.getExpenseYear());
            expense.setExpenseMonth(dto.getExpenseMonth());
            expense.setRecurring(false);
            expense.setDescription(dto.getDescription());
            expenseRepository.save(expense);
        }

        return toDto(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Xərc tapılmadı"));

        if (expense.getRecurring()) {
            // Delete all recurring expenses of this type+year with the same description
            // (i.e. the same "group" of monthly records), not ALL of the same type
            deleteRecurringGroup(expense.getType(), expense.getExpenseYear(), expense.getDescription());
        } else {
            expenseRepository.deleteById(id);
        }
    }

    private void deleteRecurringGroup(ExpenseType type, Integer year, String description) {
        for (int m = 1; m <= 12; m++) {
            List<Expense> monthExpenses = expenseRepository.findByExpenseYearAndExpenseMonth(year, m);
            for (Expense e : monthExpenses) {
                if (e.getRecurring() && e.getType() == type
                        && sameDescription(e.getDescription(), description)) {
                    expenseRepository.deleteById(e.getId());
                }
            }
        }
    }

    private boolean sameDescription(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equals(b.trim());
    }

    @Transactional
    public void createRecurringExpenses(Integer year, Integer month) {
        List<Expense> recurring = expenseRepository.findByRecurring(true);
        for (Expense recurringExpense : recurring) {
            // Only create if doesn't already exist for this month
            boolean exists = expenseRepository.findByExpenseYearAndExpenseMonth(year, month).stream()
                .anyMatch(e -> e.getType() == recurringExpense.getType() && e.getRecurring()
                        && sameDescription(e.getDescription(), recurringExpense.getDescription()));
            if (!exists) {
                Expense newExpense = Expense.builder()
                    .type(recurringExpense.getType())
                    .amount(recurringExpense.getAmount())
                    .expenseYear(year)
                    .expenseMonth(month)
                    .recurring(true)
                    .description(recurringExpense.getDescription())
                    .build();
                expenseRepository.save(newExpense);
            }
        }
    }

    public BigDecimal getTotalExpenses(Integer year, Integer month) {
        return expenseRepository.findByExpenseYearAndExpenseMonth(year, month).stream()
            .filter(e -> !e.getRecurring())
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
        dto.setRecurring(e.getRecurring());
        dto.setDescription(e.getDescription());
        return dto;
    }
}
