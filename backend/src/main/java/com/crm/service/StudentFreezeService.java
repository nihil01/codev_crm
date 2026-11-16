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
import java.util.List;

@Service @RequiredArgsConstructor
public class StudentFreezeService {
    private final StudentFreezePeriodRepository periods;
    private final StudentRepository students;
    private final PaymentRepository invoices;
    private final PaymentService payments;
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
            .createdBy(SecurityContextHolder.getContext().getAuthentication().getName()).build());
        for(Payment p:matches)payments.freeze(p.getId(),true);
        return new FreezeDto(period.getId(),year,month,period.getCreatedBy());
    }
    @Transactional
    public void unfreeze(Long studentId,int year,int month) {
        StudentFreezePeriod period=periods.findByStudentIdAndYearAndMonth(studentId,year,month)
            .orElseThrow(()->new IllegalArgumentException("Dondurulmuş ay tapılmadı"));
        periods.delete(period);
        for(Payment p:invoices.findByStudentId(studentId))
            if(p.getPaymentYear()==year && p.getPaymentMonth()==month)
                payments.freeze(p.getId(),false);
    }
}
