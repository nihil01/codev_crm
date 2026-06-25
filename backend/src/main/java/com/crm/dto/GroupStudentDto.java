package com.crm.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GroupStudentDto {
    private Long id;

    @NotNull(message = "Tələbə seçilməlidir")
    private Long studentId;

    @NotNull(message = "Məbləğ boş ola bilməz")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal monthlyAmount;

    @NotNull(message = "Qoşulma tarixi boş ola bilməz")
    private LocalDate joinDate;

    private String note;

    // Read-only fields
    private String studentFirstName;
    private String studentLastName;
    private String studentFin;
}
