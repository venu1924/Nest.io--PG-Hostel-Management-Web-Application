package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PgOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PgOwnerRepository extends JpaRepository<PgOwner, String> {

    Optional<PgOwner> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
