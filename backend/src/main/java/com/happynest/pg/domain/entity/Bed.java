package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bed {

    @Id
    @Column(length = 96)
    private String bedId;

    /** Short id sent to the website, such as 101-A. */
    @Column(nullable = false, length = 16)
    private String bedKey;

    @Column(nullable = false, length = 1)
    private String bedLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BedStatus status;

    @Column(length = 64)
    private String residentId;

    /** MONTHLY vs TEMPORARY occupant; null when vacant. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OccupantType occupantType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
}
