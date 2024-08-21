package com.alexisa.tire_reserve.model.domain;

import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.alexisa.tire_reserve.model.dto.TireChangeTimesFilterDTO;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;

public record TireChangeTimesFilter(
        @Nullable List<WorkshopId> selectedWorkshops,
        @NonNull Instant fromDate,
        @Nullable Instant toDate,
        @Nullable List<ServiceableVehicle> serviceableVehicles
) {

    public static TireChangeTimesFilter fromDTO(TireChangeTimesFilterDTO dto){
        return new TireChangeTimesFilter(
                dto.selectedWorkshops(),
                dto.fromDate(),
                dto.toDate(),
                dto.serviceableVehicles());
    }
}
