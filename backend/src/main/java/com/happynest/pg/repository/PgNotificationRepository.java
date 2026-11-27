package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PgNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PgNotificationRepository extends JpaRepository<PgNotification, String> {

    boolean existsByDedupeKey(String dedupeKey);

    List<PgNotification> findTop50ByOwnerIdOrderByCreatedAtDesc(String ownerId);

    long countByOwnerIdAndSeenFalse(String ownerId);

    Optional<PgNotification> findByIdAndOwnerId(String id, String ownerId);

    List<PgNotification> findByOwnerId(String ownerId);

    List<PgNotification> findByOwnerIdAndNoticeDate(String ownerId, LocalDate noticeDate);
}
