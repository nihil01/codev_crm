package com.crm;

import com.crm.dto.CourseGroupDto;
import com.crm.entity.*;
import com.crm.enums.*;
import com.crm.repository.CourseGroupRepository;
import com.crm.service.CourseGroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupTableMetadataTest {
    @Mock CourseGroupRepository groups;
    @InjectMocks CourseGroupService service;

    @Test
    void tableCountsOnlyActiveUniqueEnrolmentsAndShowsTeacherNames() {
        CourseGroup group=CourseGroup.builder().id(9L)
            .groupNumber("G-09").startDate(LocalDate.of(2026,11,1))
            .durationMonths(4).status(GroupStatus.BASLAYIB).build();
        Student active=Student.builder().id(1L).status(StudentStatus.AKTIV).archived(false).build();
        Student departed=Student.builder().id(2L).status(StudentStatus.AKTIV).archived(false).build();
        Student archived=Student.builder().id(3L).status(StudentStatus.AKTIV).archived(true).build();
        Student frozen=Student.builder().id(4L).status(StudentStatus.DONDURULUB).archived(false).build();
        group.getGroupStudents().add(GroupStudent.builder().student(active).group(group).build());
        group.getGroupStudents().add(GroupStudent.builder().student(departed).group(group)
            .leftAt(LocalDate.of(2026,11,15)).build());
        group.getGroupStudents().add(GroupStudent.builder().student(archived).group(group).build());
        group.getGroupStudents().add(GroupStudent.builder().student(frozen).group(group).build());
        Teacher teacher=Teacher.builder().id(5L).firstName("Aysel").lastName("Aliyeva").build();
        group.getGroupTeachers().add(GroupTeacher.builder().teacher(teacher).group(group).build());

        when(groups.findAll()).thenReturn(List.of(group));
        CourseGroupDto result=service.getAll().get(0);
        assertEquals(1L,result.getActiveStudentCount());
        assertEquals(List.of("Aysel Aliyeva"),result.getTeacherNames());
        assertEquals("G-09",result.getGroupNumber());
    }
}
