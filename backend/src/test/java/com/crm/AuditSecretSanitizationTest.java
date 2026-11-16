package com.crm;

import com.crm.repository.AuditEventRepository;
import com.crm.service.ChangeAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditSecretSanitizationTest {
    @Test
    void nestedSecretsAreExcludedBeforePersistingDiffs() {
        ChangeAuditService service=new ChangeAuditService(
            org.mockito.Mockito.mock(AuditEventRepository.class), new ObjectMapper());
        Map<String,Object> data=Map.of(
            "id",11L,
            "details",Map.of("name","Test","password","sensitive","refreshToken","sensitive"),
            "children",List.of(Map.of("api_key","sensitive","amount",25))
        );
        Object sanitized=ReflectionTestUtils.invokeMethod(service,"safeMap",data);
        String output=sanitized.toString();
        assertTrue(output.contains("name=Test"));
        assertTrue(output.contains("amount=25"));
        assertFalse(output.contains("sensitive"));
        assertFalse(output.contains("password"));
        assertFalse(output.contains("refreshToken"));
        assertFalse(output.contains("api_key"));
    }
}
