package com.crm.service;

import com.crm.dto.CourseGroupDto;
import com.crm.dto.GroupStudentDto;
import com.crm.dto.GroupTeacherDto;
import com.crm.entity.*;
import com.crm.enums.GroupStatus;
import com.crm.enums.PaymentStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseGroupService {

    private final CourseGroupRepository groupRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final GroupTeacherRepository groupTeacherRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public List<CourseGroupDto> getAll() {
        return groupRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<CourseGroupDto> getActive() {
        return groupRepository.findByStatus(GroupStatus.AKTIV).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public CourseGroupDto getById(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        return toDto(group);
    }

    @Transactional
    public CourseGroupDto create(CourseGroupDto dto) {
        CourseGroup group = CourseGroup.builder()
            .groupNumber(dto.getGroupNumber())
            .startDate(dto.getStartDate())
            .durationMonths(dto.getDurationMonths())
            .status(GroupStatus.AKTIV)
            .build();

        group.calculateEndDate();
        return toDto(groupRepository.save(group));
    }

    @Transactional
    public CourseGroupDto update(Long id, CourseGroupDto dto) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));

        group.setGroupNumber(dto.getGroupNumber());
        group.setStartDate(dto.getStartDate());
        group.setDurationMonths(dto.getDurationMonths());
        if (dto.getStatus() != null) {
            group.setStatus(dto.getStatus());
        }
        group.calculateEndDate();

        return toDto(groupRepository.save(group));
    }

    @Transactional
    public void complete(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        group.setStatus(GroupStatus.TAMAMLANMIS);
        groupRepository.save(group);
    }

    @Transactional
    public void suspend(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        group.setStatus(GroupStatus.DAYANDIRILMIS);
        groupRepository.save(group);
    }

    @Transactional
    public void resume(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        if (group.getStatus() != GroupStatus.DAYANDIRILMIS) {
            throw new RuntimeException("Yalnız dayandırılmış qrupları bərpa etmək olar");
        }
        group.setStatus(GroupStatus.AKTIV);
        groupRepository.save(group);
    }

    // === Group Students ===

    public List<GroupStudentDto> getGroupStudents(Long groupId) {
        return groupStudentRepository.findByGroupId(groupId).stream()
            .map(this::toGroupStudentDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public GroupStudentDto addStudentToGroup(Long groupId, GroupStudentDto dto) {
        CourseGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        Student student = studentRepository.findById(dto.getStudentId())
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));

        if (groupStudentRepository.findByGroupIdAndStudentId(groupId, dto.getStudentId()).isPresent()) {
            throw new RuntimeException("Bu tələbə artıq bu qrupdadır");
        }

        GroupStudent gs = GroupStudent.builder()
            .group(group)
            .student(student)
            .monthlyAmount(dto.getMonthlyAmount())
            .joinDate(dto.getJoinDate())
            .note(dto.getNote())
            .build();

        // Create payment schedule for all months
        createPaymentSchedule(student, group, dto.getMonthlyAmount(), dto.getJoinDate());

        // Only set primary group if student doesn't have one yet
        if (student.getGroup() == null) {
            student.setGroup(group);
            studentRepository.save(student);
        }

        return toGroupStudentDto(groupStudentRepository.save(gs));
    }

    @Transactional
    public void removeStudentFromGroup(Long groupId, Long studentId) {
        GroupStudent gs = groupStudentRepository.findByGroupIdAndStudentId(groupId, studentId)
            .orElseThrow(() -> new RuntimeException("Tələbə bu qrupda deyil"));

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null && student.getGroup() != null && student.getGroup().getId().equals(groupId)) {
            // Student's primary group was removed — find another group or set null
            List<GroupStudent> remaining = groupStudentRepository.findByStudentId(studentId);
            CourseGroup newPrimary = remaining.stream()
                .map(GroupStudent::getGroup)
                .filter(g -> !g.getId().equals(groupId))
                .findFirst()
                .orElse(null);
            student.setGroup(newPrimary);
            studentRepository.save(student);
        }

        groupStudentRepository.delete(gs);
    }

    // === Group Teachers ===

    public List<GroupTeacherDto> getGroupTeachers(Long groupId) {
        return groupTeacherRepository.findByGroupId(groupId).stream()
            .map(this::toGroupTeacherDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public GroupTeacherDto assignTeacher(Long groupId, GroupTeacherDto dto) {
        CourseGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Müəllim tapılmadı"));

        GroupTeacher gt = GroupTeacher.builder()
            .group(group)
            .teacher(teacher)
            .salaryPercentage(dto.getSalaryPercentage())
            .effectiveMonth(dto.getEffectiveMonth())
            .effectiveYear(dto.getEffectiveYear())
            .build();

        return toGroupTeacherDto(groupTeacherRepository.save(gt));
    }

    @Transactional
    public void removeTeacher(Long groupId, Long teacherId) {
        List<GroupTeacher> gts = groupTeacherRepository.findByGroupId(groupId);
        gts.stream()
            .filter(gt -> gt.getTeacher().getId().equals(teacherId))
            .findFirst()
            .ifPresent(groupTeacherRepository::delete);
    }

    // === Payment Schedule ===

    private void createPaymentSchedule(Student student, CourseGroup group, BigDecimal monthlyAmount, LocalDate joinDate) {
        // Billing starts at enrolment. Exactly durationMonths invoices, not durationMonths+1.
        LocalDate first = joinDate == null ? group.getStartDate() : joinDate;
        for (int i=0; i<group.getDurationMonths(); i++) {
            LocalDate due = first.plusMonths(i);
            Payment payment = Payment.builder()
                .student(student).group(group)
                .paymentYear(due.getYear()).paymentMonth(due.getMonthValue())
                .amount(monthlyAmount).dueDate(due)
                .discountAmount(BigDecimal.ZERO).frozen(false)
                .status(PaymentStatus.GOZLENILIR).build();
            paymentRepository.save(payment);
        }
    }

    // === Group Summary Calculations ===

    public BigDecimal calculateGroupCollected(Long groupId, Integer year, Integer month) {
        return paymentService.getCollectedForGroupMonth(groupId, year, month);
    }

    public BigDecimal calculateTeacherSalary(Long groupId, Integer year, Integer month) {
        List<GroupTeacher> teachers = groupTeacherRepository.findByGroupId(groupId);
        BigDecimal collected = paymentService.getCollectedForGroupMonth(groupId, year, month);

        return teachers.stream()
            .filter(gt -> gt.getEffectiveYear() <= year &&
                         (gt.getEffectiveYear() < year || gt.getEffectiveMonth() <= month))
            .map(gt -> collected.multiply(gt.getSalaryPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CourseGroupDto toDto(CourseGroup g) {
        CourseGroupDto dto = new CourseGroupDto();
        dto.setId(g.getId());
        dto.setGroupNumber(g.getGroupNumber());
        dto.setStartDate(g.getStartDate());
        dto.setDurationMonths(g.getDurationMonths());
        dto.setEndDate(g.getEndDate());
        dto.setStatus(g.getStatus());
        return dto;
    }

    private GroupStudentDto toGroupStudentDto(GroupStudent gs) {
        GroupStudentDto dto = new GroupStudentDto();
        dto.setId(gs.getId());
        dto.setStudentId(gs.getStudent().getId());
        dto.setMonthlyAmount(gs.getMonthlyAmount());
        dto.setJoinDate(gs.getJoinDate());
        dto.setNote(gs.getNote());
        dto.setStudentFirstName(gs.getStudent().getFirstName());
        dto.setStudentLastName(gs.getStudent().getLastName());
        dto.setStudentFin(gs.getStudent().getFin());
        return dto;
    }

    private GroupTeacherDto toGroupTeacherDto(GroupTeacher gt) {
        GroupTeacherDto dto = new GroupTeacherDto();
        dto.setId(gt.getId());
        dto.setTeacherId(gt.getTeacher().getId());
        dto.setSalaryPercentage(gt.getSalaryPercentage());
        dto.setCalculatedSalary(gt.getCalculatedSalary());
        dto.setSalaryStatus(gt.getSalaryStatus());
        dto.setEffectiveMonth(gt.getEffectiveMonth());
        dto.setEffectiveYear(gt.getEffectiveYear());
        dto.setTeacherFirstName(gt.getTeacher().getFirstName());
        dto.setTeacherLastName(gt.getTeacher().getLastName());
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        groupRepository.deleteById(id);
    }
}
