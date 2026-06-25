package com.crm.repository;

import com.crm.entity.Teacher;
import com.crm.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByFin(String fin);
    boolean existsByFin(String fin);
    List<Teacher> findByStatus(TeacherStatus status);
}
