package com.crm.dto;

import com.crm.enums.ExpenseType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseDto {
    private Long id;

    @NotNull(message = "Xərc növü boş ola bilməz")
    private ExpenseType type;

    @NotNull(message = "Məbləğ boş ola bilməz")
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @Min(2020)
    private Integer expenseYear;

    @NotNull
    @Min(1) @Max(12)
    private Integer expenseMonth;

    private Boolean recurring;

    private String description;
    private String adChannel;
}
