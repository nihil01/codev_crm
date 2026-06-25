package com.crm.dto;

import com.crm.enums.PaymentMethod;
import com.crm.enums.PaymentStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentDto {
    private Long id;

    @NotNull
    private Long studentId;

    @NotNull
    private Long groupId;

    @NotNull
    @Min(2020)
    private Integer paymentYear;

    @NotNull
    @Min(1) @Max(12)
    private Integer paymentMonth;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private LocalDate paymentDate;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String note;

    // Read-only
    private String studentFirstName;
    private String studentLastName;
    private String groupNumber;
}
