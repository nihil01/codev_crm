package com.crm.repository;

import com.crm.entity.TeacherShareAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TeacherShareAllocationRepository extends JpaRepository<TeacherShareAllocation,Long> {
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM TeacherShareAllocation a WHERE " +
        "a.group.id=:groupId AND a.paymentTransaction.paidAt>=:start AND a.paymentTransaction.paidAt<:end")
    BigDecimal sumForGroup(@Param("groupId") Long groupId,
        @Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM TeacherShareAllocation a WHERE " +
        "a.groupTeacher.id=:groupTeacherId AND a.paymentTransaction.paidAt>=:start AND a.paymentTransaction.paidAt<:end")
    BigDecimal sumForAssignment(@Param("groupTeacherId") Long groupTeacherId,
        @Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Query("SELECT a.teacher.id, a.teacher.firstName, a.teacher.lastName, " +
        "a.group.groupNumber, SUM(a.amount) FROM TeacherShareAllocation a " +
        "WHERE a.paymentTransaction.paidAt>=:start AND a.paymentTransaction.paidAt<:end " +
        "GROUP BY a.teacher.id,a.teacher.firstName,a.teacher.lastName,a.group.groupNumber " +
        "ORDER BY a.teacher.lastName,a.group.groupNumber")
    List<Object[]> totalsByTeacherForPeriod(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    boolean existsByGroupTeacherId(Long groupTeacherId);
    List<TeacherShareAllocation> findByTeacherId(Long teacherId);
}
