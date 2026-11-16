package com.crm.repository;

import com.crm.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.crm.enums.StudentStatus;
import com.crm.enums.GroupStatus;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByFin(String fin);
    boolean existsByFin(String fin);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.fin) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Student> search(@Param("query") String query);

    List<Student> findByGroupId(Long groupId);
    List<Student> findByArchived(boolean archived);

    long countByArchived(boolean archived);

    @Query("SELECT COUNT(DISTINCT gs.student.id) FROM GroupStudent gs " +
           "WHERE gs.student.archived = false AND gs.student.status = :studentStatus " +
           "AND gs.group.status IN :groupStatuses")
    long countInActiveGroups(@Param("studentStatus") StudentStatus studentStatus,
                             @Param("groupStatuses") List<GroupStatus> groupStatuses);
}
