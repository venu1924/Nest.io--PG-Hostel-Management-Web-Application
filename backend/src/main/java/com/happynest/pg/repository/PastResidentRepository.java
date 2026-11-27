package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PastResident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastResidentRepository extends JpaRepository<PastResident, String> {

    List<PastResident> findByOwnerIdOrderByCheckoutDateDesc(String ownerId);

    Optional<PastResident> findByIdAndOwnerId(String id, String ownerId);
}
