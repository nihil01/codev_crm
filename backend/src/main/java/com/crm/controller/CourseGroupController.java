package com.crm.controller;

import com.crm.dto.CourseGroupDto;
import com.crm.dto.GroupStudentDto;
import com.crm.dto.GroupTeacherDto;
import com.crm.service.CourseGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class CourseGroupController {

    private final CourseGroupService courseGroupService;

    @GetMapping
    public ResponseEntity<List<CourseGroupDto>> getAll() {
        return ResponseEntity.ok(courseGroupService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CourseGroupDto>> getActive() {
        return ResponseEntity.ok(courseGroupService.getActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseGroupDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseGroupService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseGroupDto> create(@Valid @RequestBody CourseGroupDto dto) {
        return ResponseEntity.ok(courseGroupService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseGroupDto> update(@PathVariable Long id, @Valid @RequestBody CourseGroupDto dto) {
        return ResponseEntity.ok(courseGroupService.update(id, dto));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> complete(@PathVariable Long id) {
        courseGroupService.complete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspend(@PathVariable Long id) {
        courseGroupService.suspend(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resume(@PathVariable Long id) {
        courseGroupService.resume(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseGroupService.delete(id);
        return ResponseEntity.ok().build();
    }

    // Group Students
    @GetMapping("/{id}/students")
    public ResponseEntity<List<GroupStudentDto>> getGroupStudents(@PathVariable Long id) {
        return ResponseEntity.ok(courseGroupService.getGroupStudents(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupStudentDto> addStudent(
            @PathVariable Long id, @Valid @RequestBody GroupStudentDto dto) {
        return ResponseEntity.ok(courseGroupService.addStudentToGroup(id, dto));
    }

    @DeleteMapping("/{groupId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStudent(@PathVariable Long groupId, @PathVariable Long studentId,
            @RequestParam String reason) {
        courseGroupService.removeStudentFromGroup(groupId, studentId, reason);
        return ResponseEntity.ok().build();
    }

    // Group Teachers
    @GetMapping("/{id}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroupTeacherDto>> getGroupTeachers(@PathVariable Long id) {
        return ResponseEntity.ok(courseGroupService.getGroupTeachers(id));
    }

    @PostMapping("/{id}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupTeacherDto> assignTeacher(
            @PathVariable Long id, @Valid @RequestBody GroupTeacherDto dto) {
        return ResponseEntity.ok(courseGroupService.assignTeacher(id, dto));
    }

    @DeleteMapping("/{groupId}/teachers/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeTeacher(@PathVariable Long groupId, @PathVariable Long teacherId) {
        courseGroupService.removeTeacher(groupId, teacherId);
        return ResponseEntity.ok().build();
    }
}
