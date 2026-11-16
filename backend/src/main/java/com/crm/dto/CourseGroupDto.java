package com.crm.dto;

import com.crm.enums.GroupStatus;
import com.crm.enums.TrainingMode;
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
    private Long courseId;
    private String courseName;
    private String lessonDays;
    private String lessonTime;
    private TrainingMode trainingMode;
    private String room;
    @Min(1)
    private Integer maxStudents;
}
