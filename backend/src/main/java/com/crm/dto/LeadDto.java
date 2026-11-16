package com.crm.dto;

import com.crm.enums.LeadSource;
import com.crm.enums.LeadStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeadDto {
    private Long id;
    @NotBlank private String fullName;
    @NotBlank private String phone;
    @NotNull private LeadSource source;
    @NotNull private LeadStage stage;
    private Long assignedManagerId;
    private String assignedManagerName;
    private LocalDateTime consultationAt;
    private String lostReason;
    private String interestedCourse;
    private String note;
    private LocalDateTime createdAt;
}
