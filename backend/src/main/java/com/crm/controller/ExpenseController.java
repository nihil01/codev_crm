package com.crm.controller;

import com.crm.dto.ExpenseDto;
import com.crm.enums.ExpenseType;
import com.crm.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<List<ExpenseDto>> getByPeriod(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(expenseService.getByPeriod(year, month));
    }

    @GetMapping("/recurring")
    public ResponseEntity<List<ExpenseDto>> getRecurring() {
        return ResponseEntity.ok(expenseService.getRecurring());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ExpenseDto>> getByType(@PathVariable ExpenseType type) {
        return ResponseEntity.ok(expenseService.getByType(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseDto> create(@Valid @RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseDto> update(@PathVariable Long id, @Valid @RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-recurring/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createRecurring(
            @PathVariable Integer year, @PathVariable Integer month) {
        expenseService.createRecurringExpenses(year, month);
        return ResponseEntity.ok().build();
    }
}
