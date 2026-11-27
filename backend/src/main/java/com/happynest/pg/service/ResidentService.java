package com.happynest.pg.service;

import com.happynest.pg.domain.entity.*;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.domain.enums.SharingType;
import com.happynest.pg.dto.*;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.*;
import com.happynest.pg.security.OwnerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final PastResidentRepository pastResidentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final BedAssignmentService bedAssignmentService;

    @Transactional
    public List<ResidentDto> getResidents() {
        settleMoveIns();
        return residentRepository.findByOwnerIdOrderByNameAsc(OwnerContext.require()).stream()
                .map(DtoMapper::toResidentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResidentDto getResident(String id) {
        return DtoMapper.toResidentDto(findResident(id));
    }

    @Transactional(readOnly = true)
    public List<PastResidentDto> getPastResidents() {
        return pastResidentRepository.findByOwnerIdOrderByCheckoutDateDesc(OwnerContext.require()).stream()
                .map(DtoMapper::toPastResidentDto)
                .toList();
    }

    @Transactional
    public ResidentDto create(CreateResidentRequest req) {
        Room room = roomRepository.findByOwnerIdAndRoomNumber(OwnerContext.require(), req.getRoomNumber())
                .orElseThrow(() -> new ApiException("Room not found.", HttpStatus.NOT_FOUND));

        bedAssignmentService.prepareRoom(room, req.getSharingType());
        SharingType sharingType = bedAssignmentService.resolveSharingType(room, req.getSharingType());
        if (sharingType == SharingType.TRIPLE) {
            bedAssignmentService.ensureTwoBedTriple(room);
        }
        Bed assignedBed = bedAssignmentService.ensureBed(room, req.getBedId());
        String id = "res-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalDate today = LocalDate.now();
        boolean laterMonth = StayPlans.joinsLaterMonth(req.getJoiningDate(), today);
        if (laterMonth) {
            if (!StayPlans.bookableForLaterJoin(assignedBed, residentRepository.findByOwnerId(OwnerContext.require()), req.getJoiningDate())) {
                throw new ApiException("Choose a vacant bed or someone who is leaving next month.", HttpStatus.BAD_REQUEST);
            }
        } else {
            bedAssignmentService.occupyBed(room, assignedBed, id, sharingType, req.getTripleMemberLimit());
            roomRepository.save(room);
        }

        BigDecimal deposit = req.getDeposit() != null ? req.getDeposit() : BigDecimal.ZERO;
        BigDecimal refundAmount = resolveRefundAmount(req.getRefundAmount(), deposit, req.getDepositRefundable());

        Resident resident = Resident.builder()
                .id(id)
                .ownerId(OwnerContext.require())
                .name(req.getName().trim())
                .phone(req.getPhone().replaceAll("\\s", ""))
                .joiningDate(req.getJoiningDate())
                .roomNumber(req.getRoomNumber())
                .bedId(assignedBed.getBedKey())
                .monthlyRent(req.getMonthlyRent())
                .deposit(deposit)
                .refundAmount(refundAmount)
                .depositRefundable(refundAmount.compareTo(BigDecimal.ZERO) > 0)
                .sharingType(sharingType)
                .notes(req.getNotes() != null ? req.getNotes().trim() : "")
                .build();
        residentRepository.save(resident);

        LocalDate join = resident.getJoiningDate() != null ? resident.getJoiningDate() : LocalDate.now();
        LocalDate now = LocalDate.now();
        int payMonth = join.getMonthValue();
        int payYear = join.getYear();
        LocalDate joinMonthStart = LocalDate.of(payYear, payMonth, 1);
        LocalDate currentStart = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
        if (joinMonthStart.isBefore(currentStart)) {
            payMonth = now.getMonthValue();
            payYear = now.getYear();
        }
        Payment payment = Payment.builder()
                .id("pay-" + id + "-" + payYear + "-" + payMonth)
                .ownerId(resident.getOwnerId())
                .residentId(id)
                .month(payMonth)
                .year(payYear)
                .amount(resident.getMonthlyRent())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        return DtoMapper.toResidentDto(resident);
    }

    @Transactional
    public ResidentDto planVacate(String id, LocalDate vacateDate) {
        Resident resident = findResident(id);
        resident.setVacateDate(vacateDate);
        return DtoMapper.toResidentDto(residentRepository.save(resident));
    }

    @Transactional
    public void settleMoveIns() {
        String ownerId = OwnerContext.require();
        LocalDate today = LocalDate.now();
        for (Resident resident : residentRepository.findByOwnerId(ownerId)) {
            if (resident.getJoiningDate() == null || resident.getJoiningDate().isAfter(today)) {
                continue;
            }
            Room room = roomRepository.findByOwnerIdAndRoomNumber(ownerId, resident.getRoomNumber()).orElse(null);
            if (room == null) {
                continue;
            }
            Bed bed;
            try {
                bed = bedAssignmentService.ensureBed(room, resident.getBedId());
            } catch (ApiException ex) {
                continue;
            }
            if (resident.getId().equals(bed.getResidentId())) {
                continue;
            }
            if (bed.getStatus() == com.happynest.pg.domain.enums.BedStatus.OCCUPIED
                    && bed.getOccupantType() != com.happynest.pg.domain.enums.OccupantType.TEMPORARY) {
                Resident holder = bed.getResidentId() == null
                        ? null
                        : residentRepository.findByIdAndOwnerId(bed.getResidentId(), ownerId).orElse(null);
                if (holder == null || holder.getVacateDate() == null || holder.getVacateDate().isAfter(today)) {
                    continue;
                }
                bedAssignmentService.vacateBed(room, bed.getBedKey());
            } else if (bed.getStatus() == com.happynest.pg.domain.enums.BedStatus.OCCUPIED) {
                continue;
            }
            bedAssignmentService.occupyBed(room, bed, resident.getId(), resident.getSharingType(), null);
            roomRepository.save(room);
        }
    }

    @Transactional
    public ResidentDto update(String id, UpdateResidentRequest req) {
        Resident resident = findResident(id);
        resident.setName(req.getName().trim());
        resident.setPhone(req.getPhone().replaceAll("\\s", ""));
        resident.setJoiningDate(req.getJoiningDate());
        resident.setMonthlyRent(req.getMonthlyRent());
        BigDecimal deposit = req.getDeposit() != null ? req.getDeposit() : BigDecimal.ZERO;
        BigDecimal refundAmount = resolveRefundAmount(req.getRefundAmount(), deposit, req.getDepositRefundable());
        resident.setDeposit(deposit);
        resident.setRefundAmount(refundAmount);
        resident.setDepositRefundable(refundAmount.compareTo(BigDecimal.ZERO) > 0);
        if (req.getNotes() != null) {
            resident.setNotes(req.getNotes());
        }
        return DtoMapper.toResidentDto(residentRepository.save(resident));
    }

    @Transactional
    public MapId delete(String id, DeleteResidentRequest req) {
        Resident resident = findResident(id);
        Room room = roomRepository.findByOwnerIdAndRoomNumber(OwnerContext.require(), resident.getRoomNumber())
                .orElse(null);
        if (room != null) {
            Bed held = room.getBeds().stream()
                    .filter(bed -> resident.getId().equals(bed.getResidentId()))
                    .findFirst()
                    .orElse(null);
            if (held != null) {
                bedAssignmentService.vacateBed(room, held.getBedKey());
                roomRepository.save(room);
            }
        }

        String reason = req != null && req.getNotes() != null ? req.getNotes().trim() : "";

        PastResident past = PastResident.builder()
                .id(resident.getId())
                .ownerId(resident.getOwnerId())
                .name(resident.getName())
                .phone(resident.getPhone())
                .joiningDate(resident.getJoiningDate())
                .checkoutDate(LocalDate.now())
                .lastRoomNumber(resident.getRoomNumber())
                .lastBedId(resident.getBedId())
                .monthlyRent(resident.getMonthlyRent())
                .deposit(resident.getDeposit())
                .refundAmount(DtoMapper.resolveRefundAmount(
                        resident.getRefundAmount(), resident.getDeposit(), resident.isDepositRefundable()))
                .depositRefundable(resident.isDepositRefundable())
                .sharingType(resident.getSharingType())
                .notes(resident.getNotes())
                .deletionReason(reason)
                .build();
        pastResidentRepository.save(past);

        paymentRepository.deleteByResidentId(id);
        residentRepository.delete(resident);
        return new MapId(id);
    }

    private static BigDecimal resolveRefundAmount(
            BigDecimal refundAmount, BigDecimal deposit, Boolean depositRefundable) {
        if (refundAmount != null) {
            return refundAmount;
        }
        return DtoMapper.resolveRefundAmount(null, deposit, depositRefundable);
    }

    private Resident findResident(String id) {
        return residentRepository.findByIdAndOwnerId(id, OwnerContext.require())
                .orElseThrow(() -> new ApiException("Resident not found.", HttpStatus.NOT_FOUND));
    }

    public record MapId(String id) {}
}
