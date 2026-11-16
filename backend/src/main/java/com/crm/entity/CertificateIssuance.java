package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="certificate_issuances")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificateIssuance {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="student_id",nullable=false)
    private Student student;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="group_id",nullable=false)
    private CourseGroup group;
    @Column(nullable=false)
    private String certificateTitle;
    @Column(nullable=false)
    private LocalDate issuedOn;
    @Column(nullable=false)
    private String issuedBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
