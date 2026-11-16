package com.crm.controller;

import com.crm.enums.ReminderDispatchStatus;
import com.crm.repository.ReminderDispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reminder-dispatches")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReminderDispatchController {
    private final ReminderDispatchRepository repo;

    public record DispatchStatus(Long id,Long invoiceId,String kind,String status,
                                 int attempts,LocalDateTime createdAt,LocalDateTime sentAt,
                                 String lastError) {}

    @GetMapping
    @Transactional(readOnly=true)
    public List<DispatchStatus> recent() {
        return repo.findAll(PageRequest.of(0,200,org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC,"id"))).stream()
            .map(x->new DispatchStatus(x.getId(),x.getPayment().getId(),
                x.getKind(),x.getStatus().name(),x.getAttempts(),
                x.getCreatedAt(),x.getSentAt(),x.getLastError())).toList();
    }
}
