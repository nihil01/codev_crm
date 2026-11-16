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
    private final ChangeAuditService audit;

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

        TeacherDto created=toDto(teacherRepository.save(teacher));
        audit.record("CREATE","TEACHER",created.getId(),null,created);
        return created;
    }

    @Transactional
    public TeacherDto update(Long id, TeacherDto dto) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));

        TeacherDto before=toDto(teacher);
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setEmail(dto.getEmail());
        teacher.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            teacher.setStatus(dto.getStatus());
        }

        TeacherDto updated=toDto(teacherRepository.save(teacher));
        audit.record("UPDATE","TEACHER",id,before,updated);
        return updated;
    }

    @Transactional
    public void archive(Long id) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));
        TeacherDto before=toDto(teacher);
        teacher.setStatus(TeacherStatus.ARXIV);
        teacherRepository.save(teacher);
        audit.record("ARCHIVE","TEACHER",id,before,toDto(teacher));
    }

    @Transactional
    public void delete(Long id) {
        Teacher teacher=teacherRepository.findById(id)
            .orElseThrow(()->new IllegalArgumentException("Müəllim tapılmadı"));
        if(!teacher.getGroupTeachers().isEmpty())
            throw new IllegalArgumentException("Qrup tarixçəsi olan müəllim silinmir; arxivləşdirin");
        TeacherDto before=toDto(teacher);
        teacherRepository.delete(teacher);
        audit.record("DELETE","TEACHER",id,before,null);
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
