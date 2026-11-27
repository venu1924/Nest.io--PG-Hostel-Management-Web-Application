package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.TemporaryResident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemporaryResidentRepository extends JpaRepository<TemporaryResident, String> {

    List<TemporaryResident> findByOwnerIdOrderByJoiningDateAscNameAsc(String ownerId);

    Optional<TemporaryResident> findByIdAndOwnerId(String id, String ownerId);
}
