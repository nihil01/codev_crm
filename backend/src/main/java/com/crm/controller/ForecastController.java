package com.crm.controller;

import com.crm.dto.ForecastDto;
import com.crm.service.ForecastService;
import com.crm.service.ScenarioForecastService;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;
    private final ScenarioForecastService scenarioForecastService;

    @GetMapping("/scenarios")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> getScenarios(@RequestParam(defaultValue="6") int months) {
        return scenarioForecastService.scenarios(months);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ForecastDto> getForecast() {
        return ResponseEntity.ok(forecastService.getForecast());
    }
}
