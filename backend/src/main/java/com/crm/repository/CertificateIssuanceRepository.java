package com.crm.repository;

import com.crm.entity.CertificateIssuance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificateIssuanceRepository extends JpaRepository<CertificateIssuance,Long> {
    Optional<CertificateIssuance> findByStudentIdAndGroupId(Long studentId,Long groupId);
}
