package com.crm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
@Getter @Setter
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String actorEmail;
    @Column(nullable = false, length = 10)
    private String httpMethod;
    @Column(nullable = false, length = 512)
    private String path;
    @Column(nullable = false)
    private Integer statusCode;
    private String entityType;
    private Long entityId;
    private String action;
    @Column(columnDefinition = "TEXT")
    private String beforeJson;
    @Column(columnDefinition = "TEXT")
    private String afterJson;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
