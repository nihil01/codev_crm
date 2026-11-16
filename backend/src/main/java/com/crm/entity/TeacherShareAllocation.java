package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_share_allocations")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherShareAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="payment_transaction_id",nullable=false)
    private PaymentTransaction paymentTransaction;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="group_teacher_id",nullable=false)
    private GroupTeacher groupTeacher;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="teacher_id",nullable=false)
    private Teacher teacher;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="group_id",nullable=false)
    private CourseGroup group;
    @Column(nullable=false,precision=5,scale=2)
    private BigDecimal percentage;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal amount;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
