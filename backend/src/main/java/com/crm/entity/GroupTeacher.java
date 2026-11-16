package com.crm.entity;

import com.crm.enums.SalaryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_teachers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CourseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private BigDecimal salaryPercentage; // Faiz (məs: 30)

    private BigDecimal calculatedSalary;

    @Enumerated(EnumType.STRING)
    private SalaryStatus salaryStatus = SalaryStatus.GOZLENILIR;

    @Column(nullable = false)
    private Integer effectiveMonth; // Hansı aydan etibarən tətbiq olunur

    @Column(nullable = false)
    private Integer effectiveYear;

    private Integer effectiveUntilMonth;
    private Integer effectiveUntilYear;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
