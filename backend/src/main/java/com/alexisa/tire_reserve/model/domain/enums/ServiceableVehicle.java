package com.alexisa.tire_reserve.model.domain.enums;

import lombok.Getter;

@Getter
public enum ServiceableVehicle {
    CAR("CAR"),
    TRUCK("TRUCK");

    private final String name;

    ServiceableVehicle(String name) {
        this.name = name;
    }
}
