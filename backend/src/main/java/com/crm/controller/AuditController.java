package com.crm.controller;

import com.crm.entity.AuditEvent;
import com.crm.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {
    private final AuditEventRepository events;

    @GetMapping
    public List<AuditEvent> recent() {
        return events.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 500));
    }
}
