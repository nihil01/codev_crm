package com.crm.repository;

import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {
    List<CourseGroup> findByStatus(GroupStatus status);
    List<CourseGroup> findByStatusIn(List<GroupStatus> statuses);
    long countByStatusIn(List<GroupStatus> statuses);

    long countByStatus(GroupStatus status);
}
