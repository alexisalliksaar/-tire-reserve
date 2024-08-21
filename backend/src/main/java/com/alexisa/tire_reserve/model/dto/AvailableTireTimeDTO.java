package com.alexisa.tire_reserve.model.dto;

import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;

public record AvailableTireTimeDTO(WorkshopId workshopId, String id, String time) {

    public static AvailableTireTimeDTO dtoFromModel(TireChangeTime tireChangeTime) {
        return new AvailableTireTimeDTO(
                tireChangeTime.getWorkshopId(),
                tireChangeTime.getId(),
                tireChangeTime.getInstant().toString()
        );
    }
}
