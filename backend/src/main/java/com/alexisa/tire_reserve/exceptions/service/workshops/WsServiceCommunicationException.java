package com.alexisa.tire_reserve.exceptions.service.workshops;

import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import lombok.Getter;

import java.io.IOException;

@Getter
public class WsServiceCommunicationException extends IOException {
    private final WorkshopId causedBy;

    public WsServiceCommunicationException(String message, Throwable cause, WorkshopId causedBy) {
        super(message, cause);
        this.causedBy = causedBy;
    }

    public WsServiceCommunicationException(WorkshopId causedBy) {
        this.causedBy = causedBy;
    }

}
