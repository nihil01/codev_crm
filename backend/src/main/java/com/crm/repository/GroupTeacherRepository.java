package com.crm.repository;

import com.crm.entity.GroupTeacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupTeacherRepository extends JpaRepository<GroupTeacher, Long> {
    List<GroupTeacher> findByGroupId(Long groupId);
    Optional<GroupTeacher> findByGroupIdAndEffectiveMonthAndEffectiveYear(
        Long groupId, Integer month, Integer year);
}
