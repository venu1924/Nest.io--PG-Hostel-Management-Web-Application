package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {

    @EntityGraph(attributePaths = "beds")
    List<Room> findByOwnerIdOrderByRoomNumberAsc(String ownerId);

    @EntityGraph(attributePaths = "beds")
    List<Room> findByOwnerIdAndFloorNumberOrderByRoomNumberAsc(String ownerId, Integer floorNumber);

    @EntityGraph(attributePaths = "beds")
    Optional<Room> findByOwnerIdAndRoomNumber(String ownerId, Integer roomNumber);

    long countByOwnerId(String ownerId);
}
