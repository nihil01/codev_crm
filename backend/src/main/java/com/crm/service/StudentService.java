package com.crm.service;

import com.crm.dto.StudentDto;
import com.crm.entity.Student;
import com.crm.enums.StudentStatus;
import com.crm.repository.StudentRepository;
import com.crm.repository.PaymentRepository;
import com.crm.repository.GroupStudentRepository;
import com.crm.repository.StudentFreezePeriodRepository;
import java.time.ZoneId;
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
    private final PaymentRepository payments;
    private final GroupStudentRepository memberships;
    private final StudentFreezePeriodRepository frozenMonths;
    private final StudentFreezeService freezes;
    private final ChangeAuditService audit;

    public List<StudentDto> getAll() {
        return studentRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<StudentDto> getActive() {
        return studentRepository.findByArchived(false).stream()
            .filter(x->x.getStatus()==StudentStatus.AKTIV)
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<StudentDto> getArchived() {
        return studentRepository.findByArchived(true).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public Student getEntity(Long id) { return studentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tələbə tapılmadı")); }

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
        String fin = normalizeFin(dto.getFin());
        if (fin != null && studentRepository.existsByFin(fin)) {
            throw new IllegalArgumentException("Bu FİN artıq mövcuddur");
        }
        validateExit(dto);

        Student student = Student.builder()
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .fin(fin)
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .reminderConsent(dto.isReminderConsent())
            .birthDate(dto.getBirthDate())
            .source(dto.getSource())
            .parentName(dto.getParentName())
            .parentPhone(dto.getParentPhone())
            .address(dto.getAddress())
            .note(dto.getNote())
            .status(dto.getStatus() == null ? StudentStatus.AKTIV : dto.getStatus())
            .exitReason(dto.getExitReason())
            .exitDate(dto.getStatus()==StudentStatus.AYRILIB?LocalDate.now(ZoneId.of("Asia/Baku")):null)
            .registrationDate(LocalDate.now())
            .archived(false)
            .build();

        StudentDto created = toDto(studentRepository.save(student));
        if (created.getStatus() == StudentStatus.DONDURULUB) freezeCurrentMonth(created.getId());
        audit.record("CREATE","STUDENT",created.getId(),null,created);
        return created;
    }

    @Transactional
    public StudentDto update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));

        StudentDto before = toDto(student);
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        String fin = normalizeFin(dto.getFin());
        if (fin != null && studentRepository.existsByFin(fin) && !fin.equals(student.getFin()))
            throw new IllegalArgumentException("Bu FİN artıq mövcuddur");
        validateExit(dto);
        student.setFin(fin);
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setReminderConsent(dto.isReminderConsent());
        student.setBirthDate(dto.getBirthDate());
        student.setSource(dto.getSource());
        student.setParentName(dto.getParentName());
        student.setParentPhone(dto.getParentPhone());
        student.setAddress(dto.getAddress());
        student.setNote(dto.getNote());
        student.setStatus(dto.getStatus() == null ? StudentStatus.AKTIV : dto.getStatus());
        student.setExitReason(dto.getStatus() == StudentStatus.AYRILIB ? dto.getExitReason() : null);
        if(dto.getStatus() == StudentStatus.AYRILIB && before.getStatus()!=StudentStatus.AYRILIB)
            student.setExitDate(LocalDate.now(ZoneId.of("Asia/Baku")));
        else if(dto.getStatus()!=StudentStatus.AYRILIB) student.setExitDate(null);
        student.setArchived(dto.isArchived());

        StudentDto changed = toDto(studentRepository.save(student));
        if (changed.getStatus() == StudentStatus.DONDURULUB &&
            before.getStatus() != StudentStatus.DONDURULUB) freezeCurrentMonth(id);
        audit.record("UPDATE","STUDENT",id,before,changed);
        return changed;
    }

    @Transactional
    public void archive(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));
        StudentDto before = toDto(student);
        student.setArchived(true);
        StudentDto changed = toDto(studentRepository.save(student));
        audit.record("ARCHIVE","STUDENT",id,before,changed);
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tələbə tapılmadı"));
        if (!payments.findByStudentId(id).isEmpty() || !memberships.findByStudentId(id).isEmpty())
            throw new IllegalArgumentException("Tarixçəsi olan tələbə silinmir; arxivləşdirin");
        StudentDto before = toDto(student);
        studentRepository.delete(student);
        audit.record("DELETE","STUDENT",id,before,null);
    }

    private void freezeCurrentMonth(Long studentId) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Baku"));
        if (!frozenMonths.existsByStudentIdAndYearAndMonth(studentId,now.getYear(),now.getMonthValue())) {
            var due = payments.findByStudentId(studentId).stream()
                .filter(x->x.getPaymentYear()==now.getYear() && x.getPaymentMonth()==now.getMonthValue())
                .toList();
            if(due.stream().noneMatch(x->freezes.collectedForInvoice(x.getId()).signum()>0))
                freezes.freeze(studentId,now.getYear(),now.getMonthValue());
        }
    }

    private static String normalizeFin(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static void validateExit(StudentDto dto) {
        if (dto.getStatus() == StudentStatus.AYRILIB &&
            (dto.getExitReason() == null || dto.getExitReason().isBlank())) {
            throw new IllegalArgumentException("Ayrılmış tələbə üçün səbəb məcburidir");
        }
    }

    private StudentDto toDto(Student s) {
        StudentDto dto = new StudentDto();
        dto.setId(s.getId());
        dto.setFirstName(s.getFirstName());
        dto.setLastName(s.getLastName());
        dto.setFin(s.getFin());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setReminderConsent(s.isReminderConsent());
        dto.setBirthDate(s.getBirthDate());
        dto.setSource(s.getSource());
        dto.setParentName(s.getParentName());
        dto.setParentPhone(s.getParentPhone());
        dto.setAddress(s.getAddress());
        dto.setNote(s.getNote());
        dto.setStatus(s.getStatus());
        dto.setExitReason(s.getExitReason());
        dto.setExitDate(s.getExitDate());
        dto.setRegistrationDate(s.getRegistrationDate());
        dto.setArchived(s.isArchived());
        if (s.getGroup() != null) {
            dto.setGroupId(s.getGroup().getId());
        }
        return dto;
    }
}
