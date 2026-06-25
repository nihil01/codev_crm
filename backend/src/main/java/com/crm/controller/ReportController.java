package com.crm.controller;

import com.crm.dto.ReportDto;
import com.crm.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> getMonthlyReport(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }
}
