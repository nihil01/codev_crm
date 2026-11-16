package com.crm.entity;

import com.crm.enums.GroupStatus;
import com.crm.enums.TrainingMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String groupNumber; // G-2024-01

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private Integer durationMonths;

    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="course_id")
    private Course course;

    @Column(name="lesson_days")
    private String lessonDays;
    @Column(name="lesson_time")
    private String lessonTime;
    @Enumerated(EnumType.STRING)
    private TrainingMode trainingMode;
    private String room;
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.AKTIV;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupStudent> groupStudents = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupTeacher> groupTeachers = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void calculateEndDate() {
        if (startDate != null && durationMonths != null) {
            this.endDate = startDate.plusMonths(durationMonths);
        }
    }
}
