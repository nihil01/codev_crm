package com.crm.controller;

import com.crm.entity.GroupTeacher;
import com.crm.entity.User;
import com.crm.enums.TeacherStatus;
import com.crm.repository.GroupTeacherRepository;
import com.crm.repository.TeacherShareAllocationRepository;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/teacher-portal")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherPortalController {
    private final UserRepository users;
    private final GroupTeacherRepository groupTeachers;
    private final TeacherShareAllocationRepository allocations;

    public record MyGroup(Long groupId,String groupNumber,String courseName,
                          String status,String lessonDays,String lessonTime,
                          String trainingMode,String room,BigDecimal sharePercent,
                          BigDecimal earnedForMonth,Integer effectiveMonth,Integer effectiveYear,
                          Integer untilMonth,Integer untilYear) {}
    public record MyOverview(Integer year,Integer month,
                             BigDecimal totalEarned,List<MyGroup> groups) {}

    @GetMapping
    @Transactional(readOnly=true)
    public MyOverview overview(@RequestParam int year,@RequestParam int month) {
        YearMonth period=YearMonth.of(year,month);
        String login=SecurityContextHolder.getContext().getAuthentication().getName();
        User user=users.findByEmail(login)
            .orElseThrow(()->new AccessDeniedException("Hesab tapılmadı"));
        if(user.getTeacher()==null || user.getTeacher().getStatus()!=TeacherStatus.AKTIV)
            throw new AccessDeniedException("Müəllim hesabı aktiv deyil");
        var start=period.atDay(1).atStartOfDay();
        var end=period.plusMonths(1).atDay(1).atStartOfDay();
        List<MyGroup> groups=groupTeachers.findByTeacherId(user.getTeacher().getId()).stream()
            .map(a->toDto(a,start,end)).toList();
        BigDecimal total=groups.stream().map(MyGroup::earnedForMonth)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        return new MyOverview(year,month,total,groups);
    }

    private MyGroup toDto(GroupTeacher assignment,
                          java.time.LocalDateTime start,java.time.LocalDateTime end) {
        var group=assignment.getGroup();
        BigDecimal earned=allocations.sumForAssignment(assignment.getId(),start,end);
        return new MyGroup(group.getId(),group.getGroupNumber(),
            group.getCourse()==null?null:group.getCourse().getName(),
            group.getStatus().name(),group.getLessonDays(),group.getLessonTime(),
            group.getTrainingMode()==null?null:group.getTrainingMode().name(),
            group.getRoom(),assignment.getSalaryPercentage(),earned,
            assignment.getEffectiveMonth(),assignment.getEffectiveYear(),
            assignment.getEffectiveUntilMonth(),assignment.getEffectiveUntilYear());
    }
}
