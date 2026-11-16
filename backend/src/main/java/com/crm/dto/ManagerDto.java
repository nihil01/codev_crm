package com.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManagerDto {
    private Long id;
    @NotBlank @Email private String email;
    @NotBlank private String fullName;
    private String password;
    private boolean active;
}
