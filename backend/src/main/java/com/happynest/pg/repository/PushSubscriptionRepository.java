package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByOwnerId(String ownerId);
}
