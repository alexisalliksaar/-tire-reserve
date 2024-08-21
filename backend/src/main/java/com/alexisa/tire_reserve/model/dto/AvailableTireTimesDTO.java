package com.alexisa.tire_reserve.model.dto;

import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;

import java.util.List;

public record AvailableTireTimesDTO(List<AvailableTireTimeDTO> tireChangeTimes, List<WorkshopId> failedWorkshopIds) {
}
