package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;

import java.util.List;

public interface WsApiServiceI {
    WorkshopsProperties.WorkshopProperties getWorkshopProperties();
    List<TireChangeTime> getAllAvailableTireChangeTimes()
            throws WsServiceCommunicationException;
    TireChangeTime bookDesiredTime(BookTimeRequest bookTimeRequest)
            throws WsServiceCommunicationException;
}
