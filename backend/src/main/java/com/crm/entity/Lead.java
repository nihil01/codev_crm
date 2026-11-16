package com.crm.entity;

import com.crm.enums.LeadSource;
import com.crm.enums.LeadStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadSource source;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStage stage;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_manager_id")
    private User assignedManager;
    private LocalDateTime consultationAt;
    @Column(columnDefinition = "TEXT")
    private String lostReason;
    private String interestedCourse;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="converted_student_id",unique=true)
    private Student convertedStudent;
    @Column(columnDefinition = "TEXT")
    private String note;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
