package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetupStatus {
    private final boolean ready;
    private final String pgName;
}
