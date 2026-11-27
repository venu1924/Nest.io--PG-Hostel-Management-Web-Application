package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.dto.FloorDto;
import com.happynest.pg.dto.RoomDto;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.RoomRepository;
import com.happynest.pg.security.OwnerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public List<FloorDto> getFloors() {
        String ownerId = OwnerContext.require();
        List<Room> rooms = roomRepository.findByOwnerIdOrderByRoomNumberAsc(ownerId);
        Map<Integer, List<Room>> byFloor = rooms.stream()
                .collect(Collectors.groupingBy(Room::getFloorNumber));

        return byFloor.keySet().stream()
                .sorted()
                .map(floor -> FloorDto.builder()
                        .floorNumber(floor)
                        .rooms(byFloor.getOrDefault(floor, List.of()).stream()
                                .sorted(Comparator.comparing(Room::getRoomNumber))
                                .map(DtoMapper::toRoomDto)
                                .toList())
                        .build())
                .toList();
    }

    public FloorDto getFloor(Integer floorNumber) {
        String ownerId = OwnerContext.require();
        List<Room> rooms = roomRepository.findByOwnerIdAndFloorNumberOrderByRoomNumberAsc(ownerId, floorNumber);
        if (rooms.isEmpty()) {
            throw new ApiException("Floor not found.", HttpStatus.NOT_FOUND);
        }
        return FloorDto.builder()
                .floorNumber(floorNumber)
                .rooms(rooms.stream().map(DtoMapper::toRoomDto).toList())
                .build();
    }

    public RoomDto getRoom(Integer roomNumber) {
        Room room = roomRepository.findByOwnerIdAndRoomNumber(OwnerContext.require(), roomNumber)
                .orElseThrow(() -> new ApiException("Room not found.", HttpStatus.NOT_FOUND));
        return DtoMapper.toRoomDto(room);
    }

    public List<RoomDto> getRooms() {
        return roomRepository.findByOwnerIdOrderByRoomNumberAsc(OwnerContext.require()).stream()
                .map(DtoMapper::toRoomDto)
                .toList();
    }
}
