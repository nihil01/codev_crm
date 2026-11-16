package com.crm.controller;

import com.crm.dto.LeadDto;
import com.crm.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {
    private final LeadService service;
    @GetMapping
    public List<LeadDto> all() { return service.all(); }
    @GetMapping("/{id}")
    public LeadDto one(@PathVariable Long id) { return service.one(id); }
    @PostMapping
    public LeadDto create(@Valid @RequestBody LeadDto data) { return service.create(data); }
    @PutMapping("/{id}")
    public LeadDto update(@PathVariable Long id, @Valid @RequestBody LeadDto data) { return service.update(id, data); }
    @PostMapping("/{id}/convert")
    public java.util.Map<String,Long> convert(@PathVariable Long id) {
        return java.util.Map.of("studentId",service.convert(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
