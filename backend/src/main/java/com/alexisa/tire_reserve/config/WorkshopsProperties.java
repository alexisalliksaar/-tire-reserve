package com.alexisa.tire_reserve.config;

import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@ConfigurationProperties("workshops")
@ConfigurationPropertiesScan
@Validated
public record WorkshopsProperties(@NotNull @NotEmpty Map<String, @Valid WorkshopProperties> map) {

    public record WorkshopProperties(
            @NotNull
            WorkshopId workshopId,
            @NotBlank
            String apiPath,
            @NotBlank
            String city,
            @NotBlank
            String address,
            @NotBlank
            String phoneNumber,
            @NotBlank
            String email,
            @Valid @NotEmpty
            List<ServiceableVehicle> serviceableVehicles
    ){}

    @Component
    @ConfigurationPropertiesBinding
    public static class WorkshopIdConverter implements Converter<String, WorkshopId> {

        @Override
        public WorkshopId convert(@NonNull String source) {
            for (WorkshopId workshopId : WorkshopId.values()) {
                if (workshopId.getName().equalsIgnoreCase(source)) {
                    return workshopId;
                }
            }
            throw new IllegalArgumentException("Invalid WorkshopId name: " + source);
        }
    }

    @Component
    @ConfigurationPropertiesBinding
    public static class ServieableVehicleConverter implements Converter<String, ServiceableVehicle> {

        @Override
        public ServiceableVehicle convert(@NonNull String source) {
            for (ServiceableVehicle serviceableVehicle : ServiceableVehicle.values()) {
                if (serviceableVehicle.getName().equalsIgnoreCase(source)) {
                    return serviceableVehicle;
                }
            }
            throw new IllegalArgumentException("Invalid ServiceableVehicle name: " + source);
        }
    }
}
