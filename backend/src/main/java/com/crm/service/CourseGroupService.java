package com.crm.service;

import com.crm.dto.CourseGroupDto;
import com.crm.dto.GroupStudentDto;
import com.crm.dto.GroupTeacherDto;
import com.crm.entity.*;
import com.crm.enums.GroupStatus;
import com.crm.enums.PaymentStatus;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import com.crm.enums.TeacherStatus;
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
    private final CourseRepository courseRepository;
    private final StudentFreezePeriodRepository freezePeriods;
    private final ChangeAuditService audit;
    private final TeacherShareAllocationRepository shareAllocations;
    private final PaymentTransactionRepository paymentTransactions;
    @Value("${app.billing.first-payment-delay-months:0}")
    private int firstPaymentDelayMonths;

    @Transactional(readOnly = true)
    public List<CourseGroupDto> getAll() {
        return groupRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseGroupDto> getActive() {
        return groupRepository.findByStatusIn(List.of(GroupStatus.AKTIV,GroupStatus.BASLAYIB)).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseGroupDto getById(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        return toDto(group);
    }

    @Transactional
    public CourseGroupDto create(CourseGroupDto dto) {
        GroupStatus initial = dto.getStatus()==null?GroupStatus.TOPLANIR:dto.getStatus();
        if(initial!=GroupStatus.TOPLANIR && initial!=GroupStatus.BASLAYIB)
            throw new IllegalArgumentException("Yeni qrup yalnız Toplanır və ya Başlayıb ola bilər");
        if (dto.getCourseId()==null || dto.getCourseId()<=0)
            throw new IllegalArgumentException("Kurs seçilməlidir");
        CourseGroup group = CourseGroup.builder()
            .groupNumber(dto.getGroupNumber())
            .startDate(dto.getStartDate())
            .durationMonths(dto.getDurationMonths())
            .status(dto.getStatus() == null ? GroupStatus.TOPLANIR : dto.getStatus())
            .build();

        applyGroupMetadata(group,dto);
        group.calculateEndDate();
        CourseGroupDto created=toDto(groupRepository.save(group));
        audit.record("CREATE","COURSE_GROUP",created.getId(),null,created);
        return created;
    }

    @Transactional
    public CourseGroupDto update(Long id, CourseGroupDto dto) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));

        CourseGroupDto before=toDto(group);
        if(dto.getStatus()!=null) validateTransition(group.getStatus(),dto.getStatus());
        group.setGroupNumber(dto.getGroupNumber());
        group.setStartDate(dto.getStartDate());
        group.setDurationMonths(dto.getDurationMonths());
        if (dto.getStatus() != null) {
            group.setStatus(dto.getStatus());
        }
        applyGroupMetadata(group,dto);
        group.calculateEndDate();

        CourseGroupDto changed=toDto(groupRepository.save(group));
        audit.record("UPDATE","COURSE_GROUP",id,before,changed);
        return changed;
    }

    @Transactional
    public void complete(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        CourseGroupDto before=toDto(group);
        validateTransition(group.getStatus(),GroupStatus.TAMAMLANMIS);
        group.setStatus(GroupStatus.TAMAMLANMIS);
        groupRepository.save(group);
        audit.record("COMPLETE","COURSE_GROUP",id,before,toDto(group));
    }

    @Transactional
    public void suspend(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        CourseGroupDto before=toDto(group);
        validateTransition(group.getStatus(),GroupStatus.DAYANDIRILMIS);
        group.setStatus(GroupStatus.DAYANDIRILMIS);
        groupRepository.save(group);
        audit.record("SUSPEND","COURSE_GROUP",id,before,toDto(group));
    }

    @Transactional
    public void resume(Long id) {
        CourseGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        if (group.getStatus() != GroupStatus.DAYANDIRILMIS) {
            throw new RuntimeException("Yalnız dayandırılmış qrupları bərpa etmək olar");
        }
        CourseGroupDto before=toDto(group);
        validateTransition(group.getStatus(),GroupStatus.BASLAYIB);
        group.setStatus(GroupStatus.BASLAYIB);
        groupRepository.save(group);
        audit.record("RESUME","COURSE_GROUP",id,before,toDto(group));
    }

    // === Group Students ===

    public List<GroupStudentDto> getGroupStudents(Long groupId) {
        return groupStudentRepository.findByGroupId(groupId).stream()
            .map(this::toGroupStudentDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public GroupStudentDto addStudentToGroup(Long groupId, GroupStudentDto dto) {
        CourseGroup group = groupRepository.lockById(groupId)
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));
        if (group.getStatus()==GroupStatus.DAYANDIRILMIS || group.getStatus()==GroupStatus.TAMAMLANMIS)
            throw new IllegalArgumentException("Dayandırılmış və tamamlanmış qrupa tələbə əlavə edilmir");
        Student student = studentRepository.findById(dto.getStudentId())
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));

        if (group.getMaxStudents()!=null && groupStudentRepository.findByGroupId(groupId).stream()
            .filter(m->m.getLeftAt()==null && !m.getStudent().isArchived() &&
                m.getStudent().getStatus()==com.crm.enums.StudentStatus.AKTIV).count()>=group.getMaxStudents())
            throw new IllegalArgumentException("Qrupun maksimum tələbə sayı dolub");

        if (groupStudentRepository.findByGroupIdAndStudentId(groupId, dto.getStudentId()).isPresent()) {
            throw new IllegalArgumentException("Bu tələbə üçün qrup qeydiyyatı artıq mövcuddur; əvvəlki qeydiyyatı əl ilə yoxlayın");
        }

        GroupStudent gs = GroupStudent.builder()
            .group(group)
            .student(student)
            .monthlyAmount(dto.getMonthlyAmount())
            .joinDate(dto.getJoinDate())
            .note(dto.getNote())
            .build();

        if (student.isArchived() || student.getStatus()!=com.crm.enums.StudentStatus.AKTIV)
            throw new IllegalArgumentException("Yalnız aktiv tələbə qrupa əlavə edilə bilər");
        // Create payment schedule for all months
        createPaymentSchedule(student, group, dto.getMonthlyAmount(), dto.getJoinDate());

        // Only set primary group if student doesn't have one yet
        if (student.getGroup() == null) {
            student.setGroup(group);
            studentRepository.save(student);
        }

        GroupStudentDto created=toGroupStudentDto(groupStudentRepository.save(gs));
        audit.record("ENROLL","GROUP_STUDENT",created.getId(),null,created);
        return created;
    }

    @Transactional
    public void removeStudentFromGroup(Long groupId, Long studentId, String reason) {
        GroupStudent membership = groupStudentRepository.findByGroupIdAndStudentId(groupId,studentId)
            .orElseThrow(()->new IllegalArgumentException("Tələbə bu qrupda deyil"));
        if(membership.getLeftAt()!=null)
            throw new IllegalArgumentException("Tələbə artıq bu qrupdan ayrılıb");
        if(reason==null || reason.isBlank())
            throw new IllegalArgumentException("Qrupdan ayrılma səbəbi yazılmalıdır");
        GroupStudentDto before=toGroupStudentDto(membership);
        LocalDate today=LocalDate.now(java.time.ZoneId.of("Asia/Baku"));
        List<Payment> bills=paymentRepository.findByStudentIdAndGroupId(studentId,groupId);

        // Never delete cash history. Unpaid future invoices can be removed;
        // invoices with realized cash or matured debts remain intact.
        boolean hasReceipts=bills.stream()
            .anyMatch(b->paymentTransactions.existsByPaymentId(b.getId()));
        boolean hasMaturedDebt=bills.stream().anyMatch(b -> {
            // Legacy invoices may lack due_date: their invoice month still matters.
            LocalDate due=b.getDueDate()!=null ? b.getDueDate()
                : YearMonth.of(b.getPaymentYear(),b.getPaymentMonth()).atDay(1);
            return !due.isAfter(today) && paymentService.remaining(b).signum()>0;
        });

        if(!hasReceipts && !hasMaturedDebt) {
            paymentRepository.deleteAll(bills);
            groupStudentRepository.delete(membership);
            audit.record("REMOVE_UNUSED","GROUP_STUDENT",membership.getId(),before,null);
        } else {
            membership.setLeftAt(today);
            membership.setExitReason(reason.trim());
            // Preserve prior due debts and actual receipts; waive only future
            // unpaid invoices so the student stops accumulating new debt.
            for(Payment bill:bills) {
                LocalDate due=bill.getDueDate()!=null ? bill.getDueDate()
                    : YearMonth.of(bill.getPaymentYear(),bill.getPaymentMonth()).atDay(1);
                if(due.isAfter(today) && !paymentTransactions.existsByPaymentId(bill.getId())) {
                    bill.setStatus(PaymentStatus.BAGISLANIB);
                    bill.setNote((bill.getNote()==null?"":bill.getNote()+" | ")+
                        "Qrupdan ayrılıb: "+reason.trim());
                    paymentRepository.save(bill);
                }
            }
            groupStudentRepository.save(membership);
            audit.record("WITHDRAW","GROUP_STUDENT",membership.getId(),
                before,toGroupStudentDto(membership));
        }
        Student student=membership.getStudent();
        if(student.getGroup()!=null && student.getGroup().getId().equals(groupId)){
            CourseGroup next=groupStudentRepository.findByStudentId(studentId).stream()
                .filter(m->m.getLeftAt()==null && !m.getGroup().getId().equals(groupId))
                .map(GroupStudent::getGroup).findFirst().orElse(null);
            student.setGroup(next);
            studentRepository.save(student);
        }
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
        if(teacher.getStatus()!=TeacherStatus.AKTIV)
            throw new IllegalArgumentException("Arxiv müəllim qrupa təyin edilə bilməz");
        if(dto.getSalaryPercentage()==null || dto.getSalaryPercentage().signum()<=0 ||
           dto.getSalaryPercentage().scale()>2 ||
           dto.getSalaryPercentage().compareTo(BigDecimal.valueOf(100))>0)
            throw new IllegalArgumentException("Müəllim payı 0–100% arasında olmalıdır");
        java.time.YearMonth begin=java.time.YearMonth.of(
            dto.getEffectiveYear()==null?LocalDate.now().getYear():dto.getEffectiveYear(),
            dto.getEffectiveMonth()==null?LocalDate.now().getMonthValue():dto.getEffectiveMonth());
        if(paymentTransactions.anyCollectedFrom(groupId,begin.atDay(1).atStartOfDay()))
            throw new IllegalArgumentException("Mədaxil olan aylar üzrə müəllimi geriyə tarixlə təyin etmək olmaz");
        List<GroupTeacher> overlapping=groupTeacherRepository.findByGroupId(groupId).stream()
            .filter(gt->gt.getEffectiveUntilYear()==null ||
                java.time.YearMonth.of(gt.getEffectiveUntilYear(),gt.getEffectiveUntilMonth()).isAfter(begin))
            .toList();
        if(overlapping.stream().anyMatch(x->x.getTeacher().getId().equals(dto.getTeacherId())))
            throw new IllegalArgumentException("Müəllim bu tarix aralığında artıq təyin olunub");
        BigDecimal total=overlapping.stream().map(GroupTeacher::getSalaryPercentage)
            .reduce(BigDecimal.ZERO,BigDecimal::add).add(dto.getSalaryPercentage());
        if(total.compareTo(BigDecimal.valueOf(100))>0)
            throw new IllegalArgumentException("Qrup müəllim paylarının cəmi 100%-i keçə bilməz");

        GroupTeacher gt = GroupTeacher.builder()
            .group(group)
            .teacher(teacher)
            .salaryPercentage(dto.getSalaryPercentage())
            .effectiveMonth(dto.getEffectiveMonth()==null?LocalDate.now().getMonthValue():dto.getEffectiveMonth())
            .effectiveYear(dto.getEffectiveYear()==null?LocalDate.now().getYear():dto.getEffectiveYear())
            .build();

        GroupTeacherDto created=toGroupTeacherDto(groupTeacherRepository.save(gt));
        audit.record("ASSIGN","GROUP_TEACHER",created.getId(),null,created);
        return created;
    }

    @Transactional
    public void removeTeacher(Long groupId, Long teacherId) {
        List<GroupTeacher> gts = groupTeacherRepository.findByGroupId(groupId);
        GroupTeacher selected=gts.stream()
            .filter(gt -> gt.getTeacher().getId().equals(teacherId))
            .findFirst().orElseThrow(()->new IllegalArgumentException("Müəllim qrupa təyin olunmayıb"));
        if(selected.getEffectiveUntilYear()!=null)
            throw new IllegalArgumentException("Bu müəllim təyinatı artıq bağlanıb");
        GroupTeacherDto before=toGroupTeacherDto(selected);
        if(shareAllocations.existsByGroupTeacherId(selected.getId())) {
            // End the assignment without deleting accruals already earned.
            java.time.YearMonth today=java.time.YearMonth.now(java.time.ZoneId.of("Asia/Baku"));
            selected.setEffectiveUntilYear(today.getYear());
            selected.setEffectiveUntilMonth(today.getMonthValue());
            groupTeacherRepository.save(selected);
            audit.record("END_ASSIGNMENT","GROUP_TEACHER",selected.getId(),before,toGroupTeacherDto(selected));
        } else {
            groupTeacherRepository.delete(selected);
            audit.record("REMOVE","GROUP_TEACHER",selected.getId(),before,null);
        }
    }

    // === Payment Schedule ===

    private void createPaymentSchedule(Student student, CourseGroup group, BigDecimal monthlyAmount, LocalDate joinDate) {
        if (monthlyAmount==null || monthlyAmount.signum()<=0 || monthlyAmount.scale()>2)
            throw new IllegalArgumentException("Aylıq ödəniş düzgün deyil");
        if(group.getEndDate()==null) group.calculateEndDate();
        LocalDate joined=joinDate==null?group.getStartDate():joinDate;
        if(joined.isAfter(group.getEndDate()))
            throw new IllegalArgumentException("Qrup müddəti bitdikdən sonra qeydiyyat olmaz");
        LocalDate first=joined.isAfter(group.getStartDate())?joined:group.getStartDate();
        int elapsed=(int)ChronoUnit.MONTHS.between(
            YearMonth.from(group.getStartDate()),YearMonth.from(first));
        int months=Math.max(0,group.getDurationMonths()-Math.max(0,elapsed));
        if(months==0) throw new IllegalArgumentException("Qrup müddəti başa çatıb");
        if(firstPaymentDelayMonths!=0 && firstPaymentDelayMonths!=1)
            throw new IllegalStateException("First payment delay must be 0 or 1 month");
        first=first.plusMonths(firstPaymentDelayMonths);
        for (int i=0; i<months; i++) {
            LocalDate due = first.plusMonths(i);
            boolean frozen=freezePeriods.existsByStudentIdAndYearAndMonth(student.getId(),due.getYear(),due.getMonthValue());
            Payment payment = Payment.builder()
                .student(student).group(group)
                .paymentYear(due.getYear()).paymentMonth(due.getMonthValue())
                .amount(monthlyAmount).dueDate(due)
                .discountAmount(BigDecimal.ZERO).frozen(frozen)
                .status(frozen?PaymentStatus.DONDURULUB:PaymentStatus.GOZLENILIR).build();
            paymentRepository.save(payment);
        }
    }

    // === Group Summary Calculations ===

    public BigDecimal calculateGroupCollected(Long groupId, Integer year, Integer month) {
        return paymentService.getCollectedForGroupMonth(groupId, year, month);
    }

    public BigDecimal calculateTeacherSalary(Long groupId, Integer year, Integer month) {
        java.time.YearMonth period=java.time.YearMonth.of(year,month);
        return shareAllocations.sumForGroup(groupId,
            period.atDay(1).atStartOfDay(),period.plusMonths(1).atDay(1).atStartOfDay());
    }

    private void validateTransition(GroupStatus oldStatus,GroupStatus newStatus) {
        if(oldStatus==newStatus) return;
        boolean allowed=switch(oldStatus) {
            case TOPLANIR -> newStatus==GroupStatus.BASLAYIB||newStatus==GroupStatus.DAYANDIRILMIS;
            case AKTIV, BASLAYIB -> newStatus==GroupStatus.DAYANDIRILMIS||newStatus==GroupStatus.TAMAMLANMIS;
            case DAYANDIRILMIS -> newStatus==GroupStatus.BASLAYIB;
            case TAMAMLANMIS -> false;
        };
        if(!allowed) throw new IllegalArgumentException("Qrup statusu bu mərhələyə keçə bilməz");
    }

    private void applyGroupMetadata(CourseGroup group, CourseGroupDto dto) {
        if (dto.getCourseId() != null && dto.getCourseId() > 0) {
            Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Kurs tapılmadı"));
            group.setCourse(course);
        }
        group.setLessonDays(dto.getLessonDays());
        group.setLessonTime(dto.getLessonTime());
        group.setTrainingMode(dto.getTrainingMode());
        group.setRoom(dto.getRoom());
        group.setMaxStudents(dto.getMaxStudents());
        if(group.getMaxStudents()!=null &&
           group.getMaxStudents()<groupStudentRepository.findByGroupId(group.getId()==null?-1L:group.getId()).stream()
               .filter(m->m.getLeftAt()==null && !m.getStudent().isArchived() &&
                   m.getStudent().getStatus()==com.crm.enums.StudentStatus.AKTIV).count())
            throw new IllegalArgumentException("Maksimum tələbə sayı mövcud iştirakçılardan az ola bilməz");
    }

    private CourseGroupDto toDto(CourseGroup g) {
        CourseGroupDto dto = new CourseGroupDto();
        dto.setId(g.getId());
        dto.setGroupNumber(g.getGroupNumber());
        dto.setStartDate(g.getStartDate());
        dto.setDurationMonths(g.getDurationMonths());
        dto.setEndDate(g.getEndDate());
        dto.setStatus(g.getStatus());
        dto.setCourseId(g.getCourse()==null?null:g.getCourse().getId());
        dto.setCourseName(g.getCourse()==null?null:g.getCourse().getName());
        dto.setLessonDays(g.getLessonDays());
        dto.setLessonTime(g.getLessonTime());
        dto.setTrainingMode(g.getTrainingMode());
        dto.setRoom(g.getRoom());
        dto.setMaxStudents(g.getMaxStudents());
        return dto;
    }

    private GroupStudentDto toGroupStudentDto(GroupStudent gs) {
        GroupStudentDto dto = new GroupStudentDto();
        dto.setId(gs.getId());
        dto.setStudentId(gs.getStudent().getId());
        dto.setMonthlyAmount(gs.getMonthlyAmount());
        dto.setJoinDate(gs.getJoinDate());
        dto.setLeftAt(gs.getLeftAt());
        dto.setExitReason(gs.getExitReason());
        dto.setStudentArchived(gs.getStudent().isArchived());
        dto.setStudentStatus(gs.getStudent().getStatus().name());
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
        java.time.YearMonth current=java.time.YearMonth.now(java.time.ZoneId.of("Asia/Baku"));
        dto.setCalculatedSalary(shareAllocations.sumForAssignment(gt.getId(),
            current.atDay(1).atStartOfDay(),current.plusMonths(1).atDay(1).atStartOfDay()));
        dto.setSalaryStatus(gt.getSalaryStatus());
        dto.setEffectiveMonth(gt.getEffectiveMonth());
        dto.setEffectiveYear(gt.getEffectiveYear());
        dto.setEffectiveUntilYear(gt.getEffectiveUntilYear());
        dto.setEffectiveUntilMonth(gt.getEffectiveUntilMonth());
        dto.setTeacherFirstName(gt.getTeacher().getFirstName());
        dto.setTeacherLastName(gt.getTeacher().getLastName());
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        CourseGroup group=groupRepository.findById(id)
            .orElseThrow(()->new IllegalArgumentException("Qrup tapılmadı"));
        if(!paymentRepository.findByGroupId(id).isEmpty() ||
           !groupStudentRepository.findByGroupId(id).isEmpty())
            throw new IllegalArgumentException("Maliyyə və tələbə tarixçəsi olan qrup silinmir");
        CourseGroupDto before=toDto(group);
        groupRepository.delete(group);
        audit.record("DELETE","COURSE_GROUP",id,before,null);
    }
}
