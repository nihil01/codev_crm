package com.crm;

import com.crm.entity.AuditEvent;
import com.crm.repository.AuditEventRepository;
import com.crm.service.ChangeAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangeAuditDiffTest {
    @Test
    void capturesChangedFieldsButNeverPasswords() {
        AuditEventRepository repo=mock(AuditEventRepository.class);
        ChangeAuditService service=new ChangeAuditService(repo,new ObjectMapper());
        service.record("UPDATE","MANAGER",7L,
            Map.of("fullName","A","active",true,"password","old-pass"),
            Map.of("fullName","B","active",true,"password","new-pass"));
        ArgumentCaptor<AuditEvent> cap=ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(cap.capture());
        AuditEvent event=cap.getValue();
        assertEquals("MANAGER",event.getEntityType());
        assertEquals(7L,event.getEntityId());
        assertTrue(event.getBeforeJson().contains("\"A\""));
        assertTrue(event.getAfterJson().contains("\"B\""));
        assertFalse(event.getBeforeJson().contains("password"));
        assertFalse(event.getAfterJson().contains("password"));
        assertFalse(event.getBeforeJson().contains("active"));
    }

    @Test
    void noChangesProducesNoAuditEvent() {
        AuditEventRepository repo=mock(AuditEventRepository.class);
        new ChangeAuditService(repo,new ObjectMapper()).record(
            "UPDATE","STUDENT",1L,Map.of("status","AKTIV"),Map.of("status","AKTIV"));
        verifyNoInteractions(repo);
    }
}
