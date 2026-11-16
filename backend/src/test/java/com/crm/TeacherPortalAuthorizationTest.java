package com.crm;

import com.crm.controller.TeacherPortalController;
import com.crm.entity.*;
import com.crm.enums.*;
import com.crm.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherPortalAuthorizationTest {
    @Mock UserRepository users;
    @Mock GroupTeacherRepository assignments;
    @Mock TeacherShareAllocationRepository allocations;
    @InjectMocks TeacherPortalController portal;

    @AfterEach
    void clearContext(){ SecurityContextHolder.clearContext(); }

    @Test
    void onlyLoggedInTeacherAssignmentsContributeToDashboard() {
        Teacher teacher=Teacher.builder().id(3L).firstName("A").lastName("B")
            .status(TeacherStatus.AKTIV).build();
        User login=User.builder().id(9L).email("teacher@example.invalid")
            .teacher(teacher).role(Role.TEACHER).active(true).build();
        CourseGroup group=CourseGroup.builder().id(7L).groupNumber("G-07")
            .status(GroupStatus.BASLAYIB).build();
        GroupTeacher assignment=GroupTeacher.builder().id(22L).teacher(teacher)
            .group(group).salaryPercentage(new BigDecimal("25.00"))
            .effectiveMonth(1).effectiveYear(2026).build();
        when(users.findByEmail("teacher@example.invalid")).thenReturn(Optional.of(login));
        when(assignments.findByTeacherId(3L)).thenReturn(List.of(assignment));
        when(allocations.sumForAssignment(eq(22L),any(LocalDateTime.class),any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("75.00"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("teacher@example.invalid","N/A"));
        var result=portal.overview(2026,11);
        assertEquals(new BigDecimal("75.00"),result.totalEarned());
        assertEquals(1,result.groups().size());
        verify(assignments).findByTeacherId(3L);
        verifyNoMoreInteractions(assignments);
    }

    @Test
    void accountWithoutLinkedTeacherIsDenied() {
        User login=User.builder().email("unlinked@example.invalid")
            .role(Role.TEACHER).active(true).build();
        when(users.findByEmail("unlinked@example.invalid")).thenReturn(Optional.of(login));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("unlinked@example.invalid","N/A"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            ()->portal.overview(2026,11));
        verifyNoInteractions(assignments);
    }
}
