package com.crm.repository;

import com.crm.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByPaymentIdOrderByPaidAtDesc(Long paymentId);
    java.util.Optional<PaymentTransaction> findByPaymentIdAndIdempotencyKey(Long paymentId, String idempotencyKey);

    @Query("SELECT COUNT(t)>0 FROM PaymentTransaction t WHERE t.payment.group.id=:groupId AND t.paidAt>=:fromDate")
    boolean anyCollectedFrom(@Param("groupId") Long groupId,@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.payment.id = :paymentId")
    BigDecimal sumForInvoice(@Param("paymentId") Long paymentId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
      "WHERE t.paidAt >= :start AND t.paidAt < :end")
    BigDecimal sumCollectedForPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
      "WHERE t.paidAt >= :start AND t.paidAt < :end AND t.payment.group.id = :groupId")
    BigDecimal sumCollectedForGroupPeriod(@Param("groupId") Long groupId,
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
