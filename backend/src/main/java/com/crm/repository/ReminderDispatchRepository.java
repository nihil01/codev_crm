package com.crm.repository;

import com.crm.entity.ReminderDispatch;
import com.crm.enums.ReminderDispatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;

public interface ReminderDispatchRepository extends JpaRepository<ReminderDispatch,Long> {
    boolean existsByPaymentIdAndKind(Long paymentId,String kind);

    List<ReminderDispatch> findByStatusInAndAttemptsLessThanOrderByIdAsc(
        Collection<ReminderDispatchStatus> states,int maximum,Pageable page);

    List<ReminderDispatch> findByStatusAndLastAttemptAtBefore(
        ReminderDispatchStatus state,LocalDateTime cutOff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM ReminderDispatch d WHERE d.id=:id")
    Optional<ReminderDispatch> lockById(@Param("id") Long id);
}
