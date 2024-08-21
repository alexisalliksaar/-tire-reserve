package com.alexisa.tire_reserve.model.domain;

import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public class TireChangeTime {
    @Getter
    private final WorkshopId workshopId;
    @Getter
    private final Instant instant;
    /**
     * Note that {@code id} isn't unique across all instances of TireChangeTime,
     * Uniqueness is provided by the pair {{@code workshopId}, {@code id}}
     */
    @Getter
    private final String id;

    private final AtomicBoolean reserved = new AtomicBoolean(false);

    public TireChangeTime(WorkshopId workshopId, String id, Instant instant) {
        this.workshopId = workshopId;
        this.id = id;
        this.instant = instant;
    }

    public static Instant resolveInstant(String isoFormattedTimeString){
        ZonedDateTime zonedDateTime = ZonedDateTime
                .parse(isoFormattedTimeString, DateTimeFormatter.ISO_DATE_TIME);
        return zonedDateTime.toInstant();
    }
    public boolean isReservedSet(){
        return reserved.get();
    }
    public void setReservedTrue(){
        reserved.set(true);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TireChangeTime that = (TireChangeTime) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getWorkshopId(), that.getWorkshopId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorkshopId(), getId());
    }
}
