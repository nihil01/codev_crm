package com.crm.repository;

import com.crm.entity.RecurringExpenseRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringExpenseRuleRepository extends JpaRepository<RecurringExpenseRule, Long> {
    List<RecurringExpenseRule> findByActiveTrueOrderByIdAsc();
    List<RecurringExpenseRule> findAllByOrderByIdAsc();
}
