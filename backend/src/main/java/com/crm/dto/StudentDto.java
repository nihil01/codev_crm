package com.crm.dto;

import jakarta.validation.constraints.*;
import com.crm.enums.LeadSource;
import com.crm.enums.StudentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDto {
    private Long id;

    @NotBlank(message = "Ad boş ola bilməz")
    private String firstName;

    @NotBlank(message = "Soyad boş ola bilməz")
    private String lastName;

    @Pattern(regexp = "^$|^[A-Za-z0-9]{7}$", message = "FİN 7 simvol olmalıdır")
    private String fin;

    @Email(message = "Yanlış e-poçt formatı")
    private String email;

    private String phone;

    private LocalDate birthDate;

    private LeadSource source;
    private String parentName;
    private String parentPhone;
    private String address;
    private String note;
    private StudentStatus status;
    private String exitReason;
    private LocalDate exitDate;

    private Long groupId;

    private LocalDate registrationDate;

    private LocalDate issueDate;

    private boolean archived;
}
