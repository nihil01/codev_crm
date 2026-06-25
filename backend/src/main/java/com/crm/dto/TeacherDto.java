package com.crm.dto;

import com.crm.enums.TeacherStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TeacherDto {
    private Long id;

    @NotBlank(message = "Ad boş ola bilməz")
    private String firstName;

    @NotBlank(message = "Soyad boş ola bilməz")
    private String lastName;

    @NotBlank(message = "FİN boş ola bilməz")
    @Size(min = 7, max = 7)
    private String fin;

    @NotBlank(message = "E-poçt boş ola bilməz")
    @Email
    private String email;

    @Email
    private String gmail;

    private String phone;

    private TeacherStatus status;
}
