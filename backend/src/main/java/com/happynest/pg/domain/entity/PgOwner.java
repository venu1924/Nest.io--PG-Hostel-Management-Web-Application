package com.happynest.pg.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pg_owner")
@Getter
@Setter
@NoArgsConstructor
public class PgOwner {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String pgName;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private Integer floors;

    @Column(nullable = false)
    private Integer roomsPerFloor;

    /** Room counts from floor 1 upward, for example 6,4,8. */
    @Column(length = 80)
    private String floorRoomCounts;
}
