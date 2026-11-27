package com.happynest.pg.service;

import com.happynest.pg.domain.entity.PgOwner;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.enums.OccupancyMode;
import com.happynest.pg.domain.enums.RoomType;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final RoomRepository roomRepository;

    @Transactional
    public void createBuilding(PgOwner owner, List<Integer> roomsOnFloors) {
        if (roomsOnFloors == null || roomsOnFloors.isEmpty() || roomsOnFloors.size() > 15) {
            throw new ApiException("Choose 1 to 15 floors.", HttpStatus.BAD_REQUEST);
        }
        List<Room> rooms = new ArrayList<>();
        for (int floor = 1; floor <= roomsOnFloors.size(); floor++) {
            Integer count = roomsOnFloors.get(floor - 1);
            if (count == null || count < 1 || count > 30) {
                throw new ApiException("Each floor needs 1 to 30 rooms.", HttpStatus.BAD_REQUEST);
            }
            for (int seq = 1; seq <= count; seq++) {
                int roomNumber = floor * 100 + seq;
                rooms.add(Room.builder()
                        .id(owner.getId() + ":" + roomNumber)
                        .ownerId(owner.getId())
                        .roomNumber(roomNumber)
                        .floorNumber(floor)
                        .type(RoomType.OPEN)
                        .capacity(0)
                        .occupancyMode(OccupancyMode.DOUBLE)
                        .beds(new ArrayList<>())
                        .build());
            }
        }
        roomRepository.saveAll(rooms);
    }
}
