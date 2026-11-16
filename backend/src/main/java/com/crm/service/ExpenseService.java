package com.crm.service;

import com.crm.dto.ExpenseDto;
import com.crm.dto.RecurringExpenseRuleDto;
import com.crm.entity.Expense;
import com.crm.entity.RecurringExpenseRule;
import com.crm.enums.ExpenseType;
import com.crm.repository.ExpenseRepository;
import com.crm.repository.RecurringExpenseRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final RecurringExpenseRuleRepository rules;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<ExpenseDto> getByPeriod(Integer year, Integer month) {
        YearMonth.of(year, month);
        return expenses.findByExpenseYearAndExpenseMonth(year, month).stream()
            .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getRecurring() {
        return expenses.findByRecurring(true).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getByType(ExpenseType type) {
        return expenses.findByType(type).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseRuleDto> getRules() {
        return rules.findAllByOrderByIdAsc().stream().map(this::toRuleDto).toList();
    }

    @Transactional
    public RecurringExpenseRuleDto addRule(RecurringExpenseRuleDto dto) {
        YearMonth start = YearMonth.of(dto.getStartYear(), dto.getStartMonth());
        validate(dto.getType(), dto.getAmount(), dto.getAdChannel());
        RecurringExpenseRule rule = RecurringExpenseRule.builder()
            .type(dto.getType()).amount(dto.getAmount()).description(dto.getDescription())
            .adChannel(normalize(dto.getAdChannel()))
            .startYear(start.getYear()).startMonth(start.getMonthValue())
            .active(dto.isActive()).build();
        rule = rules.saveAndFlush(rule);
        if (rule.isActive()) createOne(rule, start);
        return toRuleDto(rule);
    }

    @Transactional
    public RecurringExpenseRuleDto editRule(Long id, RecurringExpenseRuleDto dto) {
        RecurringExpenseRule rule = rules.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Daimi xərc qaydası tapılmadı"));
        validate(dto.getType(), dto.getAmount(), dto.getAdChannel());
        if (!rule.getStartYear().equals(dto.getStartYear())
                || !rule.getStartMonth().equals(dto.getStartMonth()))
            throw new IllegalArgumentException("Başlanğıc ay dəyişdirilə bilməz; yeni qayda yaradın");
        rule.setType(dto.getType());
        rule.setAmount(dto.getAmount());
        rule.setDescription(dto.getDescription());
        rule.setAdChannel(normalize(dto.getAdChannel()));
        rule.setActive(dto.isActive());
        rule = rules.save(rule);
        // Existing months are immutable from rule edits; month-specific edits use /expenses/{id}.
        if (rule.isActive()) createOne(rule, YearMonth.of(rule.getStartYear(), rule.getStartMonth()));
        return toRuleDto(rule);
    }

    @Transactional
    public RecurringExpenseRuleDto setRuleActive(Long id, boolean active) {
        RecurringExpenseRule rule = rules.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Daimi xərc qaydası tapılmadı"));
        rule.setActive(active);
        rule = rules.save(rule);
        if (active) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Baku"));
            createOne(rule, YearMonth.from(today));
        }
        return toRuleDto(rule);
    }

    @Transactional
    public ExpenseDto create(ExpenseDto dto) {
        YearMonth month = YearMonth.of(dto.getExpenseYear(), dto.getExpenseMonth());
        validate(dto.getType(), dto.getAmount(), dto.getAdChannel());
        if (Boolean.TRUE.equals(dto.getRecurring())) {
            RecurringExpenseRule rule = RecurringExpenseRule.builder()
                .type(dto.getType()).amount(dto.getAmount())
                .description(dto.getDescription()).adChannel(normalize(dto.getAdChannel()))
                .startYear(month.getYear()).startMonth(month.getMonthValue())
                .active(true).build();
            rule = rules.saveAndFlush(rule);
            createOne(rule, month);
            Long ruleId = rule.getId();
            return expenses.findByRecurringRuleIdAndExpenseYearAndExpenseMonth(
                ruleId, month.getYear(), month.getMonthValue()).map(this::toDto)
                .orElseThrow(() -> new IllegalStateException("Daimi xərc yaradıla bilmədi"));
        }
        return toDto(expenses.save(Expense.builder()
            .type(dto.getType()).amount(dto.getAmount()).description(dto.getDescription())
            .adChannel(normalize(dto.getAdChannel()))
            .expenseYear(month.getYear()).expenseMonth(month.getMonthValue())
            .recurring(false).cancelled(false).build()));
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseDto dto) {
        Expense expense = expenses.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Xərc tapılmadı"));
        validate(dto.getType(), dto.getAmount(), dto.getAdChannel());
        if (expense.getRecurringRule() != null) {
            // Treat one month as an independent accounting override.
            if (Boolean.FALSE.equals(dto.getRecurring()))
                throw new IllegalArgumentException("Daimi xərci dayandırmaq üçün qaydanı deaktiv edin");
            if (!expense.getExpenseYear().equals(dto.getExpenseYear())
                || !expense.getExpenseMonth().equals(dto.getExpenseMonth()))
                throw new IllegalArgumentException("Daimi xərcin ayını dəyişmək olmaz");
        } else if (Boolean.TRUE.equals(dto.getRecurring())) {
            // Explicit conversion of a one-time expense into a forward-looking rule.
            YearMonth month = YearMonth.of(dto.getExpenseYear(), dto.getExpenseMonth());
            RecurringExpenseRule rule = RecurringExpenseRule.builder()
                .type(dto.getType()).amount(dto.getAmount())
                .description(dto.getDescription()).adChannel(normalize(dto.getAdChannel()))
                .startYear(month.getYear()).startMonth(month.getMonthValue())
                .active(true).build();
            expense.setRecurringRule(rules.saveAndFlush(rule));
            expense.setRecurring(true);
        } else {
            expense.setExpenseYear(dto.getExpenseYear());
            expense.setExpenseMonth(dto.getExpenseMonth());
            expense.setRecurring(false);
        }
        expense.setType(dto.getType());
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setAdChannel(normalize(dto.getAdChannel()));
        return toDto(expenses.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenses.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Xərc tapılmadı"));
        if (expense.getRecurringRule() == null) {
            expenses.delete(expense);
        } else {
            // Soft-cancel only this month; unique key prevents scheduler recreation.
            expense.setCancelled(true);
            expenses.save(expense);
        }
    }

    @Transactional
    public ExpenseDto restore(Long id) {
        Expense expense = expenses.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Xərc tapılmadı"));
        if (expense.getRecurringRule() == null || !expense.isCancelled())
            throw new IllegalArgumentException("Bərpa ediləcək daimi aylıq xərc yoxdur");
        expense.setCancelled(false);
        return toDto(expenses.save(expense));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStart() {
        generateCurrentMonth();
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Baku")
    public void generateCurrentMonth() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Baku"));
        createRecurringExpenses(today.getYear(), today.getMonthValue());
    }

    @Transactional
    public void createRecurringExpenses(Integer year, Integer month) {
        YearMonth target = YearMonth.of(year, month);
        for (RecurringExpenseRule rule : rules.findByActiveTrueOrderByIdAsc()) createOne(rule, target);
    }

    private void createOne(RecurringExpenseRule rule, YearMonth target) {
        if (!rule.isActive() || target.isBefore(YearMonth.of(rule.getStartYear(), rule.getStartMonth())))
            return;
        // Concurrent schedulers are safe because a unique partial index exists on (rule, year, month).
        jdbc.update("""
            INSERT INTO expenses(type,amount,expense_year,expense_month,is_recurring,description,
                                 ad_channel,recurring_rule_id,cancelled,created_at,updated_at)
            VALUES (?,?,?,?,TRUE,?,?,?,FALSE,now(),now())
            ON CONFLICT (recurring_rule_id,expense_year,expense_month)
              WHERE recurring_rule_id IS NOT NULL DO NOTHING
            """, rule.getType().name(), rule.getAmount(), target.getYear(),
              target.getMonthValue(), rule.getDescription(), rule.getAdChannel(), rule.getId());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(Integer year, Integer month) {
        return sum(year, month, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRecurring(Integer year, Integer month) {
        return sum(year, month, true);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalOneTime(Integer year, Integer month) {
        return sum(year, month, false);
    }

    private BigDecimal sum(Integer year, Integer month, Boolean recurring) {
        return expenses.findByExpenseYearAndExpenseMonth(year, month).stream()
            .filter(e -> !e.isCancelled())
            .filter(e -> recurring == null || Boolean.TRUE.equals(e.getRecurring()) == recurring)
            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validate(ExpenseType type, BigDecimal amount, String channel) {
        if (type == null || amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Xərc növü və məbləği düzgün göstərilməlidir");
        if (type == ExpenseType.REKLAM && (channel == null || channel.isBlank()))
            throw new IllegalArgumentException("Reklam üçün kanal seçilməlidir");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ExpenseDto toDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(expense.getId());
        dto.setType(expense.getType());
        dto.setAmount(expense.getAmount());
        dto.setExpenseYear(expense.getExpenseYear());
        dto.setExpenseMonth(expense.getExpenseMonth());
        dto.setRecurring(Boolean.TRUE.equals(expense.getRecurring()));
        dto.setDescription(expense.getDescription());
        dto.setAdChannel(expense.getAdChannel());
        dto.setRecurringRuleId(expense.getRecurringRule() == null ? null : expense.getRecurringRule().getId());
        dto.setCancelled(expense.isCancelled());
        return dto;
    }

    private RecurringExpenseRuleDto toRuleDto(RecurringExpenseRule rule) {
        RecurringExpenseRuleDto dto = new RecurringExpenseRuleDto();
        dto.setId(rule.getId());
        dto.setType(rule.getType());
        dto.setAmount(rule.getAmount());
        dto.setDescription(rule.getDescription());
        dto.setAdChannel(rule.getAdChannel());
        dto.setStartYear(rule.getStartYear());
        dto.setStartMonth(rule.getStartMonth());
        dto.setActive(rule.isActive());
        return dto;
    }
}
