package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, String> {

    List<Resident> findByOwnerIdOrderByNameAsc(String ownerId);

    List<Resident> findByOwnerId(String ownerId);

    Optional<Resident> findByIdAndOwnerId(String id, String ownerId);

    long countByOwnerId(String ownerId);
}
