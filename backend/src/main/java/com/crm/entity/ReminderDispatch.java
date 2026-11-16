package com.crm.entity;

import com.crm.enums.ReminderDispatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name="reminder_dispatches",
       uniqueConstraints=@UniqueConstraint(columnNames={"payment_id","kind"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReminderDispatch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="payment_id",nullable=false)
    private Payment payment;
    @Column(nullable=false,length=30)
    private String kind;
    @Column(nullable=false)
    private String phone;
    @Column(nullable=false,columnDefinition="TEXT")
    private String message;
    @Column(nullable=false,unique=true,length=128)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20)
    private ReminderDispatchStatus status;
    @Column(nullable=false)
    private int attempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    @Column(length=255)
    private String lastError;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
