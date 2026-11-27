package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Payment;
import com.happynest.pg.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByOwnerIdAndMonthAndYear(String ownerId, Integer month, Integer year);

    long countByOwnerIdAndMonthAndYearAndStatus(String ownerId, Integer month, Integer year, PaymentStatus status);

    boolean existsByResidentIdAndMonthAndYear(String residentId, Integer month, Integer year);

    Optional<Payment> findByIdAndOwnerId(String id, String ownerId);

    void deleteByResidentId(String residentId);
}
