package com.crm.controller;

import com.crm.dto.ManagerDto;
import com.crm.entity.User;
import com.crm.enums.Role;
import com.crm.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/managers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ManagerController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @GetMapping
    @Transactional(readOnly = true)
    public List<ManagerDto> all() {
        return users.findAll().stream().filter(u -> u.getRole() == Role.MANAGER).map(this::dto).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody ManagerDto data) {
        if (data.getPassword() == null || data.getPassword().length() < 12)
            return ResponseEntity.badRequest().body(Map.of("message", "Şifrə ən azı 12 simvol olmalıdır"));
        String email = data.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent())
            return ResponseEntity.badRequest().body(Map.of("message", "Bu e-poçt artıq mövcuddur"));
        User user = User.builder()
            .email(email)
            .fullName(data.getFullName().trim())
            .password(encoder.encode(data.getPassword()))
            .role(Role.MANAGER)
            .active(true)
            .build();
        return ResponseEntity.ok(dto(users.save(user)));
    }

    @PutMapping("/{id}/active")
    @Transactional
    public ManagerDto setActive(@PathVariable Long id, @RequestParam boolean active) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Menecer tapılmadı"));
        if (user.getRole() != Role.MANAGER) throw new IllegalArgumentException("Menecer tapılmadı");
        user.setActive(active);
        return dto(users.save(user));
    }

    private ManagerDto dto(User user) {
        ManagerDto out = new ManagerDto();
        out.setId(user.getId());
        out.setEmail(user.getEmail());
        out.setFullName(user.getFullName());
        out.setActive(user.isActive());
        return out;
    }
}
