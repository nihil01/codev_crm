package com.crm.controller;

import com.crm.entity.Course;
import com.crm.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseRepository courses;
    public record CourseRequest(@NotBlank String name, String description) {}
    public record CourseResponse(Long id, String name, String description) {}

    @GetMapping
    @Transactional(readOnly=true)
    public List<CourseResponse> list() {
        return courses.findByActiveTrueOrderByNameAsc().stream()
            .map(c -> new CourseResponse(c.getId(),c.getName(),c.getDescription())).toList();
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        String name=request.name().trim();
        if(courses.existsByNameIgnoreCase(name))
            throw new IllegalArgumentException("Bu kurs artıq mövcuddur");
        Course c = Course.builder().name(name).description(request.description()).active(true).build();
        c = courses.save(c);
        return new CourseResponse(c.getId(),c.getName(),c.getDescription());
    }
}
