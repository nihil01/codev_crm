package com.crm;

import com.crm.entity.*;
import com.crm.enums.*;
import com.crm.repository.*;
import com.crm.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class GroupMembershipWithdrawalTest {
    @Mock CourseGroupRepository groups;
    @Mock GroupStudentRepository memberships;
    @Mock GroupTeacherRepository teachers;
    @Mock StudentRepository students;
    @Mock TeacherRepository teacherRepository;
    @Mock PaymentRepository invoices;
    @Mock PaymentService payments;
    @Mock CourseRepository courses;
    @Mock StudentFreezePeriodRepository freezes;
    @Mock ChangeAuditService audit;
    @Mock TeacherShareAllocationRepository shares;
    @Mock PaymentTransactionRepository transactions;
    @InjectMocks CourseGroupService service;

    @Test
    void unpaidFutureEnrollmentCanBeRemovedWithoutLeavingPhantomInvoices() {
        CourseGroup group=CourseGroup.builder().id(5L).build();
        Student student=Student.builder().id(7L).status(StudentStatus.AKTIV).build();
        GroupStudent member=GroupStudent.builder().id(12L).student(student)
            .group(group).joinDate(LocalDate.now()).monthlyAmount(new BigDecimal("300")).build();
        Payment bill=Payment.builder().id(99L).student(student).group(group)
            .amount(new BigDecimal("300.00")).dueDate(LocalDate.now().plusMonths(1))
            .status(PaymentStatus.GOZLENILIR).build();
        when(memberships.findByGroupIdAndStudentId(5L,7L)).thenReturn(Optional.of(member));
        when(invoices.findByStudentIdAndGroupId(7L,5L)).thenReturn(List.of(bill));

        service.removeStudentFromGroup(5L,7L,"Səhv qeydiyyat");
        verify(invoices).deleteAll(List.of(bill));
        verify(memberships).delete(member);
    }

    @Test
    void maturedUnpaidDebtIsPreservedAndFutureDebtIsWaivedOnExit() {
        CourseGroup group=CourseGroup.builder().id(5L).build();
        Student student=Student.builder().id(7L).status(StudentStatus.AKTIV).build();
        GroupStudent member=GroupStudent.builder().id(12L).student(student).group(group)
            .joinDate(LocalDate.now().minusMonths(1)).monthlyAmount(new BigDecimal("300")).build();
        Payment oldBill=Payment.builder().id(21L).student(student).group(group)
            .amount(new BigDecimal("300")).dueDate(LocalDate.now().minusDays(8))
            .status(PaymentStatus.GECIKIB).build();
        Payment future=Payment.builder().id(22L).student(student).group(group)
            .amount(new BigDecimal("300")).dueDate(LocalDate.now().plusMonths(1))
            .status(PaymentStatus.GOZLENILIR).build();
        when(memberships.findByGroupIdAndStudentId(5L,7L)).thenReturn(Optional.of(member));
        when(invoices.findByStudentIdAndGroupId(7L,5L)).thenReturn(List.of(oldBill,future));
        when(payments.remaining(oldBill)).thenReturn(new BigDecimal("300"));

        service.removeStudentFromGroup(5L,7L,"Başqa qrupa keçirildi");
        assertNotNull(member.getLeftAt());
        assertEquals(PaymentStatus.GECIKIB,oldBill.getStatus());
        assertEquals(PaymentStatus.BAGISLANIB,future.getStatus());
        verify(invoices,never()).deleteAll(any());
        verify(memberships).save(member);
    }
}
