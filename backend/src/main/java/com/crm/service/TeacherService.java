package com.crm.service;

import com.crm.dto.TeacherDto;
import com.crm.entity.Teacher;
import com.crm.enums.TeacherStatus;
import com.crm.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public List<TeacherDto> getAll() {
        return teacherRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<TeacherDto> getActive() {
        return teacherRepository.findByStatus(TeacherStatus.AKTIV).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public TeacherDto getById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));
        return toDto(teacher);
    }

    @Transactional
    public TeacherDto create(TeacherDto dto) {
        if (teacherRepository.existsByFin(dto.getFin())) {
            throw new RuntimeException("Bu FİN artıq mövcuddur");
        }

        Teacher teacher = Teacher.builder()
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .fin(dto.getFin())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .status(TeacherStatus.AKTIV)
            .build();

        return toDto(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherDto update(Long id, TeacherDto dto) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));

        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setEmail(dto.getEmail());
        teacher.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            teacher.setStatus(dto.getStatus());
        }

        return toDto(teacherRepository.save(teacher));
    }

    @Transactional
    public void archive(Long id) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));
        teacher.setStatus(TeacherStatus.ARXIV);
        teacherRepository.save(teacher);
    }

    @Transactional
    public void delete(Long id) {
        teacherRepository.deleteById(id);
    }

    private TeacherDto toDto(Teacher t) {
        TeacherDto dto = new TeacherDto();
        dto.setId(t.getId());
        dto.setFirstName(t.getFirstName());
        dto.setLastName(t.getLastName());
        dto.setFin(t.getFin());
        dto.setEmail(t.getEmail());
        dto.setPhone(t.getPhone());
        dto.setStatus(t.getStatus());
        return dto;
    }
}
