package com.crm.dto;

import com.crm.enums.ExpenseType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecurringExpenseRuleDto {
    private Long id;
    @NotNull(message = "Xərc növü tələb olunur")
    private ExpenseType type;
    @NotNull @DecimalMin(value = "0.01", message = "Məbləğ sıfırdan böyük olmalıdır")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    private String description;
    private String adChannel;
    @NotNull @Min(2020)
    private Integer startYear;
    @NotNull @Min(1) @Max(12)
    private Integer startMonth;
    private boolean active = true;
}
