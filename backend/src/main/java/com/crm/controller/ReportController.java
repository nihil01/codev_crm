package com.crm.controller;

import com.crm.dto.ReportDto;
import com.crm.service.ReportService;
import com.crm.service.ExcelReportService;
import com.crm.service.BusinessAnalyticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExcelReportService excel;
    private final BusinessAnalyticsService analytics;

    @GetMapping("/monthly/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDto> getMonthlyReport(
            @PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(year, month));
    }
    @GetMapping("/analytics/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> analytics(@PathVariable int year,@PathVariable int month){
        return analytics.summary(year,month);
    }

    @GetMapping("/export/year/{year}/month/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(@PathVariable int year,@PathVariable int month){
        byte[] bytes=excel.export(year,month);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=codev-hesabat-"+year+"-"+month+".xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }
}

