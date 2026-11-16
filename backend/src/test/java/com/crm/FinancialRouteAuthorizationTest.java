package com.crm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialRouteAuthorizationTest {
    @Autowired MockMvc mvc;

    @Test
    void managerDoesNotAccessAggregateFinance() throws Exception {
        var manager=SecurityMockMvcRequestPostProcessors.user("manager@example.invalid").roles("MANAGER");
        mvc.perform(get("/api/dashboard").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/expenses/recurring").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/monthly/year/2026/month/11").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/forecast/scenarios").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/1/teachers").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/payments/collected/year/2026/month/11").with(manager)).andExpect(status().isForbidden());
    }
}
