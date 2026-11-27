package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.dto.AdminDto;
import com.happynest.pg.dto.DashboardDto;
import com.happynest.pg.dto.PaymentSummaryDto;
import com.happynest.pg.dto.TemporaryResidentDto;
import com.happynest.pg.dto.TransferDto;
import com.happynest.pg.repository.*;
import com.happynest.pg.security.OwnerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ResidentRepository residentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final TransferRepository transferRepository;
    private final PastResidentRepository pastResidentRepository;
    private final TemporaryResidentRepository temporaryResidentRepository;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final ResidentService residentService;

    @Transactional
    public DashboardDto getDashboard() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        String ownerId = OwnerContext.require();
        paymentService.ensureMonthPayments(month, year);
        residentService.settleMoveIns();

        long paid = paymentRepository.countByOwnerIdAndMonthAndYearAndStatus(ownerId, month, year, PaymentStatus.PAID);
        long pending = paymentRepository.countByOwnerIdAndMonthAndYearAndStatus(ownerId, month, year, PaymentStatus.PENDING);

        List<Room> rooms = roomRepository.findByOwnerIdOrderByRoomNumberAsc(ownerId);
        List<Bed> beds = rooms.stream().flatMap(room -> room.getBeds().stream()).toList();
        long vacantPT = beds.stream().filter(b -> b.getStatus() == BedStatus.VACANT).count();
        long vacantIgnoringTemp = beds.stream()
                .filter(b -> b.getStatus() == BedStatus.VACANT
                        || (b.getStatus() == BedStatus.OCCUPIED && b.getOccupantType() == OccupantType.TEMPORARY))
                .count();

        List<TemporaryResidentDto> temps = temporaryResidentRepository.findByOwnerIdOrderByJoiningDateAscNameAsc(ownerId).stream()
                .map(DtoMapper::toTemporaryResidentDto)
                .sorted(Comparator.comparingInt(TemporaryResidentDto::getDaysLeft))
                .toList();
        long tempUnpaid = temps.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.PENDING).count();
        long tempPaid = temps.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.PAID).count();

        List<TransferDto> recent = transferRepository.findByOwnerIdOrderByTransferDateDesc(ownerId).stream()
                .limit(5)
                .map(t -> DtoMapper.toTransferDto(t, resolveName(t.getResidentId())))
                .toList();

        List<com.happynest.pg.domain.entity.Resident> people = residentRepository.findByOwnerId(ownerId);
        return DashboardDto.builder()
                .totalResidents(StayPlans.residentsThisMonth(people, now))
                .temporaryResidents(temps.size())
                .totalRooms(roomRepository.countByOwnerId(ownerId))
                .vacantBeds(vacantIgnoringTemp)
                .vacantBedsPT(vacantPT)
                .vacantBedsNextMonth(StayPlans.vacantBedsNextMonth(beds, people, now))
                .residentsNextMonth(StayPlans.residentsNextMonth(people, now))
                .pendingPayments(pending)
                .paymentSummary(PaymentSummaryDto.builder()
                        .month(month)
                        .year(year)
                        .paid(paid)
                        .pending(pending)
                        .total(paid + pending)
                        .temporaryPaid(tempPaid)
                        .temporaryUnpaid(tempUnpaid)
                        .build())
                .recentTransfers(recent)
                .temporaryStayAlerts(temps)
                .build();
    }

    public AdminDto getCurrentAdmin() {
        return authService.currentAdmin();
    }

    private String resolveName(String residentId) {
        return residentRepository.findByIdAndOwnerId(residentId, OwnerContext.require())
                .map(r -> r.getName())
                .orElseGet(() -> pastResidentRepository.findByIdAndOwnerId(residentId, OwnerContext.require())
                        .map(p -> p.getName())
                        .orElse("Former resident"));
    }
}
