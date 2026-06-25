package com.crm.repository;

import com.crm.entity.GroupStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupStudentRepository extends JpaRepository<GroupStudent, Long> {
    List<GroupStudent> findByGroupId(Long groupId);
    List<GroupStudent> findByStudentId(Long studentId);
    Optional<GroupStudent> findByGroupIdAndStudentId(Long groupId, Long studentId);
}
