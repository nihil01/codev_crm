package com.crm.repository;

import com.crm.entity.Expense;
import com.crm.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByExpenseYearAndExpenseMonth(Integer year, Integer month);
    List<Expense> findByType(ExpenseType type);
    List<Expense> findByIsRecurring(boolean isRecurring);
}
