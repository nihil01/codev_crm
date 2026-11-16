package com.crm.repository;

import com.crm.entity.StudentFreezePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentFreezePeriodRepository extends JpaRepository<StudentFreezePeriod,Long> {
    Optional<StudentFreezePeriod> findByStudentIdAndYearAndMonth(Long studentId,Integer year,Integer month);
    boolean existsByStudentIdAndYearAndMonth(Long studentId,Integer year,Integer month);
    List<StudentFreezePeriod> findByStudentIdOrderByYearDescMonthDesc(Long studentId);
}
