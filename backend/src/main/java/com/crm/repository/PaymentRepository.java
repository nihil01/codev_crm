package com.crm.repository;

import com.crm.entity.Payment;
import com.crm.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@Param("id") Long id);

    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByGroupId(Long groupId);
    List<Payment> findByStudentIdAndGroupId(Long studentId, Long groupId);
    List<Payment> findByPaymentYearAndPaymentMonth(Integer year, Integer month);

    @Query("SELECT p FROM Payment p WHERE p.group.id = :groupId AND p.paymentYear = :year AND p.paymentMonth = :month")
    List<Payment> findByGroupAndPeriod(@Param("groupId") Long groupId,
                                        @Param("year") Integer year,
                                        @Param("month") Integer month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.group.id = :groupId AND p.paymentYear = :year AND p.paymentMonth = :month AND p.status = 'ODENILIB'")
    BigDecimal sumCollectedForGroupAndMonth(@Param("groupId") Long groupId,
                                             @Param("year") Integer year,
                                             @Param("month") Integer month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentYear = :year AND p.paymentMonth = :month AND p.status = 'ODENILIB' AND p.group.status = 'AKTIV'")
    BigDecimal sumCollectedForMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT p FROM Payment p WHERE p.status = 'GOZLENILIR' AND p.group.status = 'AKTIV' AND (p.paymentYear < :year OR (p.paymentYear = :year AND p.paymentMonth < :month))")
    List<Payment> findOverduePayments(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'GOZLENILIR' AND p.group.status = 'AKTIV' AND (p.paymentYear < :year OR (p.paymentYear = :year AND p.paymentMonth < :month))")
    long countOverduePayments(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentYear = :year AND p.paymentMonth = :month AND p.group.status = 'AKTIV'")
    BigDecimal sumExpectedForMonth(@Param("year") Integer year, @Param("month") Integer month);

    List<Payment> findByStatus(PaymentStatus status);
}
