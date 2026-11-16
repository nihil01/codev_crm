package com.crm.controller;

import com.crm.entity.Teacher;
import com.crm.entity.User;
import com.crm.enums.Role;
import com.crm.enums.TeacherStatus;
import com.crm.repository.TeacherRepository;
import com.crm.repository.UserRepository;
import com.crm.service.ChangeAuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/admin/teacher-accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TeacherAccountController {
    private final UserRepository users;
    private final TeacherRepository teachers;
    private final PasswordEncoder encoder;
    private final ChangeAuditService audit;

    public record CreateRequest(@NotNull Long teacherId,@NotBlank String password) {}
    public record Account(Long id,Long teacherId,String email,String fullName,boolean active) {}

    @GetMapping
    @Transactional(readOnly=true)
    public List<Account> list() {
        return users.findAll().stream().filter(u->u.getRole()==Role.TEACHER)
            .map(this::toDto).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        if(request.password().length()<12)
            return ResponseEntity.badRequest().body(Map.of("message","Şifrə ən azı 12 simvol olmalıdır"));
        Teacher teacher=teachers.findById(request.teacherId())
            .orElseThrow(()->new IllegalArgumentException("Müəllim tapılmadı"));
        if(teacher.getStatus()!=TeacherStatus.AKTIV)
            throw new IllegalArgumentException("Arxiv müəllim üçün hesab yaradıla bilməz");
        String email=teacher.getEmail().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email))
            throw new IllegalArgumentException("Bu e-poçt üçün hesab artıq mövcuddur");
        if(users.findAll().stream().anyMatch(u->u.getTeacher()!=null &&
            u.getTeacher().getId().equals(teacher.getId())))
            throw new IllegalArgumentException("Müəllimin artıq hesabı var");
        User user=User.builder().email(email).password(encoder.encode(request.password()))
            .fullName(teacher.getFirstName()+" "+teacher.getLastName())
            .role(Role.TEACHER).teacher(teacher).active(true).build();
        Account account=toDto(users.saveAndFlush(user));
        audit.record("CREATE","TEACHER_ACCOUNT",account.id(),null,account);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{id}/active")
    @Transactional
    public Account changeActive(@PathVariable Long id,@RequestParam boolean active) {
        User user=users.findById(id)
            .orElseThrow(()->new IllegalArgumentException("Hesab tapılmadı"));
        if(user.getRole()!=Role.TEACHER)
            throw new IllegalArgumentException("Müəllim hesabı tapılmadı");
        if(active && user.getTeacher().getStatus()!=TeacherStatus.AKTIV)
            throw new IllegalArgumentException("Arxiv müəllim aktivləşdirilə bilməz");
        Account before=toDto(user);
        user.setActive(active);
        Account changed=toDto(users.save(user));
        audit.record("UPDATE","TEACHER_ACCOUNT",id,before,changed);
        return changed;
    }

    private Account toDto(User user) {
        Teacher teacher=user.getTeacher();
        return new Account(user.getId(),teacher.getId(),user.getEmail(),
            user.getFullName(),user.isActive());
    }
}
