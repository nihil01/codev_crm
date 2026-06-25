package com.crm.service;

import com.crm.dto.StudentDto;
import com.crm.entity.Student;
import com.crm.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public List<StudentDto> getAll() {
        return studentRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<StudentDto> getActive() {
        return studentRepository.findByArchived(false).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<StudentDto> getArchived() {
        return studentRepository.findByArchived(true).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public StudentDto getById(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));
        return toDto(student);
    }

    public List<StudentDto> search(String query) {
        return studentRepository.search(query).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public StudentDto create(StudentDto dto) {
        if (studentRepository.existsByFin(dto.getFin())) {
            throw new RuntimeException("Bu FİN artıq mövcuddur");
        }

        Student student = Student.builder()
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .fin(dto.getFin())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .birthDate(dto.getBirthDate())
            .registrationDate(LocalDate.now())
            .archived(false)
            .build();

        return toDto(studentRepository.save(student));
    }

    @Transactional
    public StudentDto update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setBirthDate(dto.getBirthDate());
        student.setArchived(dto.isArchived());

        return toDto(studentRepository.save(student));
    }

    @Transactional
    public void archive(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));
        student.setArchived(true);
        studentRepository.save(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    private StudentDto toDto(Student s) {
        StudentDto dto = new StudentDto();
        dto.setId(s.getId());
        dto.setFirstName(s.getFirstName());
        dto.setLastName(s.getLastName());
        dto.setFin(s.getFin());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setBirthDate(s.getBirthDate());
        dto.setRegistrationDate(s.getRegistrationDate());
        dto.setArchived(s.isArchived());
        if (s.getGroup() != null) {
            dto.setGroupId(s.getGroup().getId());
        }
        return dto;
    }
}
