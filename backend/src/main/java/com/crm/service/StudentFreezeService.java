package com.crm.service;

import com.crm.entity.Payment;
import com.crm.entity.Student;
import com.crm.entity.StudentFreezePeriod;
import com.crm.repository.PaymentRepository;
import com.crm.repository.StudentRepository;
import com.crm.repository.StudentFreezePeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import com.crm.enums.StudentStatus;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor
public class StudentFreezeService {
    private final StudentFreezePeriodRepository periods;
    private final StudentRepository students;
    private final PaymentRepository invoices;
    private final PaymentService payments;
    private final ChangeAuditService audit;

    public java.math.BigDecimal collectedForInvoice(Long invoiceId) {
        return payments.collectedForInvoice(invoiceId);
    }

    @Scheduled(cron="0 2 0 * * *",zone="Asia/Baku")
    @Transactional
    public void autoFreezePausedStudents() {
        YearMonth month=YearMonth.now(ZoneId.of("Asia/Baku"));
        for (Student student: students.findByStatus(StudentStatus.DONDURULUB)) {
            ensureFrozen(student,month);
        }
        for (Student student: students.findByStatus(StudentStatus.AYRILIB)) {
            if(student.getExitDate()!=null && month.isAfter(YearMonth.from(student.getExitDate())))
                ensureFrozen(student,month);
        }
    }

    private void ensureFrozen(Student student,YearMonth month) {
        Long id=student.getId();
        if(periods.existsByStudentIdAndYearAndMonth(id,month.getYear(),month.getMonthValue()))return;
        boolean paid=invoices.findByStudentId(id).stream()
            .filter(p->p.getPaymentYear().equals(month.getYear()) && p.getPaymentMonth().equals(month.getMonthValue()))
            .anyMatch(p->payments.collectedForInvoice(p.getId()).signum()>0);
        if(!paid) freeze(id,month.getYear(),month.getMonthValue());
    }
    public record FreezeDto(Long id,Integer year,Integer month,String createdBy) {}

    @Transactional(readOnly=true)
    public List<FreezeDto> list(Long studentId) {
        return periods.findByStudentIdOrderByYearDescMonthDesc(studentId).stream()
            .map(p->new FreezeDto(p.getId(),p.getYear(),p.getMonth(),p.getCreatedBy())).toList();
    }
    @Transactional
    public FreezeDto freeze(Long studentId,int year,int month) {
        java.time.YearMonth.of(year,month);
        Student student=students.findById(studentId)
            .orElseThrow(()->new IllegalArgumentException("Tələbə tapılmadı"));
        if(periods.existsByStudentIdAndYearAndMonth(studentId,year,month))
            throw new IllegalArgumentException("Bu ay artıq dondurulub");
        List<Payment> matches=invoices.findByStudentId(studentId).stream()
            .filter(p->p.getPaymentYear()==year && p.getPaymentMonth()==month).toList();
        for(Payment p:matches)
            if(payments.collectedForInvoice(p.getId()).signum()>0)
                throw new IllegalArgumentException("Artıq ödənilmiş ay dondurula bilməz");
        StudentFreezePeriod period=periods.save(StudentFreezePeriod.builder()
            .student(student).year(year).month(month)
            .createdBy(SecurityContextHolder.getContext().getAuthentication()==null?
                "SYSTEM":SecurityContextHolder.getContext().getAuthentication().getName()).build());
        for(Payment p:matches)payments.freeze(p.getId(),true);
        FreezeDto created=new FreezeDto(period.getId(),year,month,period.getCreatedBy());
        audit.record("CREATE","STUDENT_FREEZE",period.getId(),null,
            java.util.Map.of("studentId",studentId,"year",year,"month",month,"createdBy",period.getCreatedBy()));
        return created;
    }
    @Transactional
    public void unfreeze(Long studentId,int year,int month) {
        StudentFreezePeriod period=periods.findByStudentIdAndYearAndMonth(studentId,year,month)
            .orElseThrow(()->new IllegalArgumentException("Dondurulmuş ay tapılmadı"));
        Student student=students.findById(studentId)
            .orElseThrow(()->new IllegalArgumentException("Tələbə tapılmadı"));
        java.time.YearMonth current=java.time.YearMonth.now(java.time.ZoneId.of("Asia/Baku"));
        if(student.getStatus()==com.crm.enums.StudentStatus.DONDURULUB &&
            current.getYear()==year && current.getMonthValue()==month)
            throw new IllegalArgumentException("Cari ayı açmaq üçün əvvəl tələbənin statusunu Aktiv edin");

        periods.delete(period);
        periods.flush();

        for(Payment p:invoices.findByStudentId(studentId))
            if(p.getPaymentYear()==year && p.getPaymentMonth()==month)
                payments.freeze(p.getId(),false);
        audit.record("DELETE","STUDENT_FREEZE",period.getId(),
            java.util.Map.of("studentId",studentId,"year",year,"month",month,"createdBy",period.getCreatedBy()),null);
    }
}
