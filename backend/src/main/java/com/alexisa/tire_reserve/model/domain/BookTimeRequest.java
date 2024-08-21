package com.alexisa.tire_reserve.model.domain;

import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

public record BookTimeRequest(@NotBlank String contactInformation, @NotBlank String id, @NonNull WorkshopId workshopId) {

}
