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
class TeacherRoleIsolationTest {
    @Autowired MockMvc mvc;

    @Test
    void teacherCannotAccessStudentOrFinanceEndpoints() throws Exception {
        var user=SecurityMockMvcRequestPostProcessors.user("teacher@example.invalid").roles("TEACHER");
        mvc.perform(get("/api/students").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/payments").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/teachers").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/leads").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/monthly/year/2026/month/11").with(user))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/teacher-accounts").with(user))
            .andExpect(status().isForbidden());
    }
}
