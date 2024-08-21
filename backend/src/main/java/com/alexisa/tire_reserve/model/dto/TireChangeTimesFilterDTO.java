package com.alexisa.tire_reserve.model.dto;

import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public record TireChangeTimesFilterDTO(
        @Nullable List<WorkshopId> selectedWorkshops,
        @JsonDeserialize(using = LocalDateToInstantDeserializer.class)
        @NonNull Instant fromDate,
        @JsonDeserialize(using = LocalDateToInstantDeserializer.class)
        @Nullable Instant toDate,
        @Nullable List<ServiceableVehicle> serviceableVehicles
) {
    private static class LocalDateToInstantDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            String dateString = jsonParser.getText();

            LocalDate localDate = LocalDate.parse(dateString);
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }
}
