package com.happynest.pg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SetupRequest {

    @NotBlank
    @Size(max = 60)
    private String username;

    @NotBlank
    @Size(min = 8, max = 80)
    private String password;

    @NotBlank
    @Size(max = 120)
    private String pgName;

    /** How many rooms are on floor 1, floor 2, and so on. */
    @NotEmpty
    @Size(min = 1, max = 15)
    private List<Integer> roomsOnFloors;
}
