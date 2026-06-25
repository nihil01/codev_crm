package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.entity.CourseGroup;
import com.crm.entity.Payment;
import com.crm.entity.Student;
import com.crm.enums.PaymentMethod;
import com.crm.enums.PaymentStatus;
import com.crm.repository.CourseGroupRepository;
import com.crm.repository.PaymentRepository;
import com.crm.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final CourseGroupRepository groupRepository;

    public List<PaymentDto> getAll() {
        return paymentRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PaymentDto> getByStudent(Long studentId) {
        return paymentRepository.findByStudentId(studentId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PaymentDto> getByGroup(Long groupId) {
        return paymentRepository.findByGroupId(groupId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PaymentDto> getByGroupAndPeriod(Long groupId, Integer year, Integer month) {
        return paymentRepository.findByGroupAndPeriod(groupId, year, month).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PaymentDto> getByPeriod(Integer year, Integer month) {
        return paymentRepository.findByPaymentYearAndPaymentMonth(year, month).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PaymentDto> getOverdue(Integer year, Integer month) {
        return paymentRepository.findOverduePayments(year, month).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public BigDecimal getCollectedForMonth(Integer year, Integer month) {
        return paymentRepository.sumCollectedForMonth(year, month);
    }

    @Transactional
    public PaymentDto recordPayment(Long paymentId, PaymentMethod method) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));

        payment.setStatus(PaymentStatus.ODENILIB);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod(method);

        return toDto(paymentRepository.save(payment));
    }

    @Transactional
    public void markOverdue() {
        LocalDate now = LocalDate.now();
        List<Payment> overdue = paymentRepository.findOverduePayments(
            now.getYear(), now.getMonthValue()
        );
        overdue.forEach(p -> p.setStatus(PaymentStatus.GECIKIB));
        paymentRepository.saveAll(overdue);
    }

    @Transactional
    public PaymentDto updateStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ödəniş tapılmadı"));
        payment.setStatus(status);
        if (status == PaymentStatus.ODENILIB && payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        return toDto(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDto create(PaymentDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
            .orElseThrow(() -> new RuntimeException("Tələbə tapılmadı"));
        CourseGroup group = groupRepository.findById(dto.getGroupId())
            .orElseThrow(() -> new RuntimeException("Qrup tapılmadı"));

        Payment payment = Payment.builder()
            .student(student)
            .group(group)
            .paymentYear(dto.getPaymentYear())
            .paymentMonth(dto.getPaymentMonth())
            .amount(dto.getAmount())
            .status(dto.getStatus() != null ? dto.getStatus() : PaymentStatus.GOZLENILIR)
            .note(dto.getNote())
            .build();

        return toDto(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        paymentRepository.deleteById(id);
    }

    private PaymentDto toDto(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.setId(p.getId());
        dto.setStudentId(p.getStudent().getId());
        dto.setGroupId(p.getGroup().getId());
        dto.setPaymentYear(p.getPaymentYear());
        dto.setPaymentMonth(p.getPaymentMonth());
        dto.setAmount(p.getAmount());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setStatus(p.getStatus());
        dto.setNote(p.getNote());
        dto.setStudentFirstName(p.getStudent().getFirstName());
        dto.setStudentLastName(p.getStudent().getLastName());
        dto.setGroupNumber(p.getGroup().getGroupNumber());
        return dto;
    }
}
