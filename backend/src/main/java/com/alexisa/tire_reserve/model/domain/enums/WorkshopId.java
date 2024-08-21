package com.alexisa.tire_reserve.model.domain.enums;

import lombok.Getter;

@Getter
public enum WorkshopId {
    MANCHESTER("MANCHESTER"),
    LONDON("LONDON");
    private final String name;

    WorkshopId(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
