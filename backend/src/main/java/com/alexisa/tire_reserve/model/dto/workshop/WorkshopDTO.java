package com.alexisa.tire_reserve.model.dto.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import lombok.Builder;

import java.util.List;

@Builder
public record WorkshopDTO(
        String workshopId,
        String city,
        String address,
        String email,
        String phoneNumber,
        List<ServiceableVehicle> serviceableVehicles
) {
    public static WorkshopDTO fromWorkshopProperties(WorkshopsProperties.WorkshopProperties wsProps){
        WorkshopDTO.WorkshopDTOBuilder builder = new WorkshopDTOBuilder();
        builder
                .workshopId(wsProps.workshopId().getName())
                .city(wsProps.city())
                .address(wsProps.address())
                .email(wsProps.email())
                .phoneNumber(wsProps.phoneNumber())
                .serviceableVehicles(wsProps.serviceableVehicles());
        return builder.build();
    }
}
