package com.crm.repository;
import com.crm.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CourseRepository extends JpaRepository<Course,Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Course> findByActiveTrueOrderByNameAsc();
}
