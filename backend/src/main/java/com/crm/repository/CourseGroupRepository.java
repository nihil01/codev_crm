package com.crm.repository;

import com.crm.entity.CourseGroup;
import com.crm.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM CourseGroup g WHERE g.id=:id")
    Optional<CourseGroup> lockById(@Param("id") Long id);

    List<CourseGroup> findByStatus(GroupStatus status);
    List<CourseGroup> findByStatusIn(List<GroupStatus> statuses);
    long countByStatusIn(List<GroupStatus> statuses);

    long countByStatus(GroupStatus status);
}
