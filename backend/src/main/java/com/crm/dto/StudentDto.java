package com.crm.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDto {
    private Long id;

    @NotBlank(message = "Ad boş ola bilməz")
    private String firstName;

    @NotBlank(message = "Soyad boş ola bilməz")
    private String lastName;

    @NotBlank(message = "FİN boş ola bilməz")
    @Size(min = 7, max = 7, message = "FİN 7 simvol olmalıdır")
    private String fin;

    @NotBlank(message = "E-poçt boş ola bilməz")
    @Email(message = "Yanlış e-poçt formatı")
    private String email;

    private String phone;

    private LocalDate birthDate;

    private Long groupId;

    private LocalDate registrationDate;

    private LocalDate issueDate;

    private boolean archived;
}
