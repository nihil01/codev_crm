package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name="student_freeze_periods",
       uniqueConstraints=@UniqueConstraint(columnNames={"student_id","freeze_year","freeze_month"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentFreezePeriod {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="student_id",nullable=false)
    private Student student;
    @Column(name="freeze_year",nullable=false)
    private Integer year;
    @Column(name="freeze_month",nullable=false)
    private Integer month;
    @Column(nullable=false)
    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
