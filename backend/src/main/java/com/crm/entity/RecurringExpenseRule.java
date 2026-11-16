package com.crm.entity;

import com.crm.enums.ExpenseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_expense_rules")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RecurringExpenseRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private ExpenseType type;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    private String description;
    private String adChannel;
    @Column(nullable = false)
    private Integer startYear;
    @Column(nullable = false)
    private Integer startMonth;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "legacy_key", unique = true)
    private String legacyKey;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
