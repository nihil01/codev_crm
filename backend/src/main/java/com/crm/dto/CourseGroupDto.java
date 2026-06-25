package com.crm.dto;

import com.crm.enums.GroupStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CourseGroupDto {
    private Long id;

    @NotBlank(message = "Qrup nömrəsi boş ola bilməz")
    private String groupNumber;

    @NotNull(message = "Başlama tarixi boş ola bilməz")
    private LocalDate startDate;

    @NotNull(message = "Davam müddəti boş ola bilməz")
    @Min(1)
    private Integer durationMonths;

    private LocalDate endDate;

    private GroupStatus status;
}
