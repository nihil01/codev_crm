package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name="courses")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Course {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,unique=true)
    private String name;
    @Column(columnDefinition="TEXT")
    private String description;
    @Column(nullable=false)
    private boolean active = true;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
