package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CourseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private BigDecimal monthlyAmount; // Fərdi aylıq məbləğ

    @Column(nullable = false)
    private LocalDate joinDate;

    // Exit from this group without destroying payment history or other enrollments.
    private LocalDate leftAt;
    @Column(columnDefinition = "TEXT")
    private String exitReason;

    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
