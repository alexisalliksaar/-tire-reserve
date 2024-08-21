package com.alexisa.tire_reserve.model.domain;

import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;

import java.util.List;

public record AvailableTimesResponse(
        List<TireChangeTime> availableTimes,
        List<WorkshopId> failedWorkshops
) { }
