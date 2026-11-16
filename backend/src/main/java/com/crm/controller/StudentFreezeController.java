package com.crm.controller;

import com.crm.service.StudentFreezeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/freezes")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class StudentFreezeController {
    private final StudentFreezeService service;
    @GetMapping
    public List<StudentFreezeService.FreezeDto> list(@PathVariable Long studentId) {
        return service.list(studentId);
    }
    @PostMapping
    public StudentFreezeService.FreezeDto freeze(@PathVariable Long studentId,
            @RequestParam int year,@RequestParam int month) {
        return service.freeze(studentId,year,month);
    }
    @DeleteMapping
    public ResponseEntity<Void> unfreeze(@PathVariable Long studentId,
            @RequestParam int year,@RequestParam int month) {
        service.unfreeze(studentId,year,month);
        return ResponseEntity.noContent().build();
    }
}
