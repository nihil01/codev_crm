package com.crm.dto;

import com.crm.enums.SalaryStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroupTeacherDto {
    private Long id;

    @NotNull(message = "Müəllim seçilməlidir")
    private Long teacherId;

    @NotNull(message = "Maaş faizi boş ola bilməz")
    @DecimalMin(value = "1", message = "Faiz 1-dən böyük olmalıdır")
    @DecimalMax(value = "100", message = "Faiz 100-dən kiçik olmalıdır")
    private BigDecimal salaryPercentage;

    private BigDecimal calculatedSalary;

    private SalaryStatus salaryStatus;

    private Integer effectiveMonth;
    private Integer effectiveYear;

    // Read-only
    private String teacherFirstName;
    private String teacherLastName;
}
